package org.example.courework3.service;

import lombok.RequiredArgsConstructor;
import org.example.courework3.dto.UpdateSelfInfoRequest;
import org.example.courework3.entity.User;
import org.example.courework3.repository.UserRepository;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UpdateInfoService {

    private final UserRepository userRepository;
    public User updateSelfInfo(String userId, UpdateSelfInfoRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (request.getName() != null) {
            user.setName(request.getName());
        }

        if (request.getAvatar()!= null) {
            user.setAvatar(request.getAvatar());
        }

        return userRepository.save(user);
    }
}
