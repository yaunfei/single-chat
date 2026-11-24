package com.yaunfei.singlechat.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// ——— iOS 26 Liquid Glass 设计 token（DESIGN-SPEC §1.1）———

/** 页面主背景（01/02/03） */
val Bg = Color(0xFFFFFFFF)

/** 分组背景（05 我的） */
val BgGrouped = Color(0xFFF2F2F7)

/** 搜索框底色、未选中胶囊、接收气泡 */
val FillTertiary = Color(0xFFE9E9EB)

val TextPrimary = Color(0xFF000000)
val TextSecondary = Color(0xFF8E8E93)
val TextTertiary = Color(0xFFC7C7CC)

/** 0.5dp 分隔线 */
val Separator = Color(0xFFE5E5EA)

/** 强调色：发送气泡、未读角标、图标 */
val Accent = Color(0xFF007AFF)

/** 在线状态点 */
val OnlineGreen = Color(0xFF34C759)

val BubbleIncoming = Color(0xFFE9E9EB)
val BubbleOutgoing = Color(0xFF007AFF)

val LightColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E9FF),
    onPrimaryContainer = Color(0xFF003B80),
    secondary = OnlineGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3F4DE),
    onSecondaryContainer = Color(0xFF06381A),
    tertiary = Color(0xFFFF9F0A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEFCC),
    onTertiaryContainer = Color(0xFF451A03),
    background = Bg,
    onBackground = TextPrimary,
    surface = Color(0xFFFFFFFF),
    onSurface = TextPrimary,
    surfaceVariant = BgGrouped,
    onSurfaceVariant = TextSecondary,
    outline = Separator,
    outlineVariant = Separator,
    error = Color(0xFFFF3B30),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF2C2C2E),
    inverseOnSurface = Color(0xFFF2F2F7),
    inversePrimary = Color(0xFF0A84FF),
    scrim = Color.Black,
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0A3C6E),
    onPrimaryContainer = Color(0xFFCCE5FF),
    secondary = Color(0xFF30D158),
    onSecondary = Color(0xFF06381A),
    secondaryContainer = Color(0xFF1E462B),
    onSecondaryContainer = Color(0xFFB8F0C8),
    tertiary = Color(0xFFFFD60A),
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF6E5000),
    onTertiaryContainer = Color(0xFFFFE0AC),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF2F2F7),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF98989F),
    outline = Color(0xFF38383A),
    outlineVariant = Color(0xFF38383A),
    error = Color(0xFFFF453A),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFF2F2F7),
    inverseOnSurface = Color(0xFF2C2C2E),
    inversePrimary = Accent,
    scrim = Color.Black,
)

/** 主题明暗判断，玻璃/气泡等自定义组件用。 */
val ColorScheme.isDark: Boolean
    get() = background.luminance() < 0.5f
