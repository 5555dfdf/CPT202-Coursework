package org.example.courework3.repository;

import org.example.courework3.entity.Specialist;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecialistRepository extends JpaRepository<Specialist, String> {

    @Override
    @EntityGraph(attributePaths = {"expertiseList"})
    List<Specialist> findAll();

    @Override
    @EntityGraph(attributePaths = {"expertiseList"})
    Optional<Specialist> findById(String id);
}
