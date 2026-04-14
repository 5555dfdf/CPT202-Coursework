package org.example.coursework3.repository;

import org.example.coursework3.entity.Slot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface SlotRepository extends JpaRepository<Slot, String> {
    Optional<Slot> findById(String id);

    List<Slot> findBySpecialistId(String id);

    Slot getById(String id);

    Slot getSlotById(String id);
}
