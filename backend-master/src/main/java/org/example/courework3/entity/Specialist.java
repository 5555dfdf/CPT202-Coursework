package org.example.courework3.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "specialists")
public class Specialist {

    @Id
    @Column(name = "user_id", length = 36)
    private String id;

    @Column(nullable = false, length = 50)
    private String name;

    private String bio;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    private String status = "Active";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToMany
    @JoinTable(
            name = "specialist_expertise",
            joinColumns = @JoinColumn(name = "specialist_id", referencedColumnName = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "expertise_id")
    )
    private List<Expertise> expertiseList = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "Active";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
