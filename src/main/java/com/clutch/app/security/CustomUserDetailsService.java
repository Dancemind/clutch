package com.clutch.app.security;

import com.clutch.app.entity.User;
import com.clutch.app.exceptions.UserNotFoundException;
import com.clutch.app.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CustomUserDetailsService implements UserDetailsService {
    public static final String USER_NOT_FOUND_MESSAGE = "Username not found";
    public static final String USER_NOT_FOUND_BY_EMAIL_PATTERN = "Username not found: %s";

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> {
                    log.warn(USER_NOT_FOUND_MESSAGE);
                    return new UserNotFoundException(
                            String.format(USER_NOT_FOUND_BY_EMAIL_PATTERN, email)
                    );
                });
        return new CustomUserDetails(user);
    }

}
