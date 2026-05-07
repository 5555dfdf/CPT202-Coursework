package org.example.coursework3.service;

import lombok.RequiredArgsConstructor;
import org.example.coursework3.entity.Expertise;
import org.example.coursework3.repository.ExpertiseRepository;
import org.example.coursework3.vo.ExpertiseVo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ViewInfoService {

    private final ExpertiseRepository expertiseRepository;
    /**
     * Retrieves all available expertise categories from the database.
     * This list is typically used to populate dropdowns or filter menus in the UI.
     *
     * @return A list of {@link ExpertiseVo} objects representing all registered expertise types.
     */
    public List<ExpertiseVo> getExpertiseList(){
        // Retrieve all Expertise entities from the database
        List<Expertise> expertiseList = expertiseRepository.findAll();
        // Map the entity list to a list of VO for the API response
        List<ExpertiseVo> expertiseVoList = new ArrayList<>();
        for (Expertise expertise : expertiseList){
            expertiseVoList.add(ExpertiseVo.toVo(expertise));
        }
        return expertiseVoList;
    }
}
