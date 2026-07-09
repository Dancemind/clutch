package com.clutch.app.security;

import com.clutch.app.entity.User;
import com.clutch.app.enums.Role;
import com.clutch.app.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String email = oAuth2User.getAttribute("email");

        // find or create user - first login with Google
        // todo: user not found - throw error: user unknown (by email)
        User user = userRepository.findUserByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setRole(Role.ROLE_COMPANY_USER); // default role
            return userRepository.save(newUser);
        });

        return new CustomUserDetails(user, oAuth2User.getAttributes());
    }
}
