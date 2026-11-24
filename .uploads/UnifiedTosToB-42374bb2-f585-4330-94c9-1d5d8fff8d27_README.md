# 设计交付说明 · Android 开发

本目录是给 AI 编程工具（Claude Code / Cursor / Copilot 等）做 Android 项目的完整交付包。

## 目录内容

| 文件 | 用途 | 优先级 |
|---|---|---|
| `DESIGN-SPEC.md` | **给 AI 的主输入**：设计 token、页面结构、玻璃效果参数与 Android 实现方案 | ⭐ 必读 |
| `design.html` | 完整布局的 HTML/CSS 导出（内联样式，含精确 px 值、字体、图标 SVG path） | ⭐ 必读 |
| `png/*.png` | 4 个页面 3x 位图，作为视觉对照基准 | ⭐ 必读 |
| `design.pen` | 设计源文件，后续要改设计时用 | 备查 |

## 建议的 AI 提示词

```
这是 Android（Jetpack Compose）项目的设计交付包。请先读 DESIGN-SPEC.md 和 design.html，
按 png/ 里的 4 张图实现界面。要求：
1. 用 Jetpack Compose + Material 3
2. 颜色/圆角/间距全部取自 DESIGN-SPEC.md 的 token，不要硬编码
3. 毛玻璃效果按 DESIGN-SPEC.md「液态玻璃实现方案」一节处理
4. 文字用 sp、尺寸用 dp，按 1pt = 1dp 换算
```

## 关于格式的选择

- **HTML/CSS 导出比 SVG 更适合 AI**：SVG 把文字转成路径，AI 读不出字号、颜色语义和 flex 布局；HTML 保留了 `font-size`、`gap`、`padding`、`border-radius` 等全部数值，AI 可以直接映射到 Compose 的 `Modifier.padding()` / `Row(gap)` / `RoundedCornerShape()`。
- **PNG 是给 AI 做视觉回归的**：让它实现完自己截图对比，不要只靠数值。
- **不需要 PDF**：PDF 只适合给人类看，AI 读起来是图形。
- **不要导出 SVG 单屏**：文字会变成 path，丢失语义。

## 一个前提

本设计是 iOS 风格（390×830pt，iPhone 逻辑尺寸）。做 Android 时：

- 尺寸按 **1pt = 1dp** 直接换算，在 393dp 宽（Pixel 8）上基本一致
- 在 360dp 窄屏上需要压测，`fill_container` 的地方要换成 `Modifier.weight(1f)` / `fillMaxWidth()`
- 状态栏高度 62dp 是 iPhone 的，Android 要用 `WindowInsets.statusBars` 动态取
- Tab Bar 的 78dp 底部区域要换成 `WindowInsets.navigationBars`
