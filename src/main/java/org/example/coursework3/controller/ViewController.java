package org.example.coursework3.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.coursework3.result.SpecialistListResult;
import org.example.coursework3.service.ViewInfoService;
import org.example.coursework3.vo.ExpertiseVo;
import org.example.coursework3.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin
@Slf4j
public class ViewController {

    @Autowired
    private ViewInfoService viewInfoService;


    @GetMapping("/expertise")
    public Result<List<ExpertiseVo>> getExpertiseList() {

        return Result.success(viewInfoService.getExpertiseList());
    }

    @GetMapping("/specialists")
    public Result<SpecialistListResult> getSpecialistList() {
//        SpecialistListResult result = new SpecialistListResult();
        System.out.println(new SpecialistListResult());
        return Result.success(new SpecialistListResult());
    }
}