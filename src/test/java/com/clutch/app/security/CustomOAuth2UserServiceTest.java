package com.clutch.app.security;

import com.clutch.app.entity.User;
import com.clutch.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private CustomOAuth2UserService oauth2UserService;

    @BeforeEach
    void setUp() {
        oauth2UserService = new CustomOAuth2UserService(userRepository);
    }

    @Test
    void loadUser_success_shouldReturnCustomUserDetails() {
        String email = "oauth2-user@clutch.app";
        Map<String, Object> attributes = Map.of("email", email, "name", "John Doe");
        User dbUser = new User();

        OAuth2User defaultOAuth2User = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "email"
        );

        oauth2UserService = new CustomOAuth2UserService(userRepository) {
            @Override
            public OAuth2User loadUser(OAuth2UserRequest userRequest) {
                String userEmail = defaultOAuth2User.getAttribute("email");
                User user = userRepository.findUserByEmail(userEmail)
                        .orElseThrow(() -> new OAuth2AuthenticationException("user_unknown"));
                return new CustomUserDetails(user, defaultOAuth2User.getAttributes());
            }
        };

        when(userRepository.findUserByEmail(email)).thenReturn(Optional.of(dbUser));


        OAuth2User result = oauth2UserService.loadUser(createMockRequest());


        assertNotNull(result);
        assertInstanceOf(CustomUserDetails.class, result);
        assertEquals(attributes, result.getAttributes());
        verify(userRepository).findUserByEmail(email);
    }

    @Test
    void loadUser_userNotFoundInDb_shouldThrowOAuth2AuthenticationException() {
        String email = "unknown-oauth2@clutch.app";
        Map<String, Object> attributes = Map.of("email", email);

        OAuth2User defaultOAuth2User = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "email"
        );

        oauth2UserService = new CustomOAuth2UserService(userRepository) {
            @Override
            public OAuth2User loadUser(OAuth2UserRequest userRequest) {
                String userEmail = defaultOAuth2User.getAttribute("email");
                User user = userRepository.findUserByEmail(userEmail)
                        .orElseThrow(() -> new OAuth2AuthenticationException(
                                new org.springframework.security.oauth2.core.OAuth2Error(
                                        "user_unknown", "User not found: " + userEmail, null
                                )
                        ));
                return new CustomUserDetails(user, defaultOAuth2User.getAttributes());
            }
        };

        when(userRepository.findUserByEmail(email)).thenReturn(Optional.empty());


        OAuth2AuthenticationException exception = assertThrows(OAuth2AuthenticationException.class, () -> {
            oauth2UserService.loadUser(createMockRequest());
        });


        assertNotNull(exception.getError());
        assertEquals("user_unknown", exception.getError().getErrorCode());
        assertEquals("User not found: " + email, exception.getError().getDescription());
        verify(userRepository).findUserByEmail(email);
    }

    private OAuth2UserRequest createMockRequest() {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("google")
                .clientId("client-id")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost")
                .authorizationUri("http://localhost/auth")
                .tokenUri("http://localhost/token")
                .userInfoUri("http://localhost/userinfo") // Заглушка для эндпоинта
                .userNameAttributeName("email")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "fake-token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        return new OAuth2UserRequest(clientRegistration, accessToken);
    }

}
