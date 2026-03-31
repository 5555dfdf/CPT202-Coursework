package org.example.courework3.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "pricing")
public class Pricing {

    @Id
    @UuidGenerator
    @Column(length = 36, updatable = false, nullable = false)
    private String id;

    @Column(name = "specialist_id", nullable = false, length = 36)
    private String specialistId;

    @Column(nullable = false)
    private Integer duration;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(length = 10)
    private String currency = "CNY";

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
