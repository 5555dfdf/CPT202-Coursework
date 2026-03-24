package org.example.courework3.repository;

import org.example.courework3.entity.Expertise;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpertiseRepository extends JpaRepository<Expertise, Long> {
    @NotNull
    List<Expertise> findAll();
}
