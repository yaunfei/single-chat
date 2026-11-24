package com.yaunfei.singlechat.feature.chat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yaunfei.singlechat.core.designsystem.component.Avatar
import com.yaunfei.singlechat.core.designsystem.component.GlassIconButton
import com.yaunfei.singlechat.core.designsystem.component.liquidGlassSource
import com.yaunfei.singlechat.core.designsystem.component.rememberLiquidGlass
import com.yaunfei.singlechat.core.designsystem.component.liquidGlass
import com.yaunfei.singlechat.core.designsystem.theme.BubbleIncoming
import com.yaunfei.singlechat.core.designsystem.theme.BubbleOutgoing
import com.yaunfei.singlechat.core.designsystem.theme.OnlineGreen
import com.yaunfei.singlechat.core.designsystem.theme.TextSecondary
import com.yaunfei.singlechat.core.designsystem.theme.isDark
import com.yaunfei.singlechat.core.model.Conversation
import com.yaunfei.singlechat.core.model.Message
import com.yaunfei.singlechat.core.model.MessageStatus
import com.yaunfei.singlechat.core.model.MessageType
import com.yaunfei.singlechat.core.util.formatDateDivider
import com.yaunfei.singlechat.core.util.formatMessageTime
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 聊天详情（DESIGN-SPEC §2.2 · 02）：
 * 消息列表延伸到导航栏与输入栏底下；玻璃只出现在悬浮控制层。
 */
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val conversation by viewModel.conversation.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val typing by viewModel.isTyping.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val sendError by viewModel.sendError.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val dark = MaterialTheme.colorScheme.isDark

    LaunchedEffect(sendError) {
        sendError?.let { snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long) }
    }

    val items = remember(messages) { buildChatItems(messages) }
    val listState = rememberLazyListState()
    var didInitialScroll by remember { mutableStateOf(false) }

    LaunchedEffect(items.size, typing) {
        val total = items.size + if (typing) 1 else 0
        if (total == 0) return@LaunchedEffect
        if (!didInitialScroll) {
            listState.scrollToItem(total - 1)
            didInitialScroll = true
        } else {
            listState.animateScrollToItem(total - 1)
        }
    }

    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val hazeState = rememberLiquidGlass()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // 消息层：延伸到两个玻璃栏底下（DESIGN-SPEC §2.2 关键）。
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .liquidGlassSource(hazeState)
                .imePadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = statusTop + 62.dp,
                bottom = navBottom + 78.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(
                items = items,
                key = { item ->
                    when (item) {
                        is ChatItem.DateHeader -> "date_${item.timestamp}"
                        is ChatItem.Bubble -> item.message.id
                    }
                },
            ) { item ->
                when (item) {
                    is ChatItem.DateHeader -> DateDivider(item.timestamp)
                    is ChatItem.Bubble -> MessageRow(
                        message = item.message,
                        conversation = conversation,
                        dark = dark,
                        onRetry = viewModel::retry,
                    )
                }
            }
            if (typing) {
                item(key = "typing_indicator") {
                    TypingRow(conversation = conversation, dark = dark)
                }
            }
        }

        // 顶部玻璃导航（悬浮于消息之上）
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "返回",
                onClick = onBack,
                size = 34.dp,
                iconSize = 22.dp,
                hazeState = hazeState,
            )
            Spacer(Modifier.width(10.dp))

            // 对方信息胶囊
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier.liquidGlass(
                        shape = RoundedCornerShape(22.dp),
                        hazeState = hazeState,
                        tint = if (dark) {
                            Color(0xD91C1C1E)
                        } else {
                            Color.White.copy(alpha = 0.86f)
                        },
                        blurRadius = 22.dp,
                        elevation = 4.dp,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(start = 6.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(
                            seed = conversation?.avatarSeed ?: "chat",
                            name = conversation?.title ?: "聊天",
                            size = 34.dp,
                            isGroup = conversation?.isGroup == true,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = conversation?.title ?: "聊天",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 140.dp),
                            )
                            Text(
                                text = if (conversation?.isGroup == true) "群聊" else "在线",
                                fontSize = 12.sp,
                                color = if (conversation?.isGroup == true) {
                                    TextSecondary
                                } else {
                                    OnlineGreen
                                },
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(10.dp))

            // 通话胶囊（视觉占位，暂无通话功能）
            Row(
                modifier = Modifier.liquidGlass(
                    shape = RoundedCornerShape(22.dp),
                    hazeState = hazeState,
                    tint = if (dark) {
                        Color(0xD91C1C1E)
                    } else {
                        Color.White.copy(alpha = 0.86f)
                    },
                    blurRadius = 22.dp,
                    elevation = 4.dp,
                ).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Filled.Videocam,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // 底部玻璃输入栏
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(
                icon = Icons.Filled.Add,
                contentDescription = null,
                onClick = {},
                size = 34.dp,
                iconSize = 18.dp,
                iconTint = MaterialTheme.colorScheme.primary,
                hazeState = hazeState,
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .liquidGlass(
                        shape = RoundedCornerShape(17.dp),
                        hazeState = hazeState,
                        tint = if (dark) {
                            Color(0xE62C2C2E)
                        } else {
                            Color.White.copy(alpha = 0.88f)
                        },
                        blurRadius = 16.dp,
                        elevation = 3.dp,
                    )
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = viewModel::onDraftChange,
                    maxLines = 4,
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (draft.isEmpty()) {
                            Text(
                                text = "说点什么…",
                                fontSize = 15.sp,
                                color = TextSecondary,
                            )
                        }
                        inner()
                    },
                )
            }
            Spacer(Modifier.width(10.dp))
            GlassIconButton(
                icon = if (draft.isNotBlank()) Icons.Filled.Send else Icons.Filled.Mic,
                contentDescription = if (draft.isNotBlank()) "发送" else null,
                onClick = { if (draft.isNotBlank()) viewModel.send() },
                size = 34.dp,
                iconSize = 17.dp,
                iconTint = if (draft.isNotBlank()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    TextSecondary
                },
                hazeState = hazeState,
            )
        }

        // 发送错误 Snackbar，浮在输入栏上方
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 54.dp),
        ) { data -> Snackbar(data) }
    }
}

@Composable
private fun MessageRow(
    message: Message,
    conversation: Conversation?,
    dark: Boolean,
    onRetry: (String) -> Unit,
) {
    if (message.type == MessageType.SYSTEM) {
        SystemMessage(message)
        return
    }

    val mine = message.isMine
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!mine) {
            Avatar(
                seed = message.senderId,
                name = message.senderName,
                size = 30.dp,
            )
            Spacer(Modifier.width(8.dp))
        }
        Column(
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
        ) {
            if (!mine && conversation?.isGroup == true) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                )
            }
            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(
                        if (mine) {
                            RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
                        } else {
                            RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
                        },
                    )
                    .background(
                        when {
                            mine -> BubbleOutgoing
                            dark -> Color(0xFF2C2C2E)
                            else -> BubbleIncoming
                        },
                    )
                    .alpha(if (message.status == MessageStatus.SENDING) 0.72f else 1f),
            ) {
                SelectionContainer {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (mine) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    text = formatMessageTime(message.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
                if (mine) {
                    Spacer(Modifier.width(4.dp))
                    val statusText = when (message.status) {
                        MessageStatus.SENDING -> "发送中"
                        MessageStatus.SENT -> "已送达"
                        MessageStatus.FAILED -> "失败，点此重试"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (message.status == MessageStatus.FAILED) {
                            MaterialTheme.colorScheme.error
                        } else {
                            TextSecondary
                        },
                        modifier = if (message.status == MessageStatus.FAILED) {
                            Modifier.clickable { onRetry(message.id) }
                        } else {
                            Modifier
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemMessage(message: Message) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            shape = RoundedCornerShape(50),
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun DateDivider(timestamp: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = formatDateDivider(timestamp),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )
    }
}

@Composable
private fun TypingRow(conversation: Conversation?, dark: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Avatar(
            seed = conversation?.avatarSeed ?: "typing",
            name = conversation?.title ?: "对方",
            size = 30.dp,
            isGroup = conversation?.isGroup == true,
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                .background(if (dark) Color(0xFF2C2C2E) else BubbleIncoming)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                val transition = rememberInfiniteTransition(label = "typing")
                repeat(3) { index ->
                    val alpha by transition.animateFloat(
                        initialValue = 0.25f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 520, delayMillis = index * 140),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "typing_dot_$index",
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(TextSecondary.copy(alpha = alpha)),
                    )
                    if (index < 2) Spacer(Modifier.width(5.dp))
                }
            }
        }
    }
}

private sealed interface ChatItem {
    data class DateHeader(val timestamp: Long) : ChatItem
    data class Bubble(val message: Message) : ChatItem
}

private fun buildChatItems(messages: List<Message>): List<ChatItem> = buildList {
    var lastDay: LocalDate? = null
    messages.forEach { message ->
        val day = Instant.ofEpochMilli(message.createdAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        if (day != lastDay) {
            add(ChatItem.DateHeader(message.createdAt))
            lastDay = day
        }
        add(ChatItem.Bubble(message))
    }
}
