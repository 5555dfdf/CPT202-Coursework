package org.example.coursework3.service;

import lombok.RequiredArgsConstructor;
import org.example.coursework3.dto.request.UpdateSelfInfoRequest;
import org.example.coursework3.entity.User;
import org.example.coursework3.exception.MsgException;
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
            throw new MsgException("User not found");
        }

        if (request.getName() != null) {
            String trimmedName = request.getName().trim();
            if (trimmedName.isEmpty()) {
                throw new MsgException("Name cannot be empty");
            }
            user.setName(trimmedName);
        }

        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        boolean wantsToChangePassword =
                hasText(request.getOldPassword()) ||
                        hasText(request.getNewPassword()) ||
                        hasText(request.getConfirmPassword());

        if (wantsToChangePassword) {
            changePassword(user, request.getOldPassword(), request.getNewPassword(), request.getConfirmPassword());
        }

        return userRepository.save(user);
    }

    public void changePassword(String userId, String oldPassword, String newPassword, String confirmPassword) {
        User user;
        try {
            user = userRepository.findById(userId);
        } catch (Exception e) {
            throw new MsgException("User not found");
        }
        changePassword(user, oldPassword, newPassword, confirmPassword);
    }

    public void verifyOldPassword(String userId, String oldPassword) {
        User user;
        try {
            user = userRepository.findById(userId);
        } catch (Exception e) {
            throw new MsgException("User not found");
        }
        if (!hasText(oldPassword)) {
            throw new MsgException("Please enter current password");
        }
        if (!user.getPasswordHash().equals(oldPassword)) {
            throw new MsgException("Old password is incorrect");
        }
    }

    private void changePassword(User user, String oldPassword, String newPassword, String confirmPassword) {
        if (!hasText(oldPassword) || !hasText(newPassword) || !hasText(confirmPassword)) {
            throw new MsgException("oldPassword, newPassword and confirmPassword are required");
        }
        if (!user.getPasswordHash().equals(oldPassword)) {
            throw new MsgException("Old password is incorrect");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new MsgException("New password and confirm password do not match");
        }
        if (newPassword.chars().anyMatch(Character::isWhitespace)) {
            throw new MsgException("New password cannot contain spaces");
        }
        if (newPassword.length() < 8) {
            throw new MsgException("New password must be at least 8 characters");
        }
        if (newPassword.equals(oldPassword)) {
            throw new MsgException("New password must be different from old password");
        }
        user.setPasswordHash(newPassword);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
