package org.example.courework3.repository;

import org.example.courework3.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {
    List<Booking> findByCustomerIdOrderByCreatedAtDesc(String customerId);
    List<Booking> findBySpecialistIdOrderByCreatedAtDesc(String specialistId);
}
