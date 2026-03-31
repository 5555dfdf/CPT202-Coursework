package org.example.courework3.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "booking_history")
public class BookingHistory {

    @Id
    @UuidGenerator
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    @Column(name = "booking_id", nullable = false, length = 36)
    private String bookingId;

    @Column(length = 20)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }
}
