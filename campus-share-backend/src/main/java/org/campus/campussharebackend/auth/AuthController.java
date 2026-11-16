package org.campus.campussharebackend.auth;

import org.campus.campussharebackend.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal session login used so XIA-5/XIA-6 can be demonstrated before XIA-1 lands.
 * The final login flow belongs to XIA-1 and should replace this controller.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final CurrentUserProvider currentUserProvider;
    private final SessionAuthenticationStrategy sessionStrategy;
    private final CsrfTokenRepository csrfTokenRepository;

    public AuthController(AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository,
                          CurrentUserProvider currentUserProvider,
                          SessionAuthenticationStrategy sessionStrategy, CsrfTokenRepository csrfTokenRepository) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.currentUserProvider = currentUserProvider;
        this.sessionStrategy = sessionStrategy;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    public record LoginRequest(@NotBlank String account, @NotBlank String password) {
    }

    public record CsrfResponse(String token, String headerName) {
    }

    @GetMapping("/csrf")
    public ApiResponse<CsrfResponse> csrf(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return ApiResponse.ok("OK", new CsrfResponse(token.getToken(), token.getHeaderName()));
    }

    @PostMapping("/login")
    public ApiResponse<CurrentUser> login(@Valid @RequestBody LoginRequest loginRequest,
                                          HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.account(), loginRequest.password()));
        // 自定义JSON登录不会经过标准登录过滤器，显式更新会话ID并轮换CSRF令牌。
        sessionStrategy.onAuthentication(authentication, request, response);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        SecurityContextRepository repository = securityContextRepository;
        repository.saveContext(context, request, response);
        return ApiResponse.ok("登录成功", currentUserProvider.require());
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUser> me() {
        return ApiResponse.ok("OK", currentUserProvider.require());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        csrfTokenRepository.saveToken(null, request, response);
        return ApiResponse.ok("已退出", null);
    }
}
