package com.adoptify.backend.repository;

import com.adoptify.backend.model.User;
import com.adoptify.backend.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    List<User> findByRole(UserRole role);

    List<User> findByIsVerifiedFalse();
    
    List<User> findByRoleAndIsVerifiedTrue(UserRole role);

    List<User> findByLatitudeBetweenAndLongitudeBetween(Double minLat, Double maxLat, Double minLng, Double maxLng);
}
