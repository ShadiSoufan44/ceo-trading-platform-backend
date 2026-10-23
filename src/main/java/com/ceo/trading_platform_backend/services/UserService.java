package com.ceo.trading_platform_backend.services;

import com.ceo.trading_platform_backend.dto.CreateUserRequest;
import com.ceo.trading_platform_backend.dto.UserResponse;
import com.ceo.trading_platform_backend.dto.UpdateInfoRequest;
import com.ceo.trading_platform_backend.enums.UserRole;
import com.ceo.trading_platform_backend.exception.DuplicateResourceException;
import com.ceo.trading_platform_backend.exception.ResourceNotFoundException;
import com.ceo.trading_platform_backend.models.User;
import com.ceo.trading_platform_backend.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }

        // All signup default to CLIENT role
        User user = new User(
                request.fullName(),
                request.email(),
                passwordEncoder.encode(request.password()),
            Instant.now(),
                UserRole.CLIENT
        );

        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
                
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateInfo(UUID userId, UpdateInfoRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        
        if (request.newName() != null) {
            user.setFullName(request.newName());
        }

        if(request.newEmail() != null) {
            if(request.newEmail().equals(user.getEmail())) {
                throw new DuplicateResourceException("New email cannot be the same as your old email.");
            }
            if(userRepository.existsByEmail(request.newEmail())) {
                throw new DuplicateResourceException("This email already registered.");
            }

            user.setEmail(request.newEmail());
        }

        User savedUser = userRepository.save(user);
        return toResponse(savedUser);
    }
    

        public boolean isOwnProfile(UUID userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userId != null
            && authentication != null
            && authentication.isAuthenticated()
            && authentication.getAuthorities().stream()
                .anyMatch(authority -> !authority.getAuthority().equals("ROLE_ANONYMOUS"))
            && userId.toString().equals(authentication.getName());
        }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getJoinDate(),
                user.getRole().name()
        );
    }
}
