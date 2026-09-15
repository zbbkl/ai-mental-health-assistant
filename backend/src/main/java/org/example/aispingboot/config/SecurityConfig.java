package org.example.aispingboot.config;

import cn.hutool.core.text.AntPathMatcher;
import jakarta.servlet.DispatcherType;
import org.example.aispingboot.util.JwtAuthticationFilter;
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

    public record PublicRule(HttpMethod method, String pattern) {
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
                    // 其他请求都需要认证
                    auth.anyRequest().authenticated();
                })
                // 添加JWT认证过滤器
                .addFilterBefore(jwtAuthticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
