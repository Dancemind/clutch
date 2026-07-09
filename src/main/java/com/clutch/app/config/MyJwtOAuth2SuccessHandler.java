package com.clutch.app.config;

import com.clutch.app.exceptions.ResourceNotFoundException;
import com.clutch.app.security.JwtTokenProvider;
import com.clutch.app.security.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MyJwtOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        if (!(authentication.getPrincipal() instanceof OAuth2User oAuth2User)) {
            throw new AuthenticationServiceException("OAuth2 authentication failed: Principal is missing or invalid");
        }

        String userEmail = oAuth2User.getAttribute("email");
        if (userEmail == null) {
            userEmail = oAuth2User.getName();
        }

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        String email = userEmail;
        UUID tenantId;
        try {
            tenantId = TenantContext.callAsSystem(() ->
                    userService.getCompanyUuidByUserEmail(email)
            );
        } catch (ResourceNotFoundException e) {
            logger.info("Tenant id not found. Redirected.");
            String errorUrl = UriComponentsBuilder.fromUriString("/login")
                    .queryParam("error", "user_not_registered")
                    .build().toUriString();
            getRedirectStrategy().sendRedirect(request, response, errorUrl);
            return;
        }

        String token = jwtTokenProvider.createToken(userEmail, tenantId.toString(), roles);

        String targetUrl = UriComponentsBuilder.fromUriString("/dashboard")
                .queryParam("token", token)
                .build().toUriString();

        logger.info("OAth2 login successful.");

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}

