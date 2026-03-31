package org.example.courework3.repository;

import org.example.courework3.entity.Expertise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpertiseRepository extends JpaRepository<Expertise, String> {
    boolean existsByName(String name);
}