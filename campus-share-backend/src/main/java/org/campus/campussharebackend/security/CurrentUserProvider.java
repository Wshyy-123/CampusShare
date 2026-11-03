package org.campus.campussharebackend.security;

import jakarta.servlet.http.HttpServletRequest;
import org.campus.campussharebackend.common.ApiException;
import org.campus.campussharebackend.common.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 当前用户身份。
 *
 * <p>登录（XIA-1）尚未实现。开发环境可通过 app.security.dev-identity-enabled=true 打开临时调试请求头
 * {@code X-Debug-User-Id}，仅用于本地联调；默认关闭，此时任何受保护请求都返回 401。</p>
 *
 * <p>TODO(XIA-1): 会话落地后改为从服务端会话读取用户，删除调试请求头。</p>
 */
@Component
public class CurrentUserProvider {

    public static final String DEV_USER_HEADER = "X-Debug-User-Id";

    private final HttpServletRequest request;
    private final boolean devIdentityEnabled;

    public CurrentUserProvider(HttpServletRequest request,
                               @Value("${app.security.dev-identity-enabled:false}") boolean devIdentityEnabled) {
        this.request = request;
        this.devIdentityEnabled = devIdentityEnabled;
    }

    public long requireUserId() {
        if (!devIdentityEnabled) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED);
        }
        String raw = request.getHeader(DEV_USER_HEADER);
        if (raw == null || raw.isBlank()) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED);
        }
        try {
            long userId = Long.parseLong(raw.trim());
            if (userId <= 0) {
                throw new NumberFormatException("用户ID必须为正整数");
            }
            return userId;
        } catch (NumberFormatException exception) {
            throw new ApiException(ErrorCode.INVALID_ARGUMENT, "调试用户标识不合法");
        }
    }
}
