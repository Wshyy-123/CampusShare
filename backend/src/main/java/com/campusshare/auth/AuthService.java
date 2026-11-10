package com.campusshare.auth;

import com.campusshare.auth.dto.CurrentUserResponse;
import com.campusshare.auth.dto.LoginRequest;
import com.campusshare.common.error.BusinessException;
import com.campusshare.common.error.ErrorCode;
import com.campusshare.user.AppUser;
import com.campusshare.user.AppUserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository userRepository;

    public AuthService(AuthenticationManager authenticationManager, AppUserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }

    public Authentication authenticate(LoginRequest request) {
        try {
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username().trim(), request.password()));
        } catch (DisabledException | LockedException ex) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "账号不可用");
        } catch (AuthenticationException ex) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "账号或密码错误");
        }
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "登录状态已失效，请重新登录"));

        return new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole().name(),
                user.getRole().getLabel());
    }
}
