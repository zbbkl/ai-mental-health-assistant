package org.example.aispingboot.config;

import cn.hutool.core.text.AntPathMatcher;
import jakarta.servlet.DispatcherType;
import org.example.aispingboot.common.ResultCode;
import org.example.aispingboot.util.JwtAuthticationFilter;
import org.example.aispingboot.util.ResponseUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();

    /** 管理员角色值，JwtAuthticationFilter 授权的权限名是 ROLE_ + userType */
    private static final String ADMIN_ROLE = "2";

    /**
     * 匿名可访问的接口，按“请求方法 + 路径”成对声明。
     * 只按路径放行会把同一路径下的写操作（PUT / DELETE）一起放开，
     * 例如 /api/knowledge/article/* 同时匹配详情查询与文章删除。
     */
    private static final List<PublicRule> PUBLIC_RULES = List.of(
            new PublicRule(HttpMethod.GET, "/"),
            new PublicRule(HttpMethod.GET, "/api/test"),
            new PublicRule(HttpMethod.POST, "/api/user/login"),
            new PublicRule(HttpMethod.POST, "/api/user/add"),
            // 前台知识文章浏览
            new PublicRule(HttpMethod.GET, "/api/knowledge/category/tree"),
            new PublicRule(HttpMethod.GET, "/api/knowledge/article/page"),
            new PublicRule(HttpMethod.GET, "/api/knowledge/article/*"),
            // 上传后的静态文件，img 标签请求不会携带 token
            new PublicRule(HttpMethod.GET, "/files/**")
    );

    /**
     * 仅管理员可访问的接口。
     * 这些接口是管理端专用（全站统计、跨用户数据），只校验“已登录”不够：
     * 普通用户登录后同样能读到所有人的情绪日志甚至删除他人记录。
     * 这里按路径前缀集中声明，避免新增管理端接口时漏加校验。
     */
    private static final List<AdminRule> ADMIN_RULES = List.of(
            // 全站数据分析
            new AdminRule(null, "/api/data-analytics/**"),
            // 情绪日志管理端接口（分页查询全部用户、删除他人记录）
            new AdminRule(null, "/api/emotion-diary/admin/**"),
            // 知识文章的写操作，method 为 null 表示所有方法
            new AdminRule(HttpMethod.POST, "/api/knowledge/article"),
            new AdminRule(HttpMethod.PUT, "/api/knowledge/article/**"),
            new AdminRule(HttpMethod.DELETE, "/api/knowledge/article/**")
    );

    public record PublicRule(HttpMethod method, String pattern) {
    }

    /** method 为 null 表示不限制请求方法 */
    public record AdminRule(HttpMethod method, String pattern) {
    }

    public static Boolean isPublicPATH(String method, String requestUri) {
        for (PublicRule rule : PUBLIC_RULES) {
            if (rule.method().matches(method) && antPathMatcher.match(rule.pattern(), requestUri)) {
                return true;
            }
        }
        return false;
    }

    @Bean
    public JwtAuthticationFilter jwtAuthticationFilter() {
        return new JwtAuthticationFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF保护 （API服务通常不需要）
                .csrf(AbstractHttpConfigurer::disable)
                // 配置会话管理为无状态（JWT需要）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置请求的授权规则
                .authorizeHttpRequests(auth -> {
                    // 容器内部转发的错误分发不参与鉴权，否则真实异常会被改写成无响应体的 403
                    auth.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
                    // 公开的路径，无需登录即可访问
                    for (PublicRule rule : PUBLIC_RULES) {
                        auth.requestMatchers(rule.method(), rule.pattern()).permitAll();
                    }
                    // 管理端专用接口，需要管理员角色
                    for (AdminRule rule : ADMIN_RULES) {
                        if (rule.method() == null) {
                            auth.requestMatchers(rule.pattern()).hasRole(ADMIN_ROLE);
                        } else {
                            auth.requestMatchers(rule.method(), rule.pattern()).hasRole(ADMIN_ROLE);
                        }
                    }
                    // 其他请求都需要认证
                    auth.anyRequest().authenticated();
                })
                // 权限不足时返回统一的 Result 结构，前端才能弹出可读提示
                .exceptionHandling(exception -> exception.accessDeniedHandler(
                        (request, response, accessDeniedException) ->
                                ResponseUtil.writeError(response, ResultCode.NO_PERMISSION)))
                // 添加JWT认证过滤器
                .addFilterBefore(jwtAuthticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
