package org.example.coursework3.service;

import lombok.RequiredArgsConstructor;
import org.example.coursework3.dto.request.CreateSpecialistRequest;
import org.example.coursework3.dto.request.EditSpecialistRequest;
import org.example.coursework3.entity.*;
import org.example.coursework3.exception.MsgException;
import org.example.coursework3.repository.ExpertiseRepository;
import org.example.coursework3.repository.SpecialistsRepository;
import org.example.coursework3.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private  final SpecialistsRepository specialistsRepository;
    private final ExpertiseRepository expertiseRepository;

@Transactional
    public Specialist createSpecialist(CreateSpecialistRequest request) {
        if (userRepository.findByEmail(request.getUserEmail()).isPresent()) {
            throw new MsgException("该邮箱已被注册");
        }

        // 1. 保存用户
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getUserEmail());
        user.setRole(Role.Specialist);
        user.setPasswordHash(request.getPassword());
        userRepository.save(user);

        // 2. 创建专家（userId = user.getId()）
        Specialist specialist = new Specialist(user.getId(), user.getName(), request.getPrice(), request.getBio());
        List<Expertise> expertiseList = new ArrayList<>();
        for (String expertiseId : request.getExpertiseIds()) {
            Expertise expertise = expertiseRepository.findById(expertiseId)
                    .orElseThrow(() -> new MsgException("专长不存在"));
            expertiseList.add(expertise);
        }
        specialist.setExpertises(expertiseList);
        specialistsRepository.save(specialist); // JPA 自动维护中间表specialist_expertise
    return specialist;
}
    public Specialist updateSpecialist(String id, EditSpecialistRequest request) {
        Specialist specialist;
            try {
                specialist = specialistsRepository.getByUserId(id);
            } catch (Exception e) {
                throw new MsgException("该专家不存在");
            }
            if (request.getName() != null) {
                User user = userRepository.findById(id);
                user.setName(request.getName());
                userRepository.save(user);
                specialist.setName(request.getName());
            }
            if (request.getBio() != null) {
                specialist.setBio(request.getBio());
            }
            if (request.getPrice() != null) {
                specialist.setPrice(request.getPrice());
            }
            if (request.getStatus() != null) {
                specialist.setStatus(request.getStatus());
            }
            if (request.getExpertiseIds() != null){
                List<Expertise> expertiseList = new ArrayList<>();
                for (String expertiseId : request.getExpertiseIds()) {
                    Expertise expertise = expertiseRepository.findById(expertiseId)
                            .orElseThrow(() -> new MsgException("专长不存在"));
                    expertiseList.add(expertise);
                }
                specialist.setExpertises(expertiseList);
            }
            specialistsRepository.save(specialist);
    return specialist;
    }

    @Transactional
    public Specialist updateSpecialistStatus(String id, SpecialistStatus status) {
        Specialist specialist;
        try {
            specialist = specialistsRepository.getByUserId(id);
        } catch (Exception e) {
            throw new MsgException("该专家不存在");
        }
        specialist.setStatus(status);
        specialistsRepository.save(specialist);
        return specialist;
    }

    @Transactional
    public void deleteSpecialist(String id) {
        User user = userRepository.getUserById(id);
        Specialist specialist = specialistsRepository.getByUserId(id);
        specialistsRepository.delete(specialist);
        userRepository.delete(user);
    }

    public Expertise createExpertise(String name, String description) {
        if (expertiseRepository.existsByName(name)){
            throw new MsgException("该专家长已存在");
        }
        Expertise expertise = new Expertise();
        expertise.setName(name);
        expertise.setDescription(description);
        expertiseRepository.save(expertise);
        return expertise;
    }

    @Transactional
    public Expertise updateExpertise(String id, String name, String description) {
        Expertise expertise = null;
        try {
            expertise = expertiseRepository.getExpertiseById(id);
        } catch (Exception e) {
            throw new MsgException("该专长不存在");
        }
        expertise.setName(name);
        expertise.setDescription(description);
        expertiseRepository.save(expertise);
        return expertise;
    }

    public void deleteExpertise(String id) {
        if (!expertiseRepository.existsById(id)){
            throw new MsgException("请输入有效专长ID");
        }
        expertiseRepository.deleteById(id);
    }
}
