package org.example.coursework3.repository;

import org.example.coursework3.entity.Pricing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PricingRepository extends JpaRepository<Pricing, String> {

    List<Pricing> findBySpecialistIdAndDurationAndType(
            String specialistId,
            Integer duration,
            String type
    );

    List<Pricing> findAllByOrderByUpdatedAtDescCreatedAtDesc();

    List<Pricing> findBySpecialistIdOrderByUpdatedAtDescCreatedAtDesc(String specialistId);

    boolean existsBySpecialistIdAndDurationAndType(String specialistId, Integer duration, String type);

    boolean existsBySpecialistIdAndDurationAndTypeAndIdNot(String specialistId, Integer duration, String type, String id);

    List<Pricing> findFirstBySpecialistIdAndDurationOrderByCreatedAtDesc(
            String specialistId,
            Integer duration
    );

    List<Pricing> findFirstBySpecialistIdAndTypeOrderByCreatedAtDesc(
            String specialistId,
            String type
    );

    List<Pricing> findFirstBySpecialistIdOrderByCreatedAtDesc(String specialistId);
}
