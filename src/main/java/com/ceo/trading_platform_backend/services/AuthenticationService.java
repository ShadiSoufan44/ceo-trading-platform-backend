package com.ceo.trading_platform_backend.services;

import com.ceo.trading_platform_backend.dto.LoginRequest;
import com.ceo.trading_platform_backend.dto.LoginResponse;
import com.ceo.trading_platform_backend.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // TODO: Implement login functionality
        // 1. Find user by email
        // 2. Validate password using passwordEncoder.matches(plaintext, hashed)
        // 3. Generate JWT token (need JwtTokenProvider or similar)
        // 4. Return LoginResponse with token
        // 5. Handle invalid credentials with appropriate exception
        
        return null;
    }
}
