# 服务端接口

Base URL：`http://localhost:8787`（开发默认值）

## 约定

- 请求与响应均为 `application/json; charset=utf-8`。
- 受保护接口使用 `Authorization: Bearer <token>`。
- 每个响应都带 `X-Request-Id`；错误响应体：

```json
{
  "error": {
    "code": "INVALID_CODE",
    "message": "验证码不正确",
    "requestId": "8f3c2a1b"
  }
}
```

- 时间字段均为 Unix 毫秒或秒，接口中会显式标注。
- 生产环境必须配置 `JWT_SECRET` 与 `IM_SECRET_KEY`；开发环境允许使用 `.env.example` 中的占位值。

## 环境变量

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `PORT` | `8787` | 监听端口 |
| `JWT_SECRET` | `dev-only-change-me` | JWT HS256 密钥 |
| `JWT_TTL_SECONDS` | `604800` | 登录态有效期（7 天） |
| `IM_SDK_APP_ID` | `1400000000` | 腾讯云 IM SDKAppID |
| `IM_SECRET_KEY` | `dev-only-change-me` | 腾讯云 IM 密钥，仅服务端持有 |
| `IM_USERSIG_EXPIRE_SECONDS` | `604800` | UserSig 有效期（7 天） |
| `DEV_SHOW_SMS_CODE` | `true` | 开发态在响应中回显验证码 |
| `CORS_ORIGIN` | `*` | 允许的来源 |

## GET /healthz

健康检查。

```json
{ "status": "ok", "uptimeSeconds": 12 }
```

## POST /api/v1/auth/sms

发送短信验证码。开发态直接返回验证码，生产环境只返回 `sent: true`。

请求：

```json
{ "phone": "13800138000" }
```

响应：

```json
{
  "sent": true,
  "cooldownSeconds": 60,
  "devCode": "123456"
}
```

错误码：`INVALID_PHONE` (400)、`SMS_COOLDOWN` (429)。

## POST /api/v1/auth/login

验证码登录，首次登录自动创建用户。

请求：

```json
{ "phone": "13800138000", "code": "123456" }
```

响应：

```json
{
  "token": "eyJhbGciOi...",
  "expiresIn": 604800,
  "user": {
    "id": "u_3f9c1a2b7d",
    "phone": "13800138000",
    "displayName": "用户8000"
  }
}
```

错误码：`INVALID_PHONE` (400)、`INVALID_CODE` (401)、`CODE_EXPIRED` (401)、`TOO_MANY_ATTEMPTS` (429)。

## GET /api/v1/me

需要登录。返回当前用户。

```json
{
  "user": {
    "id": "u_3f9c1a2b7d",
    "phone": "13800138000",
    "displayName": "用户8000"
  }
}
```

## GET /api/v1/im/usersig

需要登录。返回当前用户的腾讯云 IM 签名。**客户端不得自行拼接或存储密钥。**

```json
{
  "userId": "u_3f9c1a2b7d",
  "sdkAppId": 1400000000,
  "userSig": "eJyrVkrLz1eyUlBK...",
  "expireAt": 1799999999
}
```

## POST /api/v1/push/token

需要登录。注册或更新设备推送 token（M1 使用）。

请求：

```json
{
  "provider": "huawei",
  "token": "push-token-from-client",
  "deviceId": "android-id"
}
```

响应：`204 No Content`。

错误码：`INVALID_PUSH_TOKEN` (400)。

## 通用错误码

| HTTP | code | 含义 |
| --- | --- | --- |
| 400 | `BAD_JSON` | 请求体不是合法 JSON 或过大 |
| 401 | `UNAUTHORIZED` | 缺少或无效 Bearer token |
| 404 | `NOT_FOUND` | 路径不存在 |
| 405 | `METHOD_NOT_ALLOWED` | 方法不支持 |
| 500 | `INTERNAL_ERROR` | 未预期异常 |
