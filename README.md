# Single Chat

[![CI](https://github.com/yaunfei/single-chat/actions/workflows/ci.yml/badge.svg)](https://github.com/yaunfei/single-chat/actions/workflows/ci.yml)

一个只做 Android 的纯聊天应用。没有信息流，没有推荐，打开就是对话。

当前版本是 **M1 云端连通**：登录、UserSig 与消息收发都已接入腾讯云 IM；SecretKey 只保存在本地 BFF，客户端拿到的只是短期 UserSig。

## 当前能力

- 登录页：手机号校验、真实短信验证码（开发态由 BFF 回显并自动填入）、错误提示
- 会话列表：置顶、未读、搜索、按对方用户 ID 新建会话、删除会话、云端连接状态
- 聊天页：文本消息、日期分割、发送状态、失败重试、长按复制、最近 50 条云端漫游
- 设置页：账号信息（含 IM userId）、浅色/深色/跟随系统、退出登录
- 消息通道：腾讯云 IM（`imsdk-plus`），Room 仍是 UI 的唯一数据源
- 服务端：短信验证码（开发态）、JWT 登录、腾讯云 UserSig、推送 token 注册
- 测试：客户端时间格式化单测；服务端 JWT / UserSig / 验证码 / HTTP 全链路测试

## 快速开始

### 服务端

零第三方依赖，Node.js 22+ 即可运行。

```bash
cd server
cp .env.example .env   # 填入 IM_SDK_APP_ID / IM_SECRET_KEY
npm test               # 不需要 npm install
npm start              # http://localhost:8787
curl http://localhost:8787/healthz
```

本地开发建议保持 `DEV_SHOW_SMS_CODE=true`，登录页会自动回显并填入验证码。

### Android

要求：JDK 17、Android SDK 35、Android Studio Ladybug 或更新版本。

```bash
# 先启动 server，再构建客户端：
./gradlew :app:assembleDebug
```

模拟器通过 `http://10.0.2.2:8787` 访问宿主机 BFF。真机调试时用
`-Psinglechat.backendBaseUrl=http://<电脑局域网 IP>:8787` 覆盖默认地址。

登录后即可用自己的账号与另一个账号互发消息；在「新对话」里输入对方的 IM userId 即可开会话。

> 当前 Token 明文保存在 DataStore，属于 M1 的取舍；M2 会迁移到 EncryptedDataStore / Keystore。

接口说明见 [docs/API.md](docs/API.md)。

## 工程结构

```text
.
├── app/                     # Android 单模块，按 feature 分包
│   └── src/main/java/com/yaunfei/singlechat/
│       ├── core/            # 模型、数据库、聊天引擎、设计系统、工具
│       ├── feature/         # auth / conversations / chat / settings
│       └── navigation/      # Compose 导航
├── server/                  # 零依赖 Node BFF
│   ├── src/                 # jwt / usersig / sms store / http app
│   └── test/                # node:test 测试
├── docs/                    # 计划、架构、接口
└── .github/workflows/ci.yml # Android + 服务端持续集成
```

## 关键设计

- **`ChatEngine` 是唯一消息边界**：UI 只认识 `Conversation` / `Message`，不认识任何 IM SDK；M1 只替换了实现（`TencentChatEngine`），页面与 ViewModel 未改动。
- **本地优先**：发送消息先写 Room（`SENDING`），IM 回调返回 msgID 后更新为 `SENT`；列表和聊天页都直接消费数据库 Flow。
- **服务端只做补集**：登录、UserSig、好友/群资料、推送 token；消息本身走云 IM。
- **密钥不落客户端**：`JWT_SECRET`、`IM_SECRET_KEY` 只存在于服务端环境变量，客户端通过 Bearer Token 换取 UserSig。

更多见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)。

## 技术栈

Kotlin 2.0 · Jetpack Compose · Material 3 · Hilt · Room · DataStore · Coroutines/Flow · Navigation Compose · 腾讯云 IM（imsdk-plus）· Node.js（标准库）

## 文档

- [开发计划](docs/PLAN.md)
- [架构说明](docs/ARCHITECTURE.md)
- [服务端接口](docs/API.md)

## 安全与仓库约定

以下内容已被 `.gitignore` 排除，永不入库：`local.properties`、`.env`、签名文件、`build/`、`node_modules/`、本地 Android SDK。提交前请确认没有把 Secret 写进代码或文档。

## 路线图

M0 本地骨架（已完成）→ **M1 腾讯云 IM 接入与真实登录（当前）** → M2 富媒体与群聊 → M3 音视频通话 → M4 合规与上架。验收标准见 [docs/PLAN.md](docs/PLAN.md)。
