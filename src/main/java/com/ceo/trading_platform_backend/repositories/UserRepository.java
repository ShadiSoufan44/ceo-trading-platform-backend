package com.ceo.trading_platform_backend.repositories;

import com.ceo.trading_platform_backend.uml_objects.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
