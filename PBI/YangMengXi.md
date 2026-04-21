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

### PBI：支付优先的二维码流程

- **User Story**  
  As a customer, I want to complete payment before booking creation, so that unpaid requests are blocked and the flow is consistent.

- **Acceptance Criteria**
  - GIVEN I am a customer and have selected a valid slot, WHEN I click submit, THEN the system should create payment intent first and open a payment QR modal.
  - GIVEN payment order is created successfully, WHEN QR modal is displayed, THEN the modal should show amount and a scannable QR code.
  - GIVEN I have finished payment, WHEN I click `Paid`, THEN frontend should call payment confirm API and then call `POST /bookings` to create booking.
  - GIVEN confirm + create booking succeeds, WHEN success modal closes, THEN page should navigate to `My Bookings`.
  - GIVEN Alipay config is missing/invalid, WHEN customer triggers payment creation, THEN frontend should show explicit error and not fake success.

### PBI：支付接口拆分（3个）

#### PBI 1 - 创建支付单接口

- **接口**: `POST /bookings/{id}/payment`
- **User Story**:  
  As a customer, I want the system to create an Alipay order before booking creation, so that I can pay by scanning a QR code first.
- **Acceptance Criteria**:
  - GIVEN slot is valid and available, WHEN calling create payment API, THEN system returns paymentId/paymentToken/qrCodeUrl/amount/currency.
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
- 支付弹窗交互完成：展示 `paymentIntentId`、slot、金额、二维码；支持 `Pay Later` 与 `Paid` 两个操作按钮。
- 支付确认流程接入：`Paid` 点击后先调用支付确认接口，再调用 `POST /bookings` 创建预约，成功提示后跳转 `My Bookings`。
- 新增模拟支付按钮：主二维码弹窗与悬浮球继续支付弹窗均支持 `Mock Payment`，用于测试环境快速完成支付闭环。

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
### 备注

- 当前支付流程已改为真实支付宝路径（DEMO）。

---
新增接口

### PBI 4 - 创建预约接口（支付后）

- **接口**: `POST /bookings`
- **User Story**  
  As a customer, I want booking creation to require payment proof, so that unpaid requests are blocked.
- **Acceptance Criteria**
  - GIVEN `paymentId` missing/invalid/expired, WHEN calling create booking, THEN API rejects.
  - GIVEN payment is successful and matches specialist/slot, WHEN calling create booking, THEN booking is created and slot is locked.
  - GIVEN booking creation succeeds, WHEN cleanup runs, THEN related Redis unpaid draft/index are removed.

### PBI 5 - 未支付列表接口（悬浮球）

- **接口**: `GET /bookings/unpaid-payments`
- **User Story**  
  As a customer, I want to query my unpaid orders list, so that I can continue payment later.
- **Acceptance Criteria**
  - GIVEN customer has unpaid intents in Redis, WHEN calling API, THEN returns list with amount/slot/remainingSeconds.
  - GIVEN some intents already expired, WHEN calling API, THEN expired records are cleaned and not returned.

### PBI 6 - 单个未支付详情接口

- **接口**: `GET /bookings/unpaid-payments/{id}`
- **User Story**  
  As a customer, I want to fetch one unpaid intent detail, so that frontend can render accurate continue-pay info.
- **Acceptance Criteria**
  - GIVEN intent belongs to current customer and not expired, WHEN calling API, THEN returns one unpaid detail record.
  - GIVEN intent not found/expired/not owned, WHEN calling API, THEN returns clear permission/expiry/not found error.

### PBI 7 - 继续支付接口（刷新二维码）

- **接口**: `POST /bookings/unpaid-payments/{id}/resume`
- **User Story**  
  As a customer, I want to resume an unpaid payment and get a refreshed QR code, so that I can complete payment after Pay Later.
- **Acceptance Criteria**
  - GIVEN unpaid intent is valid and slot still available, WHEN calling resume API, THEN returns new `paymentId` and refreshed `qrCodeUrl`.
  - GIVEN old outTradeNo exists, WHEN resume succeeds, THEN old mapping is replaced by new mapping in Redis.
  - GIVEN slot already unavailable, WHEN calling resume API, THEN API fails with explicit message and stale unpaid record is cleaned.

### PBI 8 - 模拟支付成功接口（测试）

- **接口**: `POST /bookings/{id}/payment/mock-success`
- **User Story**  
  As a tester, I want to mark an unpaid intent as paid in test environment, so that I can verify booking creation flow when real device payment is unavailable.
- **Acceptance Criteria**
  - GIVEN mock switch is enabled and intent belongs to current customer, WHEN calling this API, THEN backend marks draft payment as paid and returns success.
  - GIVEN mock switch is disabled, WHEN calling this API, THEN API returns explicit error.
  - GIVEN intent does not exist or does not belong to current customer, WHEN calling this API, THEN API returns permission/not-found error.
