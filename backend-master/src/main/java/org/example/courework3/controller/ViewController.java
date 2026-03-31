package org.example.courework3.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.courework3.entity.Expertise;
import org.example.courework3.repository.ExpertiseRepository;
import org.example.courework3.vo.ExpertiseVo;
import org.example.courework3.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/expertise")
@CrossOrigin
@Slf4j
@RequiredArgsConstructor
public class ViewController {

    private final ExpertiseRepository expertiseRepository;

    @GetMapping
    public Result<List<ExpertiseVo>> getExpertiseList() {
        List<Expertise> allExpertise = expertiseRepository.findAll();
        List<ExpertiseVo> list = allExpertise.stream()
                .map(e -> new ExpertiseVo(e.getId(), e.getName(), e.getDescription()))
                .collect(Collectors.toList());
        return Result.success(list);
    }
}