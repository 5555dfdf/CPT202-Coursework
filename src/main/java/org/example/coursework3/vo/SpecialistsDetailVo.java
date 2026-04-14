package org.example.coursework3.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpecialistsDetailVo {
    private String id;
    private String name;
    private String bio;
    private List<SpecialistsExpertiseBriefVo> expertise;
    private BigDecimal price;
}

