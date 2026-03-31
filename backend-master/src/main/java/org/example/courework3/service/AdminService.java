package org.example.courework3.service;

import lombok.RequiredArgsConstructor;
import org.example.courework3.entity.Expertise;
import org.example.courework3.entity.Role;
import org.example.courework3.entity.Specialist;
import org.example.courework3.entity.User;
import org.example.courework3.repository.ExpertiseRepository;
import org.example.courework3.repository.SpecialistRepository;
import org.example.courework3.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final ExpertiseRepository expertiseRepository;
    private final SpecialistRepository specialistRepository;
    private final UserRepository userRepository;

    // 1. 创建专家：邮箱已存在则关联并升级角色；不存在则用密码新建用户（与前台注册一致，无需先注册）
    @Transactional
    public Specialist createSpecialist(Map<String, Object> payload) {
        String userEmail = (String) payload.get("userEmail");
        if (userEmail == null || userEmail.isBlank()) {
            throw new RuntimeException("用户邮箱不能为空");
        }
        userEmail = userEmail.trim();

        String displayName = (String) payload.get("name");
        if (displayName == null || displayName.isBlank()) {
            throw new RuntimeException("专家姓名不能为空");
        }
        displayName = displayName.trim();

        User user = userRepository.findByEmail(userEmail).orElse(null);

        if (user != null) {
            if (user.getRole() == Role.Admin) {
                throw new RuntimeException("该邮箱已是管理员账号，不能添加为专家");
            }
            if (specialistRepository.existsById(user.getId())) {
                throw new RuntimeException("该用户已是专家");
            }
            user.setRole(Role.Specialist);
            if (!displayName.equals(user.getName())) {
                user.setName(displayName);
            }
            userRepository.save(user);
        } else {
            Object pwdObj = payload.get("password");
            String password = pwdObj != null ? pwdObj.toString().trim() : "";
            if (password.isEmpty()) {
                throw new RuntimeException("该邮箱尚未注册，请填写「初始密码」以同时创建登录账号");
            }
            user = new User();
            user.setEmail(userEmail);
            user.setName(displayName);
            user.setPasswordHash(password);
            user.setRole(Role.Specialist);
            userRepository.save(user);
        }

        Specialist specialist = new Specialist();
        specialist.setId(user.getId());
        specialist.setName(displayName);
        specialist.setBio((String) payload.get("bio"));

        if (payload.get("price") != null) {
            specialist.setPrice(new BigDecimal(payload.get("price").toString()));
        }

        List<String> expertiseIds = (List<String>) payload.get("expertiseIds");
        if (expertiseIds != null && !expertiseIds.isEmpty()) {
            List<Expertise> expertiseList = expertiseRepository.findAllById(expertiseIds);
            specialist.setExpertiseList(expertiseList);
        }

        return specialistRepository.save(specialist);
    }

    // 2. 更新专家信息
    @Transactional
    public Specialist updateSpecialist(String id, Map<String, Object> payload) {
        Specialist specialist = specialistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("专家不存在"));

        if (payload.get("name") != null) {
            specialist.setName((String) payload.get("name"));
        }

        if (payload.get("bio") != null) {
            specialist.setBio((String) payload.get("bio"));
        }

        if (payload.get("price") != null) {
            specialist.setPrice(new BigDecimal(payload.get("price").toString()));
        }

        if (payload.get("expertiseIds") != null) {
            List<String> expertiseIds = (List<String>) payload.get("expertiseIds");
            List<Expertise> expertiseList = expertiseRepository.findAllById(expertiseIds);
            specialist.setExpertiseList(expertiseList);
        }

        return specialistRepository.save(specialist);
    }

    // 3. 设置专家状态
    @Transactional
    public Specialist setSpecialistStatus(String id, String status) {
        Specialist specialist = specialistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("专家不存在"));

        if (!status.equals("Active") && !status.equals("Inactive")) {
            throw new RuntimeException("状态必须是 Active 或 Inactive");
        }

        specialist.setStatus(status);
        return specialistRepository.save(specialist);
    }

    // 4. 删除专家
    @Transactional
    public void deleteSpecialist(String id) {
        if (!specialistRepository.existsById(id)) {
            throw new RuntimeException("专家不存在");
        }
        specialistRepository.deleteById(id);
    }

    // 5. 创建专长
    @Transactional
    public Expertise createExpertise(String name, String description) {
        if (expertiseRepository.existsByName(name)) {
            throw new RuntimeException("专长名称已存在");
        }

        Expertise expertise = new Expertise();
        expertise.setId(UUID.randomUUID().toString());
        expertise.setName(name);
        expertise.setDescription(description);

        return expertiseRepository.save(expertise);
    }

    // 6. 更新专长
    @Transactional
    public Expertise updateExpertise(String id, String name, String description) {
        Expertise expertise = expertiseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("专长不存在"));

        if (name != null && !name.equals(expertise.getName())) {
            if (expertiseRepository.existsByName(name)) {
                throw new RuntimeException("专长名称已存在");
            }
            expertise.setName(name);
        }

        if (description != null) {
            expertise.setDescription(description);
        }

        return expertiseRepository.save(expertise);
    }

    // 7. 删除专长
    @Transactional
    public void deleteExpertise(String id) {
        if (!expertiseRepository.existsById(id)) {
            throw new RuntimeException("专长不存在");
        }
        expertiseRepository.deleteById(id);
    }
}
