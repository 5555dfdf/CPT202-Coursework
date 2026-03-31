## 接口文档（前端视角整理）

**基础说明**

- **服务基础地址**: `VITE_API_BASE_URL`（由环境变量配置）
- **统一前缀**: 下文所有 URL 均为相对路径，需要拼接到基础地址后使用
- **认证方式**:
    - 登录后返回 `token`，前端存储在 `localStorage`：`booking.auth.token`
    - 后续请求通过请求头 `Authorization: Bearer <token>` 传递

---

## 一、认证 & 用户（Auth & User）

### 1. 发送注册邮箱验证码

- **URL**: `/auth/send-email-code`
- **方法**: `POST`
- **请求头**:
    - `Content-Type: application/json`
    - 无需 `Authorization`（未注册/未登录状态使用）
- **请求体 `payload`**:

| 字段名  | 类型   | 必填 | 说明                                |
| ------- | ------ | ---- | ----------------------------------- |
| email   | string | 是   | 注册使用的邮箱地址                 |
| scene   | string | 否   | 场景标记，前端传 `"register"`      |

- **示例请求体**:

```json
{
  "email": "user@example.com",
  "scene": "register"
}
```

- **响应（建议格式）**:

```json
{
  "code": "OK",
  "message": "验证码已发送"
}
```

---

### 2. 注册（邮箱+验证码）

- **URL**: `/auth/register`
- **方法**: `POST`
- **请求头**:
    - `Content-Type: application/json`
    - 无需 `Authorization`
- **请求体 `payload`**:

| 字段名             | 类型   | 必填 | 说明                                  |
| ------------------ | ------ | ---- | ------------------------------------- |
| name               | string | 是   | 用户昵称/姓名                        |
| email              | string | 是   | 注册邮箱                              |
| password           | string | 是   | 登录密码（明文或加密，视后端实现）   |
| verificationCode   | string | 是   | 邮箱收到的验证码（如 6 位数字）      |

- **示例请求体**:

```json
{
  "name": "张三",
  "email": "user@example.com",
  "password": "password123",
  "verificationCode": "123456"
}
```

- **成功响应（前端期望）**:

```json
{
  "token": "jwt-or-other-token",
  "user": {
    "id": "user-id",
    "name": "张三",
    "email": "user@example.com",
    "role": "Customer"
  }
}
```

---

### 3. 登录

- **URL**: `/auth/login`
- **方法**: `POST`
- **请求体 `payload`**:

| 字段名   | 类型   | 必填 | 说明     |
| -------- | ------ | ---- | -------- |
| email    | string | 是   | 登录邮箱 |
| password | string | 是   | 登录密码 |

- **响应**（与注册成功一致）:

```json
{
  "token": "jwt-or-other-token",
  "user": {
    "id": "user-id",
    "name": "张三",
    "email": "user@example.com",
    "role": "Customer"
  }
}
```

---

### 4. 登出

- **URL**: `/auth/logout`
- **方法**: `POST`
- **请求头**: 需要 `Authorization`
- **请求体**: 无

- **响应（示例）**:

```json
{
  "code": "OK",
  "message": "已登出"
}
```

---

### 5. 获取当前用户信息

- **URL**: `/me`
- **方法**: `GET`
- **请求头**: 需要 `Authorization`
- **请求参数**: 无

- **响应（推荐格式）**:

```json
{
  "user": {
    "id": "user-id",
    "name": "张三",
    "email": "user@example.com",
    "role": "Customer"
  }
}
```

---

### 6. 更新当前用户信息

- **URL**: `/me`
- **方法**: `PATCH`
- **请求头**: 需要 `Authorization`
- **请求体 `payload`**（部分更新即可）:

| 字段名  | 类型   | 必填 | 说明         |
| ------- | ------ | ---- | ------------ |
| name    | string | 否   | 修改后的姓名 |
| avatar  | string | 否   | 头像 URL     |
| 其他    | any    | 否   | 视业务扩展   |

- **响应（建议）**:

```json
{
  "user": {
    "id": "user-id",
    "name": "新名字",
    "email": "user@example.com",
    "role": "Customer",
    "avatar": "https://..."
  }
}
```

---

## 二、专长 & 专家（Expertise & Specialists）

### 1. 获取专长列表

- **URL**: `/expertise`
- **方法**: `GET`
- **请求参数**: 无

- **响应示例**:

```json
[
  {
    "id": "exp-1",
    "name": "职业发展",
    "description": "职业规划与咨询"
  },
  {
    "id": "exp-2",
    "name": "情绪压力",
    "description": "情绪管理与减压"
  }
]
```

---

### 2. 获取专家列表

- **URL**: `/specialists`
- **方法**: `GET`
- **查询参数 `params`**（可选）:

| 字段名      | 类型   | 说明                                   |
| ----------- | ------ | -------------------------------------- |
| expertiseId | string | 过滤：按专长 ID                        |
| page        | number | 页码，从 1 开始                        |
| pageSize    | number | 每页条数                               |
| 其他筛选项  | any    | 如地域、价格区间等，可按业务扩展      |

- **响应示例**:

```json
{
  "items": [
    {
      "id": "sp-1",
      "name": "李老师",
      "expertiseIds": ["exp-1", "exp-2"],
      "price": 300
    }
  ],
  "total": 1,
  "page": 1,
  "pageSize": 10
}
```

---

### 3. 获取专家详情

- **URL**: `/specialists/{id}`
- **方法**: `GET`
- **路径参数**:

| 字段名 | 类型   | 说明   |
| ------ | ------ | ------ |
| id     | string | 专家ID |

- **响应示例**:

```json
{
  "id": "sp-1",
  "name": "李老师",
  "bio": "多年咨询经验...",
  "expertise": [
    { "id": "exp-1", "name": "职业发展" }
  ],
  "price": 300
}
```

---

### 4. 获取专家可预约时段

- **URL**: `/specialists/{id}/slots`
- **方法**: `GET`
- **路径参数**:

| 字段名 | 类型   | 说明   |
| ------ | ------ | ------ |
| id     | string | 专家ID |

- **查询参数 `params`**（建议）:

| 字段名 | 类型   | 说明                            |
| ------ | ------ | -------------------------------- |
| date   | string | 查询日期，如 `"2026-03-17"`     |
| from   | string | 起始时间，可选                  |
| to     | string | 结束时间，可选                  |

- **响应示例**:

```json
[
  {
    "slotId": "slot-1",
    "start": "2026-03-17T09:00:00+08:00",
    "end": "2026-03-17T09:30:00+08:00",
    "available": true
  }
]
```

---

## 三、预约相关（Bookings - 客户）

### 1. 创建预约

- **URL**: `/bookings`
- **方法**: `POST`
- **请求头**: 需要 `Authorization`
- **请求体 `payload`**:

| 字段名      | 类型   | 必填 | 说明           |
| ----------- | ------ | ---- | -------------- |
| specialistId| string | 是   | 专家 ID        |
| slotId      | string | 是   | 选中的时段 ID  |
| note        | string | 否   | 备注/说明      |

- **响应示例**:

```json
{
  "id": "bk-1",
  "specialistId": "sp-1",
  "slotId": "slot-1",
  "status": "Pending"
}
```

---

### 2. 获取我的预约列表

- **URL**: `/bookings`
- **方法**: `GET`
- **请求头**: 需要 `Authorization`
- **查询参数 `params`**（可选）:

| 字段名   | 类型   | 说明                                  |
| -------- | ------ | ------------------------------------- |
| status   | string | 筛选状态，如 `Pending/Confirmed/...` |
| page     | number | 页码                                  |
| pageSize | number | 每页条数                              |
| from     | string | 开始日期                              |
| to       | string | 结束日期                              |

- **响应示例**:

```json
{
  "items": [
    {
      "id": "bk-1",
      "specialistId": "sp-1",
      "time": "2026-03-17T09:00:00+08:00",
      "status": "Pending"
    }
  ],
  "total": 1,
  "page": 1,
  "pageSize": 10
}
```

---

### 3. 获取单个预约详情

- **URL**: `/bookings/{id}`
- **方法**: `GET`
- **请求头**: 需要 `Authorization`
- **路径参数**:

| 字段名 | 类型   | 说明   |
| ------ | ------ | ------ |
| id     | string | 预约ID |

- **响应示例**:

```json
{
  "id": "bk-1",
  "customerId": "user-1",
  "specialistId": "sp-1",
  "time": "2026-03-17T09:00:00+08:00",
  "status": "Confirmed",
  "note": "希望聊职业规划"
}
```

---

### 4. 取消预约

- **URL**: `/bookings/{id}/cancel`
- **方法**: `POST`
- **请求头**: 需要 `Authorization`
- **路径参数**:

| 字段名 | 类型   | 说明   |
| ------ | ------ | ------ |
| id     | string | 预约ID |

- **请求体 `payload`**（可选）:

| 字段名 | 类型   | 必填 | 说明     |
| ------ | ------ | ---- | -------- |
| reason | string | 否   | 取消原因 |

- **响应示例**:

```json
{
  "id": "bk-1",
  "status": "Cancelled"
}
```

---

### 5. 改期预约

- **URL**: `/bookings/{id}/reschedule`
- **方法**: `POST`
- **请求头**: 需要 `Authorization`
- **路径参数**:

| 字段名 | 类型   | 说明   |
| ------ | ------ | ------ |
| id     | string | 预约ID |

- **请求体 `payload`**（示例字段）:

| 字段名 | 类型   | 必填 | 说明        |
| ------ | ------ | ---- | ----------- |
| slotId | string | 是   | 新的时段 ID |

- **响应示例**:

```json
{
  "id": "bk-1",
  "status": "Rescheduled",
  "slotId": "slot-2"
}
```

---

## 四、专家端工作流（Specialist workflow）

### 1. 获取预约请求列表（专家端）

- **URL**: `/specialist/booking-requests`
- **方法**: `GET`
- **请求头**: 需要 `Authorization`（专家角色）
- **查询参数 `params`**（可选）:

| 字段名   | 类型   | 说明            |
| -------- | ------ | --------------- |
| status   | string | 如 `Pending` 等|
| page     | number | 页码            |
| pageSize | number | 每页条数        |

- **响应示例**:

```json
{
  "items": [
    {
      "id": "bk-1",
      "customerName": "张三",
      "time": "2026-03-17T09:00:00+08:00",
      "status": "Pending"
    }
  ],
  "total": 1,
  "page": 1,
  "pageSize": 10
}
```

---

### 2. 确认预约

- **URL**: `/specialist/bookings/{id}/confirm`
- **方法**: `POST`
- **请求头**: 需要 `Authorization`
- **路径参数**:

| 字段名 | 类型   | 说明   |
| ------ | ------ | ------ |
| id     | string | 预约ID |

- **请求体**: 无

- **响应示例**:

```json
{
  "id": "bk-1",
  "status": "Confirmed"
}
```

---

### 3. 拒绝预约

- **URL**: `/specialist/bookings/{id}/reject`
- **方法**: `POST`
- **请求头**: 需要 `Authorization`
- **路径参数**:

| 字段名 | 类型   | 说明   |
| ------ | ------ | ------ |
| id     | string | 预约ID |

- **请求体 `payload`**（建议）:

| 字段名 | 类型   | 必填 | 说明     |
| ------ | ------ | ---- | -------- |
| reason | string | 否   | 拒绝原因 |

- **响应示例**:

```json
{
  "id": "bk-1",
  "status": "Rejected"
}
```

---

### 4. 完成预约

- **URL**: `/specialist/bookings/{id}/complete`
- **方法**: `POST`
- **请求头**: 需要 `Authorization`
- **路径参数**:

| 字段名 | 类型   | 说明   |
| ------ | ------ | ------ |
| id     | string | 预约ID |

- **请求体**: 可为空或包含会话总结等字段（视业务需要）

- **响应示例**:

```json
{
  "id": "bk-1",
  "status": "Completed"
}
```

---

## 五、后台管理（Admin）

> 以下接口通常要求管理员角色，并通过 `Authorization` 校验。

### 1. 创建专家

- **URL**: `/admin/specialists`
- **方法**: `POST`
- **请求头**: 需要 `Authorization`
- **请求体 `payload`**（示例）:

| 字段名       | 类型     | 必填 | 说明           |
| ------------ | -------- | ---- | -------------- |
| name         | string   | 是   | 专家姓名       |
| expertiseIds | string[] | 是   | 专长 ID 列表  |
| price        | number   | 否   | 默认价格       |
| bio          | string   | 否   | 简介           |

- **响应**: 返回创建后的专家对象。

---

### 2. 更新专家信息

- **URL**: `/admin/specialists/{id}`
- **方法**: `PATCH`
- **请求头**: 需要 `Authorization`
- **路径参数**:

| 字段名 | 类型   | 说明   |
| ------ | ------ | ------ |
| id     | string | 专家ID |

- **请求体 `payload`**: 与创建类似，字段全部可选，为局部更新。

- **响应**: 返回更新后的专家对象。

---

### 3. 设置专家状态

- **URL**: `/admin/specialists/{id}/status`
- **方法**: `POST`
- **请求头**: 需要 `Authorization`
- **路径参数**:

| 字段名 | 类型   | 说明   |
| ------ | ------ | ------ |
| id     | string | 专家ID |

- **请求体 `payload`**（示例）:

| 字段名 | 类型   | 必填 | 说明                           |
| ------ | ------ | ---- | ------------------------------ |
| status | string | 是   | 如 `Active` / `Inactive` 等   |

- **响应**: 返回状态更新后的专家对象或简单状态结果。

---

### 4. 创建专长

- **URL**: `/admin/expertise`
- **方法**: `POST`
- **请求头**: 需要 `Authorization`
- **请求体 `payload`**:

| 字段名      | 类型   | 必填 | 说明     |
| ----------- | ------ | ---- | -------- |
| name        | string | 是   | 专长名称 |
| description | string | 否   | 描述     |

- **响应**: 返回创建后的专长对象。

---

### 5. 更新专长

- **URL**: `/admin/expertise/{id}`
- **方法**: `PATCH`
- **请求头**: 需要 `Authorization`
- **路径参数**:

| 字段名 | 类型   | 说明   |
| ------ | ------ | ------ |
| id     | string | 专长ID |

- **请求体 `payload`**: 与创建类似，字段可选。

- **响应**: 返回更新后的专长对象。

---

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

