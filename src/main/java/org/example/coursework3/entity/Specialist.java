package org.example.coursework3.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "specialists")
@Data
public class Specialist {
    @Id
    @Column(name = "user_id", length = 36, updatable = false, nullable = false)
    private String userId;
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpecialistStatus status = SpecialistStatus.Active;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price = BigDecimal.valueOf(50);

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "specialist_expertise",
            joinColumns = @JoinColumn(name = "specialist_id", referencedColumnName = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "expertise_id")
    )
    private List<Expertise> expertises = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Specialist(String id, String name, BigDecimal price, String bio) {
        this.userId = id;
        this.name = name;
        this.price = price;
        this.bio = bio;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        syncFromUser();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        syncFromUser();
    }

    private void syncFromUser() {
        if (user != null) {
            this.userId = user.getId();
            this.name = user.getName();
        }
    }
}
