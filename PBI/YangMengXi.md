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