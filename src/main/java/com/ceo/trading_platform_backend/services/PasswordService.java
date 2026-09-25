package com.ceo.trading_platform_backend.services;

import com.ceo.trading_platform_backend.dto.ChangePasswordRequest;
import com.ceo.trading_platform_backend.exception.ResourceNotFoundException;
import com.ceo.trading_platform_backend.uml_objects.User;
import com.ceo.trading_platform_backend.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void changePassword(Integer userId, ChangePasswordRequest request) {
        // TODO: Implement password change functionality
        // 1. Find user by userId
        // 2. Verify currentPassword matches user's stored password
        // 3. Throw exception if password doesn't match
        // 4. Encode new password
        // 5. Update user with new password
        // 6. Save to database
        // 7. Consider logging this action for audit trail
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
