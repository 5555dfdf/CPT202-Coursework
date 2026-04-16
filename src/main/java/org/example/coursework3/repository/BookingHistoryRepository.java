package org.example.coursework3.repository;


import org.example.coursework3.entity.BookingHistory;
import org.example.coursework3.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingHistoryRepository extends JpaRepository<BookingHistory, String> {
    boolean existsByBookingIdAndStatus(String bookingId, BookingStatus status);


    BookingHistory getByBookingIdAndStatus(String id, BookingStatus status);
}
