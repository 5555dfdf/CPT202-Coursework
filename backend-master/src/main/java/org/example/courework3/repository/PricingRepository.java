package org.example.courework3.repository;

import org.example.courework3.entity.Pricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PricingRepository extends JpaRepository<Pricing, String> {

    List<Pricing> findBySpecialistId(String specialistId);

    List<Pricing> findBySpecialistIdAndDuration(String specialistId, Integer duration);

    List<Pricing> findBySpecialistIdAndType(String specialistId, String type);

    List<Pricing> findBySpecialistIdAndDurationAndType(String specialistId, Integer duration, String type);
}
