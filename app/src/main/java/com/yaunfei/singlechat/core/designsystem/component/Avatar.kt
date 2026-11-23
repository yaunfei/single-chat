package com.yaunfei.singlechat.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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

private val AvatarPalettes = listOf(
    listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)),
    listOf(Color(0xFF0EA5E9), Color(0xFF14B8A6)),
    listOf(Color(0xFFF97316), Color(0xFFEF4444)),
    listOf(Color(0xFF10B981), Color(0xFF84CC16)),
    listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)),
    listOf(Color(0xFF0F766E), Color(0xFF0284C7)),
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
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.34f))
            .background(Brush.linearGradient(palette)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isGroup) "群" else name.take(1),
            color = Color.White,
            fontSize = (size.value * 0.36f).sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}
