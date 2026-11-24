package com.yaunfei.singlechat.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 设计稿配色：每个联系人有稳定的渐变
private val AvatarPalettes = listOf(
    // 林晓 - 紫蓝
    listOf(Color(0xFF8C9BFF), Color(0xFF5B6BE6)),
    // 产品设计组 - 粉红
    listOf(Color(0xFFFFB3C7), Color(0xFFF26D93)),
    // 陈默 - 青绿
    listOf(Color(0xFF7FD1C1), Color(0xFF3FA894)),
    // 妈妈 - 暖橙
    listOf(Color(0xFFFFCF8B), Color(0xFFF0A93C)),
    // 周舟 - 紫色
    listOf(Color(0xFFB9A7FF), Color(0xFF8A6FF0)),
    // 设计灵感 - 天蓝
    listOf(Color(0xFF9AD0FF), Color(0xFF4E9CF0)),
    // Aaron Wu - 灰银
    listOf(Color(0xFFD5D5DA), Color(0xFFA9A9B0)),
    // 张三 - 珊瑚
    listOf(Color(0xFFF7B7A3), Color(0xFFE87C5D)),
    // 李四 - 嫩绿
    listOf(Color(0xFFB8E986), Color(0xFF6DBE45)),
    // 王五 - 浅蓝
    listOf(Color(0xFFA3D9FF), Color(0xFF3E9BE9)),
    // 赵六 - 金橙
    listOf(Color(0xFFFFD3A5), Color(0xFFFF9A3C)),
    // 孙七 - 紫罗兰
    listOf(Color(0xFFD7A9FF), Color(0xFF9B5DE5)),
)

@Composable
fun Avatar(
    seed: String,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    isGroup: Boolean = false,
) {
    val palette = remember(seed) {
        AvatarPalettes[Math.floorMod(seed.hashCode(), AvatarPalettes.size)]
    }
    // 设计稿：人是正圆，群聊是圆角方块（圆角约 size*0.5 即胶囊形）
    val shape = if (isGroup) {
        RoundedCornerShape(size * 0.5f)
    } else {
        CircleShape
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Brush.linearGradient(palette)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isGroup) "组" else name.take(1),
            color = Color.White,
            fontSize = (size.value * 0.38f).sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}
