package com.yaunfei.singlechat.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaunfei.singlechat.core.designsystem.theme.Accent
import com.yaunfei.singlechat.core.designsystem.theme.TextSecondary
import com.yaunfei.singlechat.core.designsystem.theme.isDark
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * 页面级玻璃状态。
 *
 * 玻璃的质感来自「模糊背后的真实内容」，所以页面必须：
 * 1. 用 [rememberLiquidGlass] 创建状态；
 * 2. 把滚动内容标记为 [liquidGlassSource]；
 * 3. 把状态传给玻璃组件。
 * 否则玻璃背后没有任何内容可折射，看起来就是一块白板（DESIGN-SPEC §3.5）。
 */
@Composable
fun rememberLiquidGlass(): HazeState = remember { HazeState() }

/** 把滚动内容标记为玻璃的采样源（必须在玻璃元素之前绘制）。 */
fun Modifier.liquidGlassSource(state: HazeState): Modifier = this.hazeSource(state)

/**
 * 液态玻璃（Haze 真实背景模糊 + 边缘高光）。
 *
 * tint 刻意保持极低不透明度：颜色应主要来自背后的模糊内容，而不是玻璃本身。
 * 无 Haze 状态时（例如未接入的页面）回退为半透明填充，保证不会隐形。
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape,
    hazeState: HazeState? = null,
    tint: Color? = null,
    blurRadius: Dp = 18.dp,
    elevation: Dp = 6.dp,
): Modifier {
    val dark = MaterialTheme.colorScheme.isDark
    val haze = hazeState
    val glassTint = tint ?: if (dark) {
        Color(0xD91C1C1E)
    } else {
        Color.White.copy(alpha = 0.72f)
    }

    val body = this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = Color(0x330B1220),
            spotColor = Color(0x400B1220),
        )
        .clip(shape)
        .let { base ->
            if (haze != null) {
                base.hazeEffect(
                    state = haze,
                    style = HazeStyle(
                        backgroundColor = Color.Transparent,
                        tints = listOf(HazeTint(glassTint)),
                        blurRadius = blurRadius,
                        noiseFactor = 0.02f,
                    ),
                )
            } else {
                base.background(if (dark) Color(0xE62C2C2E) else glassTint)
            }
        }

    return body.border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.45f),
            0.45f to Color.White.copy(alpha = 0.10f),
            1f to Color.White.copy(alpha = 0.30f),
        ),
        shape = shape,
    )
}

/** 玻璃圆形图标钮（DESIGN-SPEC §3.2 图标按钮档）。 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    hazeState: HazeState? = null,
) {
    val dark = MaterialTheme.colorScheme.isDark
    Box(
        modifier = modifier
            .size(size)
            .liquidGlass(
                shape = RoundedCornerShape(size / 2),
                hazeState = hazeState,
                tint = if (dark) Color(0xCC2C2C2E) else Color.White.copy(alpha = 0.90f),
                blurRadius = 14.dp,
                elevation = 5.dp,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(iconSize),
        )
    }
}

data class GlassTab(val label: String, val icon: ImageVector)

/**
 * 悬浮玻璃 Tab Bar（DESIGN-SPEC §2.1）：
 * 58dp 玻璃胶囊 r=29，选中项是更亮的玻璃凸起（r=23），不是实心药丸。
 */
@Composable
fun GlassTabBar(
    tabs: List<GlassTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
) {
    val dark = MaterialTheme.colorScheme.isDark
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
    ) {
        val capsuleShape = RoundedCornerShape(29.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .liquidGlass(
                    shape = capsuleShape,
                    hazeState = hazeState,
                    tint = if (dark) Color(0xD91C1C1E) else Color.White.copy(alpha = 0.72f),
                    blurRadius = 30.dp,
                    elevation = 12.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
            // 底部内阴影：让胶囊有玻璃厚度。
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to Color(0x140B1220),
                        ),
                    ),
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            ) {
                tabs.forEachIndexed { index, tab ->
                    val selected = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onSelect(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .liquidGlass(
                                        shape = RoundedCornerShape(23.dp),
                                        hazeState = hazeState,
                                        tint = if (dark) {
                                            Color(0xF02C2C2E)
                                        } else {
                                            Color.White.copy(alpha = 0.92f)
                                        },
                                        blurRadius = 18.dp,
                                        elevation = 3.dp,
                                    )
                                    .padding(horizontal = 18.dp, vertical = 5.dp),
                            ) {
                                TabContent(tab, tint = Accent)
                            }
                        } else {
                            TabContent(tab, tint = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabContent(tab: GlassTab, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = tab.label,
            fontSize = 11.sp,
            color = tint,
            maxLines = 1,
        )
    }
}

/** 实色搜索框（DESIGN-SPEC §3.1：搜索框不是玻璃）。 */
@Composable
fun SolidSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(7.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = com.yaunfei.singlechat.core.designsystem.theme.TextTertiary,
                    )
                }
                inner()
            },
        )
        if (value.isNotEmpty()) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
                    .clickable { onValueChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "清空",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}
