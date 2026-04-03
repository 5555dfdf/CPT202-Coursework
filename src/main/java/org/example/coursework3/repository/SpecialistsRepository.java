package org.example.coursework3.repository;

import org.example.coursework3.entity.Specialist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecialistsRepository extends JpaRepository<Specialist, String> {
    Page<Specialist> findDistinctByExpertises_Id(String expertiseId, Pageable pageable);
}

