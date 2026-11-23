# Single Chat

[![CI](https://github.com/yaunfei/single-chat/actions/workflows/ci.yml/badge.svg)](https://github.com/yaunfei/single-chat/actions/workflows/ci.yml)

一个只做 Android 的纯聊天应用。没有信息流，没有推荐，打开就是对话。

当前版本是 **M0 可运行骨架**：客户端完全可跑，消息通道先用本地演示引擎模拟；服务端已提供可独立运行、可测试的登录与 UserSig 协议实现，为接入腾讯云 IM 做好准备。

## 当前能力

- 登录页：手机号校验、验证码倒计时、本地演示登录
- 会话列表：置顶、未读、搜索、新建会话、删除会话
- 聊天页：文本消息、日期分割、发送状态、失败重试、输入中提示、长按复制
- 设置页：账号信息、浅色/深色/跟随系统、退出登录
- 首次启动自动播种 4 个演示会话，对方会按人设回复
- 服务端：短信验证码（开发态）、JWT 登录、腾讯云 UserSig、推送 token 注册
- 测试：客户端时间格式化单测；服务端 JWT / UserSig / 验证码 / HTTP 全链路测试

## 快速开始

### Android

要求：JDK 17、Android SDK 35、Android Studio Ladybug 或更新版本。

```bash
# 用 Android Studio 打开仓库根目录即可；命令行构建：
./gradlew :app:assembleDebug
```

安装后使用任意合法的中国大陆手机号登录，演示验证码为 **123456**。

> 彩蛋：在聊天框发送 `/fail` 可以体验消息发送失败与重试流程。

### 服务端

零第三方依赖，Node.js 22+ 即可运行。

```bash
cd server
cp .env.example .env   # 填入 JWT_SECRET / IM_SECRET_KEY（本地演示可先用默认值）
npm test               # 不需要 npm install
npm start              # http://localhost:8787
curl http://localhost:8787/healthz
```

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

- **`ChatEngine` 是唯一消息边界**：UI 只认识 `Conversation` / `Message`，不认识任何 IM SDK。M1 接入腾讯云 IM 时，只需新增一个实现。
- **本地优先**：发送消息先写 Room（`SENDING`），回调后更新状态；列表和聊天页都直接消费数据库 Flow。
- **服务端只做补集**：登录、UserSig、好友/群资料、推送 token；消息本身走云 IM。
- **密钥不落客户端**：`UserSig`、`JWT_SECRET`、`IM_SECRET_KEY` 只存在于服务端环境变量。

更多见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)。

## 技术栈

Kotlin 2.0 · Jetpack Compose · Material 3 · Hilt · Room · DataStore · Coroutines/Flow · Navigation Compose · Node.js（标准库）

## 文档

- [开发计划](docs/PLAN.md)
- [架构说明](docs/ARCHITECTURE.md)
- [服务端接口](docs/API.md)

## 安全与仓库约定

以下内容已被 `.gitignore` 排除，永不入库：`local.properties`、`.env`、签名文件、`build/`、`node_modules/`、本地 Android SDK。提交前请确认没有把 Secret 写进代码或文档。

## 路线图

M0 本地骨架 → M1 腾讯云 IM 接入与真实登录 → M2 富媒体与群聊 → M3 音视频通话 → M4 合规与上架。验收标准见 [docs/PLAN.md](docs/PLAN.md)。
