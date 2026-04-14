# 系统需求文档：管理员功能模块
## PBI 1: 专家管理
- **User Story**:
  As an administrator, I want to create, update, and manage specialist profiles, so that I can maintain accurate specialist information in the system.

### Acceptance Criteria
GIVEN the administrator accesses the specialist management page, WHEN the administrator enters valid specialist information (name, expertise, price, bio), THEN the system should create a new specialist profile.

GIVEN an existing specialist profile exists, WHEN the administrator updates the specialist information, THEN the system should successfully update the profile.

GIVEN a specialist is no longer active, WHEN the administrator changes the status to "Inactive", THEN the specialist should no longer appear in customer search results.

GIVEN a specialist has no existing bookings, WHEN the administrator deletes the specialist, THEN the specialist profile should be permanently removed from the system.

---

## PBI 2: 专长管理
- **User Story**:
  As an administrator, I want to create, update, and delete expertise categories, so that specialists can be properly classified and customers can filter by relevant fields.

### Acceptance Criteria
GIVEN the administrator accesses the expertise management page, WHEN the administrator enters a new category name and description, THEN the system should create a new expertise category.

GIVEN an existing expertise category exists, WHEN the administrator updates its name or description, THEN the system should save the changes.

GIVEN an expertise category is no longer needed, WHEN the administrator deletes it, THEN the category should be removed from the system.

GIVEN the administrator attempts to delete an expertise category that is linked to one or more specialists, WHEN the administrator confirms deletion, THEN the system should display an error message and prevent deletion.

---

## PBI 3: 时段管理
- **User Story**:
  As an administrator, I want to create, update, and delete specialist availability slots, so that customers can book consultations during available times.

### Acceptance Criteria
GIVEN the administrator selects a specialist, WHEN the administrator enters start time and end time, THEN the system should create a new availability slot.

GIVEN an existing slot exists, WHEN the administrator updates its time or availability status, THEN the system should save the changes.

GIVEN a slot is no longer needed, WHEN the administrator deletes it, THEN the slot should be removed from the system.

GIVEN the administrator views the slot list, WHEN filters are applied by specialist, date, or availability, THEN the system should display only matching slots.

---

## PBI 4: 预约管理与监控
- **User Story**:
  As an administrator, I want to view all bookings and their details, so that I can monitor system activity and assist customers when needed.

### Acceptance Criteria
GIVEN bookings exist in the system, WHEN the administrator accesses the booking list, THEN the system should display all bookings with summary information.

GIVEN the administrator selects a specific booking, WHEN the administrator clicks to view details, THEN the system should display full booking information including customer, specialist, time, and status.

GIVEN the administrator needs to manage a booking, WHEN the administrator cancels or reschedules a booking, THEN the system should update the booking status accordingly.

GIVEN no bookings exist, WHEN the administrator views the booking list, THEN the system should display a message indicating no records.

---

## PBI 5: 价格计算与定价管理
- **User Story**:
  As an administrator, I want to configure pricing rules and calculate consultation fees, so that customers are charged correctly based on specialist and service type.

### Acceptance Criteria
GIVEN a specialist has defined pricing rules, WHEN the administrator calculates a quote for a consultation, THEN the system should return the correct price based on specialist level and duration.

GIVEN pricing rules are updated, WHEN the administrator saves new pricing settings, THEN the system should apply the new rules to future bookings.

GIVEN a booking is created, WHEN the system calculates the total fee, THEN the price should be automatically generated and displayed to the customer.

GIVEN a booking is cancelled within the refund eligibility period, WHEN the refund is processed, THEN the payment status should be updated to "REFUNDED".

---
