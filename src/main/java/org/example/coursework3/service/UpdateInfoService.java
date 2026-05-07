package org.example.coursework3.service;

import lombok.RequiredArgsConstructor;
import org.example.coursework3.dto.request.UpdateSelfInfoRequest;
import org.example.coursework3.entity.User;
import org.example.coursework3.exception.MsgException;
import org.example.coursework3.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateInfoService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    /**
     * Updates the basic profile information of the current user.
     * Optionally triggers a password change if password fields are provided in the request.
     *
     * @param userId  The unique identifier of the user to be updated.
     * @param request A DTO containing the potential updates (name, avatar, and password fields).
     * @return The updated User entity after persistence.
     * @throws MsgException if the user is not found or if the name is blank.
     */
    public User updateSelfInfo(String userId, UpdateSelfInfoRequest request) {
        User user;
        try {
            user = userRepository.findById(userId);
        } catch (Exception e) {
            throw new MsgException("User not found");
        }
        // Update basic profile fields
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
        // Check if the user intends to update their password
        boolean wantsToChangePassword =
                hasText(request.getOldPassword()) ||
                        hasText(request.getNewPassword()) ||
                        hasText(request.getConfirmPassword());

        if (wantsToChangePassword) {
            changePassword(user, request.getOldPassword(), request.getNewPassword(), request.getConfirmPassword());
        }

        return userRepository.save(user);
    }
    /**
     * Independent method to update a user's password.
     *
     * @param userId          The ID of the user.
     * @param oldPassword     The current password for verification.
     * @param newPassword     The desired new password.
     * @param confirmPassword The re-entry of the new password for verification.
     */
    public void changePassword(String userId, String oldPassword, String newPassword, String confirmPassword) {
        User user;
        try {
            user = userRepository.findById(userId);
        } catch (Exception e) {
            throw new MsgException("User not found");
        }
        changePassword(user, oldPassword, newPassword, confirmPassword);
    }
    /**
     * Verifies if the provided password matches the one stored in the database.
     * Useful for pre-verification steps in high-security UI flows.
     *
     * @param userId      The ID of the user.
     * @param oldPassword The plain-text password to verify.
     * @throws MsgException if the password is blank or incorrect.
     */
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
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new MsgException("Old password is incorrect");
        }
    }
    /**
     * Internal logic for password modification.
     * Performs strict validation on password matching and uniqueness compared to the old password.
     *
     * @param user            The user entity to modify.
     * @param oldPassword     The plain-text current password.
     * @param newPassword     The plain-text new password.
     * @param confirmPassword The confirmation of the new password.
     * @throws MsgException for various validation failures.
     */
    private void changePassword(User user, String oldPassword, String newPassword, String confirmPassword) {
        if (!hasText(oldPassword) || !hasText(newPassword) || !hasText(confirmPassword)) {
            throw new MsgException("oldPassword, newPassword and confirmPassword are required");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new MsgException("Old password is incorrect");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new MsgException("New password and confirm password do not match");
        }
        if (newPassword.equals(oldPassword)) {
            throw new MsgException("New password must be different from old password");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    /**
     * Utility method to determine if a string contains actual text content.
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
