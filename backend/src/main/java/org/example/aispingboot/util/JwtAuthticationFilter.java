package org.example.aispingboot.util;

import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.aispingboot.DTO.response.UserLoginResponseDTO;
import org.example.aispingboot.common.ResultCode;
import org.example.aispingboot.config.SecurityConfig;
import org.example.aispingboot.enumClass.UserStatus;
import org.example.aispingboot.service.UserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT 认证过滤器
 *
 * 与最初实现的两点差别：
 * 1. 公开路径不再直接跳过过滤器。带 token 访问公开路径时同样建立认证上下文，
 *    这样 CurrentUserUtil#currentUserIsAdmin() 在公开接口（如文章列表）上也能拿到真实身份。
 * 2. 权限（ROLE_x）取自数据库中的当前 user_type，而不是 token 里的 roleType。
 *    token 有效期 24 小时，若管理员被降权，仅凭 token 里的旧角色会让其继续拥有管理员权限。
 */
public class JwtAuthticationFilter extends OncePerRequestFilter {
    @Resource
    private UserService userService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        // 公开路径允许匿名访问，但不代表跳过认证：带了合法 token 就要认出身份
        boolean publicPath = SecurityConfig.isPublicPATH(request.getMethod(), request.getRequestURI());
        String token = JwtTokenUtil.extractTokenFromRequest(request);

        if (!StringUtils.hasText(token)) {
            clearSecurityContext();
            if (publicPath) {
                chain.doFilter(request, response);
                return;
            }
            ResponseUtil.writeError(response, ResultCode.ACCESS_UNAUTHORIZED);
            return;
        }

        UserLoginResponseDTO.UserDetailResponseDTO user = null;
        boolean tokenValid = false;
        try {
            JwtTokenUtil.TokenVerificationResult validationResult = JwtTokenUtil.validateToken(token);
            tokenValid = validationResult != null && validationResult.isValid();
            if (tokenValid) {
                user = userService.getUserById(validationResult.getUserId());
            }
        } catch (Exception e) {
            tokenValid = false;
        }

        if (!tokenValid) {
            clearSecurityContext();
            if (publicPath) {
                // 公开页面按未登录处理，不因为本地残留的过期 token 而打不开页面
                chain.doFilter(request, response);
                return;
            }
            ResponseUtil.writeError(response, ResultCode.TOKEN_INVALID);
            return;
        }

        if (user == null || !UserStatus.NORMAL.getCode().equals(user.getStatus())) {
            clearSecurityContext();
            if (publicPath) {
                chain.doFilter(request, response);
                return;
            }
            ResponseUtil.writeError(response, ResultCode.TOKEN_ACCESS_FORBIDDEN);
            return;
        }

        // 角色以数据库当前值为准
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getUserType())
        );
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername(), // 用户名作为主体
                null,
                authorities
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 将token存储到请求属性中
        request.setAttribute("jwtToken", token);

        chain.doFilter(request, response);
    }

    // 清理Spring Security上下文
    private void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
