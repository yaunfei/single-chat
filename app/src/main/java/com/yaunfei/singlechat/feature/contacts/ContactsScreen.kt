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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.yaunfei.singlechat.core.designsystem.theme.Separator
import com.yaunfei.singlechat.core.designsystem.theme.TextSecondary
import com.yaunfei.singlechat.core.model.Contact
import com.yaunfei.singlechat.core.model.GroupInfo

@Composable
fun ContactsScreen(
    onOpenChat: (String) -> Unit,
    onTabSelect: (Int) -> Unit = {},
    viewModel: ContactsViewModel = hiltViewModel(),
) {
    val contacts by viewModel.filteredContacts.collectAsStateWithLifecycle()
    val frequentlyUsed by viewModel.frequentlyUsed.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var addContactId by remember { mutableStateOf("") }
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val hazeState = rememberLiquidGlass()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // Header
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
                contentDescription = "添加好友",
                onClick = { showAddDialog = true },
                hazeState = hazeState,
            )
        }

        SolidSearchField(
            value = searchQuery,
            onValueChange = viewModel::setSearchQuery,
            placeholder = "搜索联系人",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp),
        )

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .liquidGlassSource(hazeState),
                contentPadding = PaddingValues(
                    top = 12.dp,
                    bottom = 110.dp + navBarBottom,
                ),
            ) {
                // 常用 Section
                if (frequentlyUsed.isNotEmpty()) {
                    item {
                        SectionHeader(text = "常用")
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(frequentlyUsed, key = { it.id }) { contact ->
                                FrequentlyContactItem(
                                    contact = contact,
                                    onClick = { onOpenChat(contact.id) },
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }

                // 群聊 Section
                if (groups.isNotEmpty()) {
                    item {
                        SectionHeader(text = "群聊 · ${groups.size}")
                    }
                    items(groups, key = { it.id }) { group ->
                        GroupRow(
                            group = group,
                            onClick = { onOpenChat(group.id) },
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }

                // 联系人 Section
                if (contacts.isNotEmpty()) {
                    item {
                        SectionHeader(text = "联系人 · ${contacts.size}")
                    }
                    items(contacts, key = { it.id }) { contact ->
                        ContactRow(
                            contact = contact,
                            onClick = { onOpenChat(contact.id) },
                        )
                    }
                }

                if (contacts.isEmpty() && frequentlyUsed.isEmpty() && groups.isEmpty()) {
                    item {
                        EmptyContacts(modifier = Modifier.padding(32.dp))
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
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加好友") },
            text = {
                Column {
                    Text("输入对方的用户 ID。", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    TextField(
                        value = addContactId,
                        onValueChange = { addContactId = it },
                        placeholder = { Text("用户 ID") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addContactById(addContactId)
                        addContactId = ""
                        showAddDialog = false
                    },
                    enabled = addContactId.isNotBlank(),
                ) {
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
    )
}

@Composable
private fun FrequentlyContactItem(
    contact: Contact,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        Avatar(seed = contact.id, name = contact.name, size = 48.dp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = contact.name,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ContactRow(
    contact: Contact,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(seed = contact.id, name = contact.name, size = 52.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (contact.signature.isNullOrBlank().not()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = contact.signature ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    HorizontalDivider(
        color = Separator,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 80.dp),
    )
}

@Composable
private fun GroupRow(
    group: GroupInfo,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            seed = group.name,
            name = group.name,
            size = 52.dp,
            isGroup = true,
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${group.memberCount} 人",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )
        }
    }
    HorizontalDivider(
        color = Separator,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 80.dp),
    )
}

@Composable
private fun EmptyContacts(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "联",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = "暂无联系人",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "点右上角添加好友，开始对话。",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
