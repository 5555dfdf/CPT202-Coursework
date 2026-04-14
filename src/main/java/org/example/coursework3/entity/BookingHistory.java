package org.example.coursework3.entity;


import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "booking_history")
@Data
public class BookingHistory {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "booking_id", nullable = false, length = 36)
    private String bookingId;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.Pending;


    @Column(columnDefinition = "TEXT")
    private String reason;

    @UpdateTimestamp
    @Column(name = "changed_at")
    private LocalDateTime changedAt;
}
