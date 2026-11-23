# 架构说明

## 1. 总览

```text
┌──────────────────────── Android ────────────────────────┐
│  feature: auth / conversations / chat / settings        │
│        │ StateFlow + 事件                               │
│  core: model / designsystem / util                      │
│        │ ChatEngine（唯一消息边界）                      │
│        ├── LocalChatEngine  ── Room ── 本地 SQLite       │
│        └── TencentChatEngine（M1）── 腾讯云 IM SDK       │
└──────────────────────────┬──────────────────────────────┘
                           │ HTTPS
┌──────────────────────────▼──────────────────────────────┐
│  BFF（server/）                                          │
│  POST 短信验证码 · POST 登录（JWT） · GET UserSig          │
│  POST 推送 token · 好友/群资料（M1） · 审核回调（M4）       │
└──────────────────────────┬──────────────────────────────┘
                           │
                腾讯云 IM / TRTC / COS / 短信（M1+）
```

## 2. Android 分层

单模块、按 feature 分包，避免早期多模块带来的构建复杂度；当功能稳定后再拆 `core:*` 与 `feature:*`。

| 包 | 职责 | 依赖限制 |
| --- | --- | --- |
| `feature/*` | 页面与 ViewModel | 只能依赖 `core` 的模型与接口 |
| `core/chat` | `ChatEngine` 接口与本地实现 | 不依赖 Compose |
| `core/database` | Room 实体、DAO、映射 | 不依赖 UI |
| `core/model` | 纯数据模型 | 无 Android 依赖 |
| `core/designsystem` | 主题、颜色、通用组件 | 只依赖 Compose |
| `core/util` | 时间格式化等纯函数 | 无 Android 依赖，可单测 |

单向数据流：`UI 事件 → ViewModel → ChatEngine/Repository → Room Flow → ViewModel StateFlow → UI`。

## 3. 数据模型

```text
Conversation
  id, title, peerId?, isGroup, isPinned, unreadCount,
  lastMessage, lastMessageAt, updatedAt, avatarSeed

Message
  id (clientMsgId), conversationId, senderId, senderName,
  text, type(TEXT|IMAGE|SYSTEM), status(SENDING|SENT|FAILED), createdAt
```

数据库表：

| 表 | 关键字段 | 说明 |
| --- | --- | --- |
| `conversations` | `id` 主键，`updatedAt` 索引 | 列表排序：置顶优先，其次最近更新 |
| `messages` | `id` 主键，`(conversationId, createdAt)` 索引 | 按时间升序读取，日期分割在 UI 层完成 |

M0 使用 `fallbackToDestructiveMigration()`，正式版本前必须改为显式迁移。

## 4. 关键链路

### 发送文本

1. ViewModel 校验非空，调用 `ChatEngine.sendText`。
2. 引擎生成 `clientMsgId`，写入 `messages(status = SENDING)`。
3. 同一事务更新会话预览与 `updatedAt`。
4. 模拟通道延迟后把状态改为 `SENT`；若文本为 `/fail`，改为 `FAILED`。
5. 触发对方的模拟回复，写入消息并递增未读数（当前会话处于前台时不递增）。

### 接收与未读

- 会话页点击进入聊天时 `markRead` 清零未读，并注册“当前活跃会话”。
- 模拟回复到达时根据活跃会话决定是否递增未读，避免“正在聊天还显示未读”。

### 输入中状态

`ChatEngine.observeTyping(conversationId)` 暴露布尔流；本地引擎在生成回复前设为 `true`，写入后复位。

## 5. 替换为真实 IM

接入腾讯云 IM 时新增 `TencentChatEngine : ChatEngine`：

1. 初始化 SDK：App 启动后调用 `initSDK`，用户登录后 `login(userSig)`。
2. 发送：调用 `sendMessage`，用 `clientMsgId` 做幂等；回调更新本地 Room 状态。
3. 接收：监听新消息，先落库再更新会话与未读。
4. 会话：用 IM 的会话列表做增量同步，业务字段（置顶、免打扰）覆盖在本地表。
5. 资料：群资料与好友关系仍以自有后端为准，IM 只做消息通道。
6. UserSig：客户端向 BFF 请求短期签名，过期前刷新。

切换点只有 Hilt 绑定：

```kotlin
@Binds
abstract fun bindChatEngine(impl: TencentChatEngine): ChatEngine
```

UI、ViewModel、数据库不需要改动。

## 6. 服务端设计

- 零依赖 Node.js，`createApp()` 返回纯 `http.RequestListener`，便于测试时监听随机端口。
- `jwt.mjs`：HS256 签名与校验，校验 `exp`、`nbf`、`iss`，使用 `timingSafeEqual` 防时序攻击。
- `usersig.mjs`：按腾讯云 UserSig 算法生成（HMAC-SHA256 → zlib → Base64），签名密钥只来自环境变量。
- `store.mjs`：短信验证码状态机，5 分钟过期、60 秒重发冷却、5 次尝试上限、一次性使用。
- 错误统一为 `{ error: { code, message, requestId } }`，不泄露内部堆栈。
- JSON body 限制 64KB；所有响应带 CORS 与 `requestId`。

## 7. 决策记录

| 决策 | 原因 |
| --- | --- |
| 单模块起步 | 个人 + AI 开发，多模块的构建成本高于收益 |
| 本地引擎 + 接口隔离 | 无云账号也能完整体验；迁移只换实现 |
| 不做端到端加密（M0） | 先保证合规与可用；E2EE 需要独立的消息协议设计 |
| 服务端零依赖 | 沙箱与 CI 可直接运行测试，不引入供应链风险 |
| 品牌色不跟随动态取色 | 保持产品识别度，主题切换只影响明暗 |
