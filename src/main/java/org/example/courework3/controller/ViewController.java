package org.example.courework3.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.courework3.service.ViewInfoService;
import org.example.courework3.vo.ExpertiseVo;
import org.example.courework3.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
//@RequestMapping("/auth")
@CrossOrigin
@Slf4j
public class ViewController {

    @Autowired
    private ViewInfoService viewInfoService;

    @GetMapping("/expertise")
    public Result<List<ExpertiseVo>> getExpertiseList() {

        return Result.success(viewInfoService.getExpertiseList());
    }
}