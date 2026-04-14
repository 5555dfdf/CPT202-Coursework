package org.example.coursework3.service;

import lombok.RequiredArgsConstructor;
import org.example.coursework3.entity.Expertise;
import org.example.coursework3.entity.Specialist;
import org.example.coursework3.exception.MsgException;
import org.example.coursework3.repository.SpecialistsRepository;
import org.example.coursework3.vo.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SpecialistsInfoService {

    private final SpecialistsRepository specialistRepository;

    @Transactional(readOnly = true)
    public SpecialistsDetailVo getSpecialistDetail(String id) {
        Specialist specialist = specialistRepository.findById(id).orElseThrow(() -> new RuntimeException("specialist not found: " + id));
        List<SpecialistsExpertiseBriefVo> expertise = specialist.getExpertises().stream().map(e -> new SpecialistsExpertiseBriefVo(e.getId(), e.getName())).toList();

        return new SpecialistsDetailVo(
                specialist.getUserId(),
                specialist.getName(),
                specialist.getBio(),
                expertise,
                specialist.getPrice()
        );
    }

    @Transactional(readOnly = true)
    public SpecialistsPageVo getSpecialists(String expertiseId, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(pageSize, 1);
        Pageable pageable = PageRequest.of(safePage - 1, safePageSize);
        Page<Specialist> specialistPage;

        if (expertiseId != null && !expertiseId.isBlank()) {
            specialistPage = specialistRepository.findDistinctByExpertises_Id(expertiseId, pageable);
        } else {
            specialistPage = specialistRepository.findAll(pageable);
        }

        List<SpecialistsVo> items = specialistPage.getContent().stream().map(s -> {
            List<String> expertiseIds = s.getExpertises().stream().map(Expertise::getId).toList();
            return new SpecialistsVo(s.getUserId(), s.getName(), s.getStatus(), expertiseIds, s.getPrice());
        }).toList();

        return new SpecialistsPageVo(
                items,
                specialistPage.getTotalElements(),
                safePage,
                safePageSize
        );
    }

}

