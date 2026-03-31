package org.example.courework3.repository;

import org.example.courework3.entity.Slot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SlotRepository extends JpaRepository<Slot, String> {
    List<Slot> findBySpecialistIdOrderByStartTimeAsc(String specialistId);
    List<Slot> findByAvailableTrueAndSpecialistIdOrderByStartTimeAsc(String specialistId);
}
