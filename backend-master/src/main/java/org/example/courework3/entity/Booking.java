package org.example.courework3.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    private String id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "specialist_id", nullable = false)
    private String specialistId;

    @Column(name = "slot_id", nullable = false)
    private String slotId;

    private String note;

    private String status = "Pending";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "Pending";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
