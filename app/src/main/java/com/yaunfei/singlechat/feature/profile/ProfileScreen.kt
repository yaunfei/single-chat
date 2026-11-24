package com.yaunfei.singlechat.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yaunfei.singlechat.BuildConfig
import com.yaunfei.singlechat.core.chat.ChatConnectionState
import com.yaunfei.singlechat.core.designsystem.component.Avatar
import com.yaunfei.singlechat.core.designsystem.component.GlassIconButton
import com.yaunfei.singlechat.core.designsystem.component.liquidGlassSource
import com.yaunfei.singlechat.core.designsystem.component.rememberLiquidGlass
import com.yaunfei.singlechat.core.designsystem.theme.OnlineGreen
import com.yaunfei.singlechat.core.designsystem.theme.TextSecondary
import com.yaunfei.singlechat.core.model.Session
import com.yaunfei.singlechat.core.model.ThemeMode
import com.yaunfei.singlechat.feature.conversations.MainGlassTabBar
import com.yaunfei.singlechat.feature.settings.ProfileStats
import com.yaunfei.singlechat.feature.settings.SettingsViewModel

/**
 * 我的页（DESIGN-SPEC §2.4 · 05）：
 * 分组背景 + 白卡片（Profile / Stats / 设置分组）+ 悬浮玻璃 TabBar。
 */
@Composable
fun ProfileScreen(
    onTabSelect: (Int) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val connection by viewModel.connectionState.collectAsStateWithLifecycle()
    var showServerDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val hazeState = rememberLiquidGlass()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "我的",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.weight(1f))
            GlassIconButton(
                icon = Icons.Filled.Settings,
                contentDescription = "服务器设置",
                onClick = { showServerDialog = true },
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .liquidGlassSource(hazeState)
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(Modifier.height(16.dp))
            ProfileCard(session = state.session)

            Spacer(Modifier.height(12.dp))
            StatsCard(stats = stats)

            Spacer(Modifier.height(20.dp))
            GroupLabel("账号")
            SettingsGroupCard {
                MenuRow(
                    title = "我的 ID",
                    subtitle = state.session?.userId ?: "未登录",
                )
                MenuRow(
                    title = "服务器地址",
                    subtitle = state.serverBaseUrl,
                    onClick = { showServerDialog = true },
                )
                MenuRow(
                    title = "IM 连接",
                    subtitle = when (connection) {
                        ChatConnectionState.ONLINE -> "已连接"
                        ChatConnectionState.CONNECTING -> "连接中…"
                        ChatConnectionState.FAILED -> "连接失败 · 检查服务器地址"
                        ChatConnectionState.OFFLINE -> "离线"
                    },
                    subtitleColor = if (connection == ChatConnectionState.ONLINE) OnlineGreen else null,
                )
            }

            Spacer(Modifier.height(20.dp))
            GroupLabel("偏好")
            SettingsGroupCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = "外观",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = state.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(mode.label()) },
                            )
                        }
                    }
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 14.dp),
                )
                MenuRow(title = "语言", subtitle = "简体中文")
            }

            Spacer(Modifier.height(20.dp))
            GroupLabel("关于")
            SettingsGroupCard {
                MenuRow(title = "版本", subtitle = "Single Chat ${BuildConfig.VERSION_NAME}")
                MenuRow(title = "阶段", subtitle = "M1 · 云端连通")
                MenuRow(title = "设计原则", subtitle = "把通道交给 PaaS，把体验留给自己")
            }

            Spacer(Modifier.height(20.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLogoutConfirm = true },
            ) {
                Text(
                    text = "退出登录",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(vertical = 15.dp),
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = "把话说完，把心放轻。",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 110.dp + navBarBottom),
            )
            }

            MainGlassTabBar(
                selectedIndex = 2,
                onSelect = onTabSelect,
                modifier = Modifier.align(Alignment.BottomCenter),
                hazeState = hazeState,
            )
        }
    }

    if (showServerDialog) {
        var serverUrl by remember(state.serverBaseUrl) { mutableStateOf(state.serverBaseUrl) }
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            title = { Text("服务器地址") },
            text = {
                Column {
                    Text("登录、验证码和 UserSig 都经过这个 BFF 地址。留空则使用打包内置地址。")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        placeholder = { Text("https://…") },
                        singleLine = true,
                        shape = RoundedCornerShape(17.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveServerBaseUrl(serverUrl)
                        showServerDialog = false
                    },
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) { Text("取消") }
            },
        )
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("退出登录") },
            text = { Text("退出后聊天记录仍保留在本机，下次登录可继续查看。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        viewModel.logout()
                    },
                ) {
                    Text("退出", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun ProfileCard(session: Session?) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(
                seed = session?.userId ?: "me",
                name = session?.displayName ?: "我",
                size = 56.dp,
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = session?.displayName ?: "未登录",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${session?.phone?.ifBlank { "—" } ?: "—"} · ${session?.userId ?: ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StatsCard(stats: ProfileStats) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatCell(value = stats.contacts, label = "联系人", modifier = Modifier.weight(1f))
            StatDivider()
            StatCell(value = stats.conversations, label = "会话", modifier = Modifier.weight(1f))
            StatDivider()
            StatCell(value = stats.unread, label = "未读", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCell(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (value > 999) "999+" else value.toString(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(0.5.dp)
            .height(30.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary,
        modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsGroupCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column { content() }
    }
}

@Composable
private fun MenuRow(
    title: String,
    subtitle: String,
    subtitleColor: androidx.compose.ui.graphics.Color? = null,
    onClick: (() -> Unit)? = null,
) {
    var rowModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp)
    onClick?.let { rowModifier = rowModifier.then(Modifier.clickable(onClick = it)) }
    Row(
        modifier = rowModifier
            .heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = subtitleColor ?: TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 180.dp),
        )
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "跟随系统"
    ThemeMode.LIGHT -> "浅色"
    ThemeMode.DARK -> "深色"
}
