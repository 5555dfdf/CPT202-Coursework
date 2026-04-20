Mengxi Yang
## 六、定价（Pricing）

### 1. 获取价格报价

- **URL**: `/pricing/quote`
- **方法**: `POST`
- **请求头**: 需要 `Authorization`（视业务而定，如仅登录用户可报价）
- **请求体 `payload`**（示例）:

| 字段名       | 类型   | 必填 | 说明               |
| ------------ | ------ | ---- | ------------------ |
| specialistId | string | 是   | 专家 ID            |
| duration     | number | 否   | 时长（分钟）       |
| type         | string | 否   | 服务类型，如 online |

- **响应示例**:

```json
{
  "amount": 300,
  "currency": "CNY",
  "detail": "60 分钟在线咨询"
}
```

对应Pbi:
User Story

As an administrator, I want to view all pricing rules, so that I can monitor and manage the pricing settings efficiently.

Acceptance Criteria
GIVEN I am an administrator
WHEN I open the pricing management page
THEN I should see a list of all pricing rules.

GIVEN I am an administrator
WHEN I search by specialist ID, duration, and service type
THEN the system should return the matching pricing rules.

GIVEN I am an administrator
WHEN I search by specialist ID, but no duration or service type
THEN the system should return all possible matching pricing rules.

GIVEN I am an administrator
WHEN I search by the three keys but they are not matched
THEN the system should return an error messege.



备注：
前端参数交给王佳琪已经更新过了

后端改了部分字段

UserRepository里：
public interface UserRepository extends JpaRepository<User, String> {
Optional<User> findByEmail(String email);
}    
另，修改yml文档
url: jdbc:mysql://localhost:3306/booking_system?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC

---

补充（支付流程）

### PBI：下单后支付二维码流程

- **User Story**  
  As a customer, I want to complete payment right after creating a booking, so that I can clearly finish the booking process before entering `My Bookings`.

- **Acceptance Criteria**
  - GIVEN I am a customer and have selected a valid slot, WHEN I submit booking, THEN the system should create booking first and open a payment QR modal instead of directly jumping to booking list.
  - GIVEN payment order is created successfully, WHEN QR modal is displayed, THEN the modal should show booking info, amount, and a scannable QR code.
  - GIVEN I have finished payment, WHEN I click `Paid`, THEN frontend should call payment confirm API and show success message.
  - GIVEN payment confirm succeeds, WHEN success modal closes, THEN page should navigate to `My Bookings`.
  - GIVEN Alipay config is missing/invalid, WHEN customer triggers payment creation, THEN frontend should show explicit error and not fake success.

### PBI：支付接口拆分（3个）

#### PBI 1 - 创建支付单接口

- **接口**: `POST /bookings/{id}/payment`
- **User Story**:  
  As a customer, I want the system to create an Alipay order after booking creation, so that I can pay by scanning a QR code.
- **Acceptance Criteria**:
  - GIVEN booking belongs to current customer, WHEN calling create payment API, THEN system returns paymentId/paymentToken/qrCodeUrl/amount/currency.
  - GIVEN Alipay key config is missing, WHEN calling API, THEN system returns a clear error and no fake QR is generated.

#### PBI 2 - 支付确认接口

- **接口**: `POST /bookings/{id}/payment/confirm`
- **User Story**:  
  As a customer, I want to confirm payment result from backend verification, so that the frontend can reliably show payment success.
- **Acceptance Criteria**:
  - GIVEN payment exists and belongs to current customer, WHEN clicking `Paid`, THEN backend queries Alipay trade status.
  - GIVEN Alipay status is `TRADE_SUCCESS` or `TRADE_FINISHED`, WHEN confirm API returns, THEN response contains `paymentStatus=SUCCESS`.
  - GIVEN payment is not completed, WHEN confirm API is called, THEN system returns an explicit non-success message.

#### PBI 3 - 支付宝回调接口

- **接口**: `POST /bookings/alipay/notify`
- **User Story**:  
  As a system, I want to process Alipay async notifications, so that payment status can be updated even without manual frontend confirmation.
- **Acceptance Criteria**:
  - GIVEN callback is received, WHEN signature verification passes and trade status is success, THEN backend marks draft payment as paid.
  - GIVEN signature verification fails or trade status is not success, WHEN callback is processed, THEN backend returns failure and does not mark paid.

 - 前端

- 预约提交流程改造：`createBooking` 后不再直接跳转，改为弹出支付二维码弹窗。
- 支付弹窗交互完成：展示 bookingId、slot、金额、二维码；支持 `Pay Later` 与 `Paid` 两个操作按钮。
- 支付确认流程接入：`Paid` 点击后调用支付确认接口，成功提示后跳转 `My Bookings`。
-

 - 接口/后端

- 新增客户支付接口：
  - `POST /bookings/{id}/payment`（创建支付单）
  - `POST /bookings/{id}/payment/confirm`（确认支付）
- 新增支付宝异步回调接口：
  - `POST /bookings/alipay/notify`
- 支付网关服务接入（Alipay SDK）：
  - 预下单 `alipay.trade.precreate`
  - 订单查询 `alipay.trade.query`
  - 回调签名校验 `rsaCheckV1`
- 支付相关 DTO 完成：
  - `CreateBookingPaymentRequest`
  - `ConfirmBookingPaymentRequest`
  - `CreateBookingPaymentResult`
  - `ConfirmBookingPaymentResult`
- 配置项补充：
  - `alipay.gateway-url`
  - `alipay.app-id`
  - `alipay.private-key`
  - `alipay.public-key`
  - `alipay.notify-url`


关于修改的文件备注：
- 新增
  - `.env.alipay.example`
  - `src/main/java/org/example/coursework3/dto/request/CreateBookingPaymentRequest.java`
  - `src/main/java/org/example/coursework3/dto/request/ConfirmBookingPaymentRequest.java`
  - `src/main/java/org/example/coursework3/dto/response/CreateBookingPaymentResult.java`
  - `src/main/java/org/example/coursework3/dto/response/ConfirmBookingPaymentResult.java`
  - `src/main/java/org/example/coursework3/service/AlipayGatewayService.java`

- 修改
  - `frontend/src/api/client.js`
  - `frontend/src/pages/customer/CustomerSpecialistSlotsPage.vue`
  - `src/main/java/org/example/coursework3/controller/BookingController.java`
  - `src/main/java/org/example/coursework3/service/CustomerBookingService.java`
  - `src/main/resources/application.yml`
  - `接口文档.md`
  - `pom.xml`
  



### 备注

- 当前支付流程已改为真实支付宝路径（DEMO）。
