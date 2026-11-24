package com.yaunfei.singlechat.feature.contacts

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
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yaunfei.singlechat.core.designsystem.component.Avatar
import com.yaunfei.singlechat.core.designsystem.component.GlassIconButton
import com.yaunfei.singlechat.core.designsystem.component.SolidSearchField
import com.yaunfei.singlechat.core.designsystem.component.liquidGlassSource
import com.yaunfei.singlechat.core.designsystem.component.rememberLiquidGlass
import com.yaunfei.singlechat.core.designsystem.theme.Separator
import com.yaunfei.singlechat.core.designsystem.theme.TextSecondary
import com.yaunfei.singlechat.feature.conversations.MainGlassTabBar

@Composable
fun ContactsScreen(
    onOpenChat: (String) -> Unit,
    onTabSelect: (Int) -> Unit,
    viewModel: ContactsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val hazeState = rememberLiquidGlass()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // Header：大标题 + 玻璃添加联系人按钮（DESIGN-SPEC §2.3）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "通讯录",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.weight(1f))
            GlassIconButton(
                icon = Icons.Filled.PersonAdd,
                contentDescription = "添加联系人",
                onClick = { showAddDialog = true },
            )
        }

        SolidSearchField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            placeholder = "搜索联系人",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp),
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.contacts.isEmpty() -> EmptyContacts(
                    hasQuery = state.query.isNotBlank(),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 100.dp),
                )

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .liquidGlassSource(hazeState),
                    contentPadding = PaddingValues(
                        top = 16.dp,
                        bottom = 110.dp + navBarBottom,
                    ),
                ) {
                    if (state.query.isBlank() && state.contacts.size > 1) {
                        item(key = "favorites_header") { SectionLabel("常用") }
                        item(key = "favorites_row") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(18.dp),
                            ) {
                                items(
                                    state.contacts.take(8),
                                    key = { "fav_${it.id}" },
                                ) { contact ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(60.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { openContact(contact, onOpenChat, viewModel) }
                                            .padding(vertical = 4.dp),
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
                        item(key = "all_header") {
                            SectionLabel("全部联系人 · ${state.contacts.size}")
                        }
                    }
                    items(state.contacts, key = { it.id }) { contact ->
                        ContactRow(
                            contact = contact,
                            onClick = { openContact(contact, onOpenChat, viewModel) },
                        )
                    }
                }
            }

            MainGlassTabBar(
                selectedIndex = 1,
                onSelect = onTabSelect,
                modifier = Modifier.align(Alignment.BottomCenter),
                hazeState = hazeState,
            )
        }
    }

    if (showAddDialog) {
        var peerId by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加联系人") },
            text = {
                Column {
                    Text("输入对方的云端用户 ID，直接发起对话。")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = peerId,
                        onValueChange = { peerId = it.take(64) },
                        placeholder = { Text("如 u_1a2b3c") },
                        singleLine = true,
                        shape = RoundedCornerShape(17.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = peerId.isNotBlank(),
                    onClick = {
                        showAddDialog = false
                        viewModel.createConversationById(peerId) { conversationId ->
                            onOpenChat(conversationId)
                        }
                    },
                ) {
                    Text("开始聊天")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("取消") }
            },
        )
    }
}

private fun openContact(
    contact: com.yaunfei.singlechat.core.model.Contact,
    onOpenChat: (String) -> Unit,
    viewModel: ContactsViewModel,
) {
    viewModel.createConversation(contact) { conversationId -> onOpenChat(conversationId) }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary,
        modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 8.dp),
    )
}

@Composable
private fun ContactRow(
    contact: com.yaunfei.singlechat.core.model.Contact,
    onClick: () -> Unit,
) {
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(onClick = onClick)
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(seed = contact.id, name = contact.name, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (contact.bio.isNotBlank()) {
                    Text(
                        text = contact.bio,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        androidx.compose.material3.HorizontalDivider(
            color = Separator,
            thickness = 0.5.dp,
            modifier = Modifier
                .padding(start = 68.dp)
                .align(Alignment.BottomStart),
        )
    }
}

@Composable
private fun EmptyContacts(
    hasQuery: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "友",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 24.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = if (hasQuery) "没有找到联系人" else "通讯录还是空的",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (hasQuery) {
                "换个关键词试试。"
            } else {
                "点右上角，输入对方的用户 ID 开始聊天。"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
