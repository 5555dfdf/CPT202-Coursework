package org.example.coursework3.repository;

import org.example.coursework3.entity.Expertise;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpertiseRepository extends JpaRepository<Expertise, String> {
    @NotNull
    List<Expertise> findAll();

    boolean existsByName(String name);

    Expertise getExpertiseById(String id);
}
