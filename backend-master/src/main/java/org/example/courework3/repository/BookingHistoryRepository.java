package org.example.courework3.repository;

import org.example.courework3.entity.BookingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingHistoryRepository extends JpaRepository<BookingHistory, String> {

    List<BookingHistory> findByBookingIdOrderByChangedAtDesc(String bookingId);
}
