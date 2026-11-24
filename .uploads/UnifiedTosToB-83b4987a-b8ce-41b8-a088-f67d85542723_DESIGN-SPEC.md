# DESIGN-SPEC · 聊天 App（iOS 26 Liquid Glass 风格）

设计基准画布：**390 × 830 pt**（iPhone 逻辑尺寸）
Android 换算：**1 pt = 1 dp**，字号用 sp。目标宽度 393dp（Pixel 8）几乎 1:1；360dp 窄屏需按「响应式注意事项」处理。

---

## 1. 设计 Token

### 1.1 颜色

| Token | 值 | 用途 |
|---|---|---|
| `bg` | `#FFFFFF` | 页面主背景（01/02/03 页） |
| `bg-grouped` | `#F2F2F7` | 分组背景（05 我的） |
| `fill-tertiary` | `#E9E9EB` | 搜索框底色、未选中的胶囊/chip、圆形图标钮底 |
| `text-primary` | `#000000` | 主文字 |
| `text-secondary` | `#8E8E93` | 次要文字、时间戳、预览文字 |
| `text-tertiary` | `#C7C7CC` | 占位符文字 |
| `separator` | `#E5E5EA` | 0.5dp 分隔线 |
| `accent` | `#007AFF` | 强调色、发送气泡、未读角标、图标 |
| `green` | `#34C759` | 在线状态点 |

气泡与卡片另用：
- 接收气泡 `#E9E9EB`
- 发送气泡 `#007AFF`，文字 `#FFFFFF`
- 卡片白底 `#FFFFFF`（05 页卡片叠在 `bg-grouped` 上）

### 1.2 字体

`Inter`（Android 可用 `Inter` 或回退 `Roboto`；数字和拉丁字符建议直接引 Inter 字体资源）

| 场景 | 字号 | 字重 | 行高 |
|---|---|---|---|
| 页面大标题（消息 / 通讯录 / 我的） | 34sp | Bold (700) | 默认 |
| 区块标题 / 卡片主文字 | 20sp | SemiBold (600) | 默认 |
| 列表主标题（会话名、联系人名） | 17sp | SemiBold (600) | 默认 |
| 列表主文字（消息预览、菜单项） | 16sp | Normal (400) | 默认 |
| 正文 / 气泡文字 | 15sp | Normal (400) | 1.35 |
| 时间戳、辅助说明 | 13sp | Normal / SemiBold | 默认 |
| 角标数字 | 12–13sp | SemiBold (600) | 默认 |
| Tab Bar 标签 | 10–12sp | Medium (500) | 默认 |

### 1.3 圆角

| 值 | 用途 |
|---|---|
| `29` | Tab Bar 玻璃胶囊 |
| `23` | Tab 选中态玻璃凸起 |
| `22` | 导航栏胶囊（联系人 / 通话视频组）、底部搜索条 |
| `20` | 圆形图标按钮（40×40） |
| `17` | 圆形小按钮（34×34）、输入框 |
| `16` | 筛选 chip（31dp 高 → 半圆） |
| `14` | 设置分组卡片、头像方块 |
| `11` | 搜索框（38dp 高） |
| `8` | 小图标容器 |

### 1.4 间距

- 页面左右边距：`16dp`
- 状态栏高度：`62dp`（iOS 值，Android 见 §4）
- 页面标题区：`41dp` 高，标题与搜索框间距 `20dp`
- 列表行高：会话行 `74dp`，联系人行 `56dp`
- 分隔线：`0.5dp`，仅 `bottom`
- Tab Bar 外容器：`padding [8, 16, 12, 16]`，玻璃胶囊 `58dp` 高
- 底部输入栏：`padding [8, 12, 12, 12]`，控件 `34dp` 高

---

## 2. 页面结构

> 坐标格式：`x, y`，相对父容器左上角。`fill` = 宽度撑满父容器。

### 2.1 01 · 消息（会话列表）

```
Screen 390×830, bg=#FFFFFF
├─ Status Bar            0,0    390×62   (系统，见 §4)
├─ Content               0,62   390×768  纵向 gap=20, padding=[2,16,0,16]
│  ├─ Header             16,2   358×41   横向 space-between
│  │  ├─ Title "消息"    34sp Bold
│  │  └─ Filter Button   40×40 r=20 【玻璃】icon=list-filter, tint=accent
│  ├─ Search             16,63  358×38   r=11 fill=#E9E9EB  ← 非玻璃
│  │  ├─ icon search     17×17 fill=text-secondary
│  │  └─ "搜索"          17sp fill=text-secondary
│  └─ Chat List          16,121 358×auto 纵向, 行高 74
│     └─ Row ×N: Avatar 52×52 r=26 | Name 17/600 | Time 13 | Preview 16 | Badge
└─ Tab Bar               0,752  390×78   padding=[8,16,12,16]
   └─ Tab Capsule        16,8   358×58 r=29 【玻璃】
      ├─ Specular（高光层，已在 shader 内实现，可省略）
      └─ Items           padding=6  横向均分 3 项
         ├─ 消息   选中态【玻璃凸起 r=23】icon + label
         ├─ 通讯录 未选中
         └─ 我的   未选中
```

### 2.2 02 · 会话（聊天详情）

```
Screen 390×830, bg=#FFFFFF
├─ Status Bar            0,0    390×62
├─ Messages              0,62   390×768  纵向 gap=10, padding=[70,16,90,16]
│  └─ 气泡行 ×N（Incoming / Outgoing 交替）
├─ Nav Bar               0,62   390×54   绝对定位，悬浮于消息之上
│  ├─ Back               8,10   34×34 r=17 【玻璃】icon=chevron-left
│  ├─ Peer               119,5  93×44 r=22 【玻璃】avatar 34 + 名字/在线
│  └─ Actions            288,5  90×44 r=22 【玻璃】icon=phone + video
└─ Input Bar             0,762  390×54   绝对定位
   ├─ Plus               12,8   34×34 r=17 【玻璃】icon=plus
   ├─ Field              54,8   282×34 r=17 【玻璃】"iMessage" 占位
   └─ Mic                344,8  34×34 r=17 【玻璃】icon=mic
```

**关键**：`Messages` 要延伸到导航栏和输入栏底下（上下各留 70/90dp 内边距），这样玻璃才有内容可折射。不要给 `Messages` 加 `clip`。

### 2.3 03 · 通讯录

```
Screen 390×830
├─ Status Bar
├─ Content               0,62   390×768  纵向 gap=18, padding=[2,16,0,16]
│  ├─ Header             358×41  Title "通讯录" + Add Button 40×40 【玻璃】
│  ├─ Search             358×38  r=11  ← 非玻璃
│  ├─ Favorites          358×101 常用联系人横向头像
│  ├─ Groups             358×244 群聊列表（3 行，行高 74）
│  └─ All Contacts       358×auto 按字母分组
└─ Tab Bar               0,752  390×78  （同 01）
```

### 2.4 05 · 我的

```
Screen 390×830, bg=#F2F2F7
├─ Status Bar
├─ Content               0,62   390×768  纵向 gap=20, padding=[2,16,0,16]
│  ├─ Header             358×41  Title "我的" + Settings Button 40×40 【玻璃】
│  ├─ Profile Card       358×96  r=14 fill=#FFFFFF
│  ├─ Stats              358×70  r=14 fill=#FFFFFF（3 列数字）
│  ├─ 账号               358×186 r=14 fill=#FFFFFF（3 行菜单）
│  ├─ 偏好               358×186 r=14 fill=#FFFFFF（3 行菜单）
│  └─ 退出登录           358×auto r=14 fill=#FFFFFF
└─ Tab Bar               0,752  390×78  （同 01）
```

---

## 3. 液态玻璃（Liquid Glass）实现方案

### 3.1 哪些元素是玻璃，哪些不是

**是玻璃（控制层）** —— 全部悬浮在内容之上：
- Tab Bar 胶囊 + 选中态凸起
- 导航栏三个胶囊（返回 / 联系人 / 通话视频）
- 输入栏三个控件（+ / 输入框 / 麦克风）
- 页面右上角圆形图标钮（Filter / 添加 / 设置）

**不是玻璃（内容层）** —— 保持实色：
- 搜索框（`#E9E9EB` 实色）
- 消息气泡
- 会话列表、联系人列表
- 卡片（Profile / Stats / 设置分组）

> 这是 Apple 的原始规则：玻璃只用于「悬浮在内容之上的功能层」，内容本身保持实色以保证可读性。

### 3.2 玻璃参数（按元素类型，共 3 档）

原设计用 WebGL shader 实现，参数如下。Android 端按语义映射：

| 元素 | 圆角 | 着色 tint | 模糊 | 边缘高光 | 阴影 |
|---|---|---|---|---|---|
| **Tab Bar 胶囊** | 29 | `#FFFFFF14` (8% 白) | 强 (3) | 中 (1.0) | `0 6 18 #0B122030` |
| **Tab 选中凸起** | 23 | `#FFFFFF40` (25% 白) | 中 (1.5) | 强 (1.3) | `0 3 8 #0B122030` |
| **图标按钮**（40dp） | 20 | `#FFFFFF33` (20% 白) | 弱 (1.5) | 中 (1.2) | `0 3 8 #0B122030` |
| **小圆钮**（34dp） | 17 | `#FFFFFF33` | 弱 (1.5) | 中 (1.2) | `0 2 6 #0B122030` |
| **导航/输入胶囊** | 22 | `#FFFFFF33` | 弱 (1.5) | 中 (1.2) | `0 2 6 #0B122030` |
| **输入框** | 17 | `#FFFFFF40` | 弱 (1.5) | 中 (1.2) | `0 2 6 #0B122030` |

所有玻璃的 tint 都**极低不透明度**（8%–25% 白）——颜色主要来自背后内容的模糊与折射，不是来自玻璃本身。这是液态玻璃和普通「磨砂玻璃」的核心区别。

### 3.3 Android 上的实现路径

**推荐：Haze 库**（`dev.chrisbanes.haze`，Compose 生态事实标准）

```kotlin
// 1. 在滚动内容上标记 hazeSource
LazyColumn(
    modifier = Modifier.hazeSource(state = hazeState)
) { /* 列表 */ }

// 2. 在玻璃元素上用 hazeEffect
Box(
    modifier = Modifier
        .clip(RoundedCornerShape(29.dp))
        .hazeEffect(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = Color.White.copy(alpha = 0.08f), // tint
                blurRadius = 30.dp,                                 // 强模糊档
                tints = listOf(HazeTint(Color.White.copy(alpha = 0.08f))),
            )
        )
)
```

- `blurRadius`：Tab Bar 用 `30.dp`，其余玻璃件用 `16.dp`
- `backgroundColor` 的 alpha 严格用上表的 tint 值
- Haze 支持 `noiseFactor`，可加极轻微噪点避免色带（`0.02f` 足够）

**边缘高光（必须有）**：Haze 不提供，需手动画。

```kotlin
// 在玻璃内容之上叠一层边框渐变
Box(
    modifier = Modifier
        .matchParentSize()
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.65f),   // 顶部亮
                0.5f to Color.White.copy(alpha = 0.20f),
                1f to Color.White.copy(alpha = 0.55f),   // 底部次亮
            ),
            shape = RoundedCornerShape(29.dp)
        )
)
```

**内侧暗边（可选，增强立体感）**：底部一条 `0.5–1dp` 的 `#0B122014` 内阴影，或直接用 `Brush.verticalGradient` 叠一层 `transparent → Color(0x140B1220)` 的 `0 → 8dp` 渐变。

**阴影**：Compose 的 `Modifier.shadow(elevation, shape, ambientColor, spotColor)` 在 Android 12+ 支持着色阴影：
```kotlin
Modifier.shadow(
    elevation = 8.dp,
    shape = RoundedCornerShape(29.dp),
    ambientColor = Color(0x300B1220),
    spotColor = Color(0x300B1220),
)
```

**注意**：`Modifier.shadow` 会画在玻璃下层，顺序必须是 `shadow → clip → hazeEffect → 内容 → border`。

### 3.4 边缘折射（可选，进阶）

原设计用 shader 做了「边缘透镜折射」——边缘像素被向内挤压，形成一圈扭曲的亮环。这是液态玻璃的灵魂，但 Compose 没有原生 backdrop 采样。

三个选项，按投入排序：

1. **不做**（推荐起步）：用 §3.3 的渐变边框模拟高光，视觉上已经 80% 接近，性能零成本。
2. **AGSL RuntimeShader**（Android 13+）：把背后内容截成 bitmap 作 `setInputBuffer`，用 `RuntimeShader` 做折射采样。真实但昂贵，且需要自己管理截屏与刷新，不建议第一版做。
3. **折中**：用 `RenderEffect.createBlurEffect()` + 极轻的 `graphicsLayer { scaleX = 1.01f }` 在边缘制造轻微形变，成本低，效果有限。

建议第一版走 **1**，跑通后再评估。

### 3.5 必须避开的坑

- ❌ `Modifier.blur()` —— 它模糊的是**组件自己**，不是背后的内容，用在这里完全无效
- ❌ 用高不透明度白色做「磨砂玻璃」——会变成塑料片，失去液态感
- ❌ 给内容层（气泡、卡片）上玻璃 —— 可读性崩坏，也不符合 Apple 的设计规则
- ✅ 玻璃元素必须**真的叠在滚动内容之上**，否则没东西可折射，看起来就是一块白板

---

## 4. 响应式与 Android 适配

| 项 | 设计值 | Android 做法 |
|---|---|---|
| 状态栏 | 62dp 固定 | `WindowInsets.statusBars` 动态取，不要写死 |
| 底部安全区 | Tab Bar 78dp 内含 12dp 底部 padding | `WindowInsets.navigationBars` + `Modifier.navigationBarsPadding()` |
| 页面宽度 | 390pt | `fillMaxWidth()`，不要写死 390dp |
| 左右边距 | 16dp | 保持固定 dp |
| 屏幕宽 < 380dp | — | Tab Bar 三项目文字可缩到 10sp；卡片内边距从 16 降到 12 |
| 横屏 / 平板 | — | 会话列表可用 `ListDetailPaneScaffold` 双栏；本 spec 只覆盖手机竖屏 |

**尺寸换算**：1pt = 1dp，字号 pt → sp 数值相同（设计基准密度 1x）。

---

## 5. 图标

设计用 **Lucide** 图标集（HTML 导出里是精确 SVG path）。Android 侧：

- 优先用 `androidx.compose.material.icons` 里语义相同的图标
- Lucide 有官方 Android 移植：`com.composables:icons-lucide`
- 若必须像素一致，把 `design.html` 里对应 `<svg><path d="...">` 的 path 数据直接转成 `ImageVector`（`pathData` 用 `PathParser` 解析）

用到的图标：`list-filter`、`search`、`square-pen`、`chevron-left`、`phone`、`video`、`plus`、`mic`、`user-plus`、`settings`、`message-circle`、`users`、`user`、`bell`、`palette`、`languages`、`qr-code`、`shield-check`。

---

## 6. 验收清单

AI 实现完成后，请逐项自检：

- [ ] 4 个页面都能在 393dp 宽下完整显示，无横向滚动
- [ ] Tab Bar 悬浮在列表之上，列表内容能从它底下穿过并透出模糊色彩
- [ ] Tab Bar 的 tint 是 8% 白左右（不是实心白）
- [ ] 玻璃元素有一圈渐变边框（顶部亮、中部暗、底部次亮）
- [ ] 搜索框是实色 `#E9E9EB`，**没有**模糊和半透明
- [ ] 消息气泡、卡片是实色，没有玻璃效果
- [ ] 选中态 Tab 是「更亮的玻璃凸起」，不是白色实心药丸
- [ ] 状态栏和底部导航栏都用了 `WindowInsets`，没有写死高度
- [ ] 文字颜色对比度足够（`text-secondary` 在 `#FFFFFF` 上为 3.0:1，用于次要文字可接受；正文必须用 `text-primary`）
