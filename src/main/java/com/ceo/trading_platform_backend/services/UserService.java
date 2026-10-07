package com.ceo.trading_platform_backend.services;

import com.ceo.trading_platform_backend.dto.CreateUserRequest;
import com.ceo.trading_platform_backend.dto.UserResponse;
import com.ceo.trading_platform_backend.exception.DuplicateResourceException;
import com.ceo.trading_platform_backend.exception.ResourceNotFoundException;
import com.ceo.trading_platform_backend.models.User;
import com.ceo.trading_platform_backend.models.Client;
import com.ceo.trading_platform_backend.models.Analyst;
import com.ceo.trading_platform_backend.models.Administrator;
import com.ceo.trading_platform_backend.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /* 
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }

        // All signup default to CLIENT role
        Client client = new Client(
                request.fullName(),
                request.email(),
                passwordEncoder.encode(request.password()),
                OffsetDateTime.now()
        );

        return toResponse(userRepository.save(client));
    }
    */

    public boolean isOwnProfile(UUID userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        
        // Get the authenticated user's ID (assumes principal is User object or string ID)
        // This will be set properly once JWT authentication is implemented
        try {
            String principal = auth.getName();
            return principal.equals(userId.toString());
        } catch (Exception e) {
            return false;
        }
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getJoinDate(),
                getRoleFromUser(user)
        );
    }

    private String getRoleFromUser(User user) {
        if (user instanceof Client) {
            return "CLIENT";
        } else if (user instanceof Analyst) {
            return "ANALYST";
        } else if (user instanceof Administrator) {
            return "ADMIN";
        }
        return "CLIENT"; // Default fallback
    }
}
