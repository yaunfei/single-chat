package com.yaunfei.singlechat.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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

/**
 * 液态玻璃（静态实现，DESIGN-SPEC §3.4 方案 1）：
 * 半透明底 + 顶部到底部的渐变高光边 + 柔和投影。
 * 玻璃只用于「悬浮在内容之上的功能层」，内容层（气泡/卡片/列表）保持实色。
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape,
    tint: Color? = null,
    elevation: Dp = 6.dp,
): Modifier {
    val dark = MaterialTheme.colorScheme.isDark
    val baseTint = tint ?: if (dark) Color(0xCC2C2C2E) else Color(0x99FFFFFF)
    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = Color(0x300B1220),
            spotColor = Color(0x300B1220),
        )
        .clip(shape)
        .background(baseTint)
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.65f),
                    Color.White.copy(alpha = 0.20f),
                    Color.White.copy(alpha = 0.55f),
                ),
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
) {
    Box(
        modifier = modifier
            .size(size)
            .liquidGlass(shape = RoundedCornerShape(size / 2))
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
) {
    val dark = MaterialTheme.colorScheme.isDark
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .liquidGlass(
                    shape = RoundedCornerShape(29.dp),
                    tint = if (dark) Color(0xD91C1C1E) else Color(0xB3FFFFFF),
                    elevation = 10.dp,
                ),
            contentAlignment = Alignment.Center,
        ) {
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
                                        tint = if (dark) Color(0xF02C2C2E) else Color(0xF2FFFFFF),
                                        elevation = 2.dp,
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
