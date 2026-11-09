package com.campusshare.auth;

import com.campusshare.auth.dto.CsrfTokenResponse;
import com.campusshare.auth.dto.CurrentUserResponse;
import com.campusshare.auth.dto.LoginRequest;
import com.campusshare.common.api.ApiResponse;
import com.campusshare.common.error.BusinessException;
import com.campusshare.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(AuthService authService, SecurityContextRepository securityContextRepository) {
        this.authService = authService;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/csrf")
    public ApiResponse<CsrfTokenResponse> csrfToken(HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken == null) {
            csrfToken = (CsrfToken) request.getAttribute("_csrf");
        }
        if (csrfToken == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "CSRF 令牌初始化失败");
        }
        return ApiResponse.success(new CsrfTokenResponse(csrfToken.getHeaderName(), csrfToken.getToken()));
    }

    @PostMapping("/login")
    public ApiResponse<CurrentUserResponse> login(@Valid @RequestBody LoginRequest request,
                                                  HttpServletRequest httpRequest,
                                                  HttpServletResponse httpResponse) {
        Authentication authentication = authService.authenticate(request);

        httpRequest.getSession(true);
        httpRequest.changeSessionId();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        return ApiResponse.success(authService.currentUser(authentication.getName()));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> me(Authentication authentication) {
        return ApiResponse.success(authService.currentUser(authentication.getName()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ApiResponse.success();
    }
}
