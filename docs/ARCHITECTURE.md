# 架构说明

## 1. 总览

```text
┌──────────────────────── Android ────────────────────────┐
│  feature: auth / conversations / chat / settings        │
│        │ StateFlow + 事件                               │
│  core: model / designsystem / util                      │
│        │ ChatEngine（唯一消息边界）                      │
│        ├── LocalChatEngine（M0，保留作离线演示）           │
│        └── TencentChatEngine（M1，当前绑定）── 腾讯云 IM    │
└──────────────────────────┬──────────────────────────────┘
                           │ HTTPS
┌──────────────────────────▼──────────────────────────────┐
│  BFF（server/）                                          │
│  POST 短信验证码 · POST 登录（JWT） · GET UserSig          │
│  POST 推送 token · 好友/群资料（M2） · 审核回调（M4）       │
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
  id (本地 UUID 或 im_<msgID>), conversationId, senderId, senderName,
  text, type(TEXT|IMAGE|SYSTEM), status(SENDING|SENT|FAILED), createdAt,
  isMine（由 senderId 与当前 IM userId 计算）

Session
  userId, displayName, phone, token（BFF 登录凭证）
```

数据库表：

| 表 | 关键字段 | 说明 |
| --- | --- | --- |
| `conversations` | `id` 主键，`updatedAt` 索引 | 列表排序：置顶优先，其次最近更新 |
| `messages` | `id` 主键，`remoteId`（IM msgID），`(conversationId, createdAt)` 索引 | 按时间升序读取；`remoteId` 用于漫游/回执去重 |

Room 当前是 v2，`MIGRATION_1_2` 显式给 `messages` 增加 `remoteId`；不再使用破坏性迁移。

## 4. 关键链路

### 发送文本

1. ViewModel 校验非空，调用 `ChatEngine.sendText`。
2. 引擎生成 UUID 作为本地主键，写入 `messages(status = SENDING)`。
3. 同一事务更新会话预览与 `updatedAt`。
4. 调用 IM `sendMessage`；成功回调拿到 msgID 后写入 `remoteId` 并置为 `SENT`，失败置为 `FAILED`。
5. 若高级消息监听先把同一条消息写进库，成功回调会按 `remoteId` 删除重复行。

### 接收与未读

- 会话页点击进入聊天时 `markRead` 清零未读，并注册“当前活跃会话”。
- `onRecvNewMessage` 先落库再更新会话预览；当前会话处于前台时不递增未读。
- 进入聊天页额外拉取最近 50 条云端漫游，按 `remoteId` 去重后补入本地。

### 连接状态与重登

`ChatConnectionState`（OFFLINE / CONNECTING / ONLINE / FAILED）由 IM SDK 回调驱动，会话列表顶栏直接展示。UserSig 过期时清空缓存并自动重新向 BFF 取签名、重登。

### 输入中状态

`ChatEngine.observeTyping(conversationId)` 仍是接口的一部分；M1 的腾讯云实现暂不启用在线输入状态，返回 `false`。

## 5. 腾讯云 IM 接入（M1 已实现）

`TencentChatEngine` 的关键流程：

1. `ensureSeeded()` 读取 DataStore 中的 Session，向 BFF 换取 UserSig，首次调用 `initSDK` 后 `login`。
2. 账号切换（`im_user_id` 变化）时清空 Room，避免看到上一个账号的本地消息。
3. 会话列表用 `getConversationList` 全量同步；本地乐观发送的较新预览会保留。
4. 接收监听 `onRecvNewMessage`，会话不存在时自动创建，再写入 Room。
5. 已读同时调用 IM 的 `markC2CMessageAsRead` / `markGroupMessageAsRead` 并清本地未读。
6. 删除会话先调 IM，再删本地；退出登录同时 `logout` IM。
7. `SecretKey` 从未进入客户端：客户端只持有 BFF 的 JWT 与短期 UserSig。

> M1 取舍：登录 Token 明文保存在 DataStore；后续迁移到 EncryptedDataStore / Android Keystore。

切换点只有 Hilt 绑定：

```kotlin
@Binds
abstract fun bindChatEngine(impl: TencentChatEngine): ChatEngine
```

UI、ViewModel、数据库不需要改动。

## 6. 服务端设计

- 零依赖 Node.js，`createApp()` 返回纯 `http.RequestListener`，便于测试时监听随机端口。
- `jwt.mjs`：HS256 签名与校验，校验 `exp`、`nbf`、`iss`，使用 `timingSafeEqual` 防时序攻击。
- `usersig.mjs`：按腾讯云官方 TLSSigAPIv2 算法生成（HMAC-SHA256 签名 → JSON deflate → Base64URL 变体），并用官方 `tls-sig-api-v2` 参考实现（`vendor/TLSSigAPIv2.cjs`）在单测中交叉验证；签名密钥只来自环境变量。
- `store.mjs`：短信验证码状态机，5 分钟过期、60 秒重发冷却、5 次尝试上限、一次性使用。
- 错误统一为 `{ error: { code, message, requestId } }`，不泄露内部堆栈。
- JSON body 限制 64KB；所有响应带 CORS 与 `requestId`。

## 7. 决策记录

| 决策 | 原因 |
| --- | --- |
| 单模块起步 | 个人 + AI 开发，多模块的构建成本高于收益 |
| 本地引擎 + 接口隔离 | 无云账号也能完整体验；迁移只换实现 |
| 不做端到端加密（M1） | 先保证合规与可用；E2EE 需要独立的消息协议设计 |
| 登录 Token 明文存 DataStore | M1 快速跑通链路；后续迁移 EncryptedDataStore |
| 服务端零依赖 | 沙箱与 CI 可直接运行测试，不引入供应链风险 |
| 品牌色不跟随动态取色 | 保持产品识别度，主题切换只影响明暗 |
