package com.mecash.service.auth;

import com.mecash.entity.Account;
import com.mecash.service.account.AccountService;
import com.mecash.model.reponse.AccountResponse;
import com.mecash.model.reponse.AuthResponse;
import com.mecash.model.request.LoginRequest;
import com.mecash.model.request.SignupRequest;
import com.mecash.model.reponse.SignupResponse;
import com.mecash.common.exception.ConflictException;
import com.mecash.config.MecashProperties;
import com.mecash.security.JwtService;
import com.mecash.entity.AccountUser;
import com.mecash.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService{

    private final UserRepository userRepository;
    private final AccountService AccountService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final long tokenExpirationMs;

    public AuthServiceImpl(UserRepository userRepository,
                           AccountService AccountService,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtService jwtService,
                           MecashProperties properties) {
        this.userRepository = userRepository;
        this.AccountService = AccountService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.tokenExpirationMs = properties.getJwt().getExpirationMs();
    }

    /**
     * Registers a user and atomically opens their first account. The email is normalised to
     * lower case and the password is stored only as a BCrypt hash.
     */
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        AccountUser accountUser = userRepository.save(AccountUser.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .build());

        Account account = AccountService.openAccountForNewUser(accountUser);
        return new SignupResponse(accountUser.getEmail(), AccountResponse.from(account));
    }

    /**
     * Verifies credentials through the Spring Security {@link AuthenticationManager} (which
     * checks the BCrypt hash) and issues a JWT. Bad credentials surface as a 401.
     */
    @Override
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));
        String token = jwtService.generateToken(email);
        return AuthResponse.bearer(token, tokenExpirationMs, email);
    }
}
