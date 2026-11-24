package com.yaunfei.singlechat.feature.conversations

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yaunfei.singlechat.core.designsystem.component.Avatar
import com.yaunfei.singlechat.core.designsystem.component.GlassIconButton
import com.yaunfei.singlechat.core.designsystem.component.MainGlassTabBar
import com.yaunfei.singlechat.core.designsystem.component.SolidSearchField
import com.yaunfei.singlechat.core.designsystem.component.liquidGlassSource
import com.yaunfei.singlechat.core.designsystem.component.rememberLiquidGlass
import com.yaunfei.singlechat.core.designsystem.theme.Accent
import com.yaunfei.singlechat.core.designsystem.theme.Separator
import com.yaunfei.singlechat.core.designsystem.theme.TextSecondary
import com.yaunfei.singlechat.core.model.Contact
import com.yaunfei.singlechat.core.model.Conversation
import com.yaunfei.singlechat.core.util.formatConversationTime
import dev.chrisbanes.haze.HazeState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    onOpenChat: (String) -> Unit,
    onTabSelect: (Int) -> Unit,
    viewModel: ConversationListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val connectionDetail by viewModel.connectionDetail.collectAsStateWithLifecycle()
    var showNewChat by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Conversation?>(null) }
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val hazeState = rememberLiquidGlass()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // Header：大标题 + 玻璃筛选按钮（DESIGN-SPEC §2.1）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "消息",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.weight(1f))
            GlassIconButton(
                icon = Icons.Filled.Tune,
                contentDescription = "筛选",
                onClick = { showNewChat = true },
                hazeState = hazeState,
            )
        }

        SolidSearchField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            placeholder = "搜索",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp),
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                state.conversations.isEmpty() -> EmptyConversations(
                    query = state.query,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 100.dp),
                )

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .liquidGlassSource(hazeState),
                    contentPadding = PaddingValues(
                        top = 4.dp,
                        bottom = 110.dp + navBarBottom,
                    ),
                ) {
                    items(state.conversations, key = { it.id }) { conversation ->
                        ConversationRow(
                            conversation = conversation,
                            onClick = {
                                viewModel.markRead(conversation.id)
                                onOpenChat(conversation.id)
                            },
                            onMarkRead = { viewModel.markRead(conversation.id) },
                            onDelete = { pendingDelete = conversation },
                        )
                    }
                }
            }

            MainGlassTabBar(
                selectedIndex = 0,
                onSelect = onTabSelect,
                modifier = Modifier.align(Alignment.BottomCenter),
                hazeState = hazeState,
            )
        }
    }

    if (showNewChat) {
        ModalBottomSheet(
            onDismissRequest = { showNewChat = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            NewChatSheet(
                contacts = contacts,
                onPick = { contact ->
                    viewModel.createConversation(contact) { conversationId ->
                        showNewChat = false
                        onOpenChat(conversationId)
                    }
                },
                onCreateById = { peerId ->
                    viewModel.createConversationById(peerId) { conversationId ->
                        showNewChat = false
                        onOpenChat(conversationId)
                    }
                },
            )
            Spacer(Modifier.height(12.dp))
        }
    }

    pendingDelete?.let { conversation ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除会话") },
            text = { Text("将删除与「${conversation.title}」的本地聊天记录，且不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteConversation(conversation.id)
                        pendingDelete = null
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationRow(
    conversation: Conversation,
    onClick: () -> Unit,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 74.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true },
                )
                .padding(start = 16.dp, end = 16.dp, top = 11.dp, bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(
                seed = conversation.avatarSeed,
                name = conversation.title,
                isGroup = conversation.isGroup,
                size = 52.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = conversation.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatConversationTime(conversation.lastMessageAt),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = conversation.lastMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (conversation.unreadCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        UnreadBadge(conversation.unreadCount)
                    }
                }
            }
        }
        HorizontalDivider(
            color = Separator,
            thickness = 0.5.dp,
            modifier = Modifier
                .padding(start = 86.dp)
                .align(Alignment.BottomStart),
        )

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("标为已读") },
                onClick = {
                    menuExpanded = false
                    onMarkRead()
                },
            )
            DropdownMenuItem(
                text = { Text("删除会话") },
                onClick = {
                    menuExpanded = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun UnreadBadge(count: Int) {
    Box(
        modifier = Modifier
            .height(18.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Accent)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = androidx.compose.ui.graphics.Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun NewChatSheet(
    contacts: List<Contact>,
    onPick: (Contact) -> Unit,
    onCreateById: (String) -> Unit,
) {
    var peerId by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    ) {
        Text(
            text = "开始新的对话",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "输入对方的用户 ID，或从最近联系人中继续。",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = peerId,
            onValueChange = { peerId = it.take(64) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            placeholder = { Text("对方用户 ID，如 u_1a2b3c") },
            singleLine = true,
            shape = RoundedCornerShape(17.dp),
        )
        TextButton(
            onClick = { onCreateById(peerId) },
            enabled = peerId.isNotBlank(),
            modifier = Modifier.padding(start = 24.dp),
        ) {
            Text("开始聊天")
        }

        if (contacts.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "最近联系人",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(4.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(contacts, key = { it.id }) { contact ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(60.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onPick(contact) }
                            .padding(vertical = 8.dp),
                    ) {
                        Avatar(seed = contact.id, name = contact.name, size = 52.dp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyConversations(
    query: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Accent.copy(alpha = 0.1f),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "聊",
                    color = Accent,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = if (query.isBlank()) "还没有对话" else "没有找到会话",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (query.isBlank()) {
                "点右上角，和一个人说说话。"
            } else {
                "换个关键词试试。"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
