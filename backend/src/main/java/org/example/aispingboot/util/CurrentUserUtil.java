package org.example.aispingboot.util;

import org.example.aispingboot.enumClass.UserType;
import org.example.aispingboot.exception.BusinessException;
import org.springframework.util.StringUtils;

/**
 * 当前登录用户工具类
 * 与 JwtAuthticationFilter 不同，这里允许在公开接口（匿名可访问）中探测可选登录态，
 * 例如知识文章列表需要区分“管理员看全部 / 其他人只看已发布”。
 */
public class CurrentUserUtil {

    /**
     * 解析当前请求携带的 token，未登录或 token 非法时返回 null
     */
    public static JwtTokenUtil.TokenVerificationResult currentUserOrNull() {
        String token = JwtTokenUtil.getCurrentToken();
        if (!StringUtils.hasText(token)) {
            return null;
        }
        try {
            return JwtTokenUtil.validateToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取当前登录用户ID，未登录时抛出业务异常
     */
    public static Long requireUserId() {
        JwtTokenUtil.TokenVerificationResult result = currentUserOrNull();
        if (result == null || result.getUserId() == null) {
            throw new BusinessException("暂未登录或token已经过期");
        }
        return result.getUserId();
    }

    /**
     * 当前请求是否来自管理员（用户类型 2）
     */
    public static boolean currentUserIsAdmin() {
        JwtTokenUtil.TokenVerificationResult result = currentUserOrNull();
        return result != null && UserType.ADMIN.getCode().equals(result.getRoleType());
    }
}
