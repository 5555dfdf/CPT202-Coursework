系统需求文档 Customer 功能模块 Jiaqi Wang

PBI 1 Specialists 页新增 Page Size 分页控制
User Story
As a customer, I want to change page size on the Specialists page, so that I can control how many specialists are shown per page.
Acceptance Criteria
GIVEN I am on the Specialists page, WHEN I change Page Size, THEN the system should reset to page 1 and request specialist data using the selected pageSize.
GIVEN I use Prev or Next after changing Page Size, WHEN a pagination request is sent, THEN the system should keep the selected pageSize and return correct paging results.

PBI 2 My Bookings 的 Booking Details 改为弹窗内操作
User Story
As a customer, I want booking details to open in a modal from My Bookings, so that I can view and manage a booking without leaving the list context.
Acceptance Criteria
GIVEN I am on My Bookings, WHEN I click Details, THEN booking details should open in a modal and load by booking ID.
GIVEN I perform cancel or reschedule from the modal context, WHEN the action is triggered, THEN the system should execute the same booking operation logic as before.

PBI 3 Customer 页面输入框数量限制统一
User Story
As a customer, I want input fields to enforce clear character limits, so that my submissions are valid and consistent with system constraints.
Acceptance Criteria
GIVEN I am typing in Specialists keyword input, WHEN text length exceeds 100 characters, THEN the system should limit input to 100.
GIVEN I am editing profile name, WHEN text length exceeds 50 characters, THEN the system should limit input to 50.
GIVEN I am filling booking note on specialist slots page, WHEN text length exceeds 300 characters, THEN the system should limit input to 300.
GIVEN I am filling cancel reason in booking details, WHEN text length exceeds 300 characters, THEN the system should limit input to 300 and show limit feedback.
