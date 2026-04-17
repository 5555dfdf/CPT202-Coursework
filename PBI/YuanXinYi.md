
### PBI1: Update Profile Information
**User Story:**
- As a user, I want to update my profile name and avatar so that my account information stays accurate.

**Acceptance Criteria:**
- GIVEN the user has logged into the system, WHEN the user opens the profile page, THEN the system should display the user's current name, email address, user role, and default avatar.
- GIVEN the user is on the profile page, WHEN the user views her email address and user role, THEN the system should display the email address and user role as read-only and shall not allow them to be edited.
- GIVEN the user is on the profile page, WHEN the user enters a valid new name and clicks the save button, THEN the system shall update the name successfully and display the updated name on the profile page.
- GIVEN the user is on the profile page, WHEN the user leaves the name field empty and attempts to save the changes, THEN the system shall reject the update and display an error message.
- GIVEN the user is on the profile page, WHEN the user clicks save without changing the name, THEN the system shall not submit an update request and shall display a short informational message.
- GIVEN the user has successfully updated her name, WHEN the updated profile information is displayed, THEN the system shall automatically refresh the default avatar to show the first letter of the new name.
- GIVEN the user's default avatar is displayed on the profile page, WHEN the profile information is loaded or updated, THEN the system shall display the avatar with a solid-colour background and the initial of the current name.

### PBI2: Change Password
**User Story:**
- As a user, I want to change my password, so that my account is more secure.

**Acceptance Criteria:**
- GIVEN the user has logged into the system, WHEN the user opens the password change section on the profile page, THEN the system shall display fields for current password, new password, and password confirmation.
- GIVEN the user is on the password change section, WHEN the user enters the correct current password, a valid new password, and a matching confirmation password, then submits the form, THEN the system shall update the password successfully and display a success message.
- GIVEN the user is on the password change form, WHEN the user enters an incorrect current password and leaves the current password field, THEN the system shall display an error message indicating that the current password is wrong.
- GIVEN the user is on the password change form, WHEN the user focuses the confirmation password field without entering a valid new password, THEN the system shall display a message to ask the user to enter a valid new password first.
- GIVEN the user is on the password change form, WHEN the user enters a new password that is fewer than 8 characters and leaves the new password field, THEN the system shall display an error message: "The password must be at least 8 characters".
- GIVEN the user is on the password change form, WHEN the user enters a confirmation password that does not match the new password and submits the form, THEN the system shall reject the password update and display an error message.
- GIVEN the user is on the password change form, WHEN the user enters whitespace in password fields, THEN the system shall not accept whitespace characters as part of the password.
- GIVEN the password update succeeds, WHEN the success message is shown, THEN the message shall auto-dismiss after a short duration.
