package org.example.coursework3.repository;

import org.example.coursework3.entity.Booking;
import org.example.coursework3.entity.BookingStatus;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface BookingRepository extends JpaRepository<Booking,String> {

    Page<Booking> findBySpecialistId(String specialistId, Pageable pageable);

    Page<Booking> findBySpecialistIdAndStatus(String specialistId, BookingStatus status, Pageable pageable);
    @NotNull
    Optional<Booking> findById(String id);

    List<Booking> findByStatusAndCreatedAtBefore(BookingStatus status, LocalDateTime createdAt);

    List<Booking> findByStatusAndUpdatedAtBefore(BookingStatus status, LocalDateTime updatedAt);
}
