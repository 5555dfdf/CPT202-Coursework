package org.example.coursework3.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpecialistsPageVo {
    private List<SpecialistsVo> items;
    private long total;
    private int page;
    private int pageSize;
}

