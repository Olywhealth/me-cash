package com.mecash.service.auth;

import com.mecash.model.reponse.AuthResponse;
import com.mecash.model.reponse.SignupResponse;
import com.mecash.model.request.LoginRequest;
import com.mecash.model.request.SignupRequest;
import org.springframework.transaction.annotation.Transactional;

public interface AuthService {
    @Transactional
    SignupResponse signup(SignupRequest request);

    AuthResponse login(LoginRequest request);
}
