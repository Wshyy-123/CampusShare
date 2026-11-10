package com.campusshare.auth;

import com.campusshare.auth.entity.UserAccount;
import com.campusshare.auth.mapper.UserMapper;
import com.campusshare.common.BusinessException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    private final UserMapper userMapper;

    public CurrentUserProvider(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public CurrentUser require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new BusinessException(401, "AUTH_REQUIRED", "请先登录");
        }
        UserAccount user = userMapper.findByAccount(authentication.getName());
        if (user == null || !user.isEnabled()) {
            throw new BusinessException(401, "AUTH_REQUIRED", "登录状态无效，请重新登录");
        }
        return new CurrentUser(Long.toString(user.getId()), user.getAccount(),
                user.getDisplayName(), user.getRole());
    }
}
