package org.example.coursework3.controller;

import lombok.RequiredArgsConstructor;
import org.example.coursework3.result.Result;
import org.example.coursework3.service.SlotInfoService;
import org.example.coursework3.service.SpecialistsInfoService;
import org.example.coursework3.vo.SlotVo;
import org.example.coursework3.vo.SpecialistsDetailVo;
import org.example.coursework3.vo.SpecialistsPageVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping
@RequiredArgsConstructor
public class SpecialistsController {

    @Autowired
    private SlotInfoService slotInfoService;

    private final SpecialistsInfoService specialistInfoService;


    @GetMapping("/specialists")
    public Result<SpecialistsPageVo> getSpecialists(
            @RequestParam(required = false) String expertiseId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return Result.success(specialistInfoService.getSpecialists(expertiseId, page, pageSize));
    }

    @GetMapping("/specialists/{id}")
    public Result<SpecialistsDetailVo> getSpecialist(@PathVariable String id) {
        return Result.success(specialistInfoService.getSpecialistDetail(id));
    }

    @GetMapping("/specialists/{id}/slots")
    public Result<List<SlotVo>> getSpecialistSlots(@PathVariable("id") String specialistId,
                                                   @RequestParam(required = false) String date,
                                                   @RequestParam(required = false) String from,
                                                   @RequestParam(required = false) String to){


        List<SlotVo> slots = slotInfoService.getSpecialistSlots(specialistId,date,from,to);
        return Result.success(slots);
    }
}
