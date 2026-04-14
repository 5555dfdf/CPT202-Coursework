package org.example.coursework3.repository;

import org.example.coursework3.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    User findById(String id);

    User getUserById(String id);
}