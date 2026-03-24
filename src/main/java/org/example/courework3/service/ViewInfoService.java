package org.example.courework3.service;

import lombok.RequiredArgsConstructor;
import org.example.courework3.entity.Expertise;
import org.example.courework3.repository.ExpertiseRepository;
import org.example.courework3.vo.ExpertiseVo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ViewInfoService {

    private final ExpertiseRepository expertiseRepository;

    public List<ExpertiseVo> getExpertiseList(){
        List<Expertise> expertiseList = expertiseRepository.findAll();
        List<ExpertiseVo> expertiseVoList = new ArrayList<>();
        for (Expertise expertise : expertiseList){
            expertiseVoList.add(ExpertiseVo.toVo(expertise));
        }
        return expertiseVoList;
    }
}
