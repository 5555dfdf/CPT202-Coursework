package org.example.coursework3.service;

import lombok.RequiredArgsConstructor;
import org.example.coursework3.exception.MsgException;
import org.example.coursework3.dto.request.UpdateSelfInfoRequest;
import org.example.coursework3.entity.User;
import org.example.coursework3.repository.UserRepository;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UpdateInfoService {

    private final UserRepository userRepository;
    public User updateSelfInfo(String userId, UpdateSelfInfoRequest request) {
        User user;
        try {
            user = userRepository.findById(userId);
        } catch (Exception e) {
            throw new MsgException("用户不存在");
        }

        if (request.getName() != null) {
            user.setName(request.getName());
        }

        if (request.getAvatar()!= null) {
            user.setAvatar(request.getAvatar());
        }

        return userRepository.save(user);
    }
}
