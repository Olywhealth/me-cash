package com.mecash.security;

import com.mecash.entity.AccountUser;
import com.mecash.repository.UserRepository;
import java.util.Collections;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads users for Spring Security by email (our username). Returns the framework's own
 * {@link org.springframework.security.core.userdetails.User} so the password encoder can
 * verify the stored BCrypt hash during login.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AccountUser accountUser = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("AccountUser not found: " + email));
        return org.springframework.security.core.userdetails.User.builder()
                .username(accountUser.getEmail())
                .password(accountUser.getPassword())
                .authorities(Collections.emptyList())
                .build();
    }
}
