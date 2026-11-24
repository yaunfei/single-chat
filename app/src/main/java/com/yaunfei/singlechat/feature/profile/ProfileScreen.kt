package com.yaunfei.singlechat.feature.profile

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yaunfei.singlechat.core.designsystem.component.Avatar
import com.yaunfei.singlechat.core.designsystem.component.GlassIconButton
import com.yaunfei.singlechat.core.designsystem.component.MainGlassTabBar
import com.yaunfei.singlechat.core.designsystem.component.liquidGlassSource
import com.yaunfei.singlechat.core.designsystem.component.rememberLiquidGlass
import com.yaunfei.singlechat.core.designsystem.theme.BgGrouped
import com.yaunfei.singlechat.core.designsystem.theme.Separator
import com.yaunfei.singlechat.core.designsystem.theme.TextSecondary

@Composable
fun ProfileScreen(
    onTabSelect: (Int) -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val hazeState = rememberLiquidGlass()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGrouped)
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
                text = "我的",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.weight(1f))
            GlassIconButton(
                icon = Icons.Filled.Settings,
                contentDescription = "设置",
                onClick = { },
                hazeState = hazeState,
            )
        }

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
                // 资料卡
                item {
                    ProfileCard(
                        avatarSeed = state.profile?.id ?: "me",
                        name = state.profile?.name ?: "未命名",
                        bio = state.profile?.bio ?: "这个人很懒，什么都没留下",
                    )
                }

                // 统计卡
                item {
                    StatsCard(
                        friends = state.stats?.friends ?: 0,
                        groups = state.stats?.groups ?: 0,
                        moments = state.stats?.moments ?: 0,
                    )
                }

                // 账号 section
                item { SectionTitle(text = "账号") }
                item {
                    MenuCard {
                        MenuItem(
                            icon = "聊",
                            title = "账号信息",
                            subtitle = state.profile?.id ?: "",
                        )
                        MenuItem(
                            icon = "锁",
                            title = "隐私设置",
                        )
                        MenuItem(
                            icon = "盾",
                            title = "账号安全",
                        )
                    }
                }

                // 偏好 section
                item { SectionTitle(text = "偏好") }
                item {
                    MenuCard {
                        MenuItem(
                            icon = "铃",
                            title = "通知与声音",
                        )
                        MenuItem(
                            icon = "夜",
                            title = "深色外观",
                            trailing = "跟随系统",
                        )
                        MenuItem(
                            icon = "言",
                            title = "语言",
                            trailing = "简体中文",
                        )
                    }
                }

                // 关于 section
                item { SectionTitle(text = "关于") }
                item {
                    MenuCard {
                        MenuItem(
                            icon = "?",
                            title = "帮助与反馈",
                        )
                        MenuItem(
                            icon = "ⓘ",
                            title = "关于 SingleChat",
                            trailing = "0.7.0",
                        )
                    }
                }

                // 退出按钮
                item {
                    Spacer(Modifier.height(24.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        onClick = onLogout,
                    ) {
                        Text(
                            text = "退出登录",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            MainGlassTabBar(
                selectedIndex = 2,
                onSelect = onTabSelect,
                modifier = Modifier.align(Alignment.BottomCenter),
                hazeState = hazeState,
            )
        }
    }
}

@Composable
private fun ProfileCard(
    avatarSeed: String,
    name: String,
    bio: String,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(seed = avatarSeed, name = name, size = 64.dp)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun StatsCard(
    friends: Int,
    groups: Int,
    moments: Int,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
        ) {
            StatItem(value = friends, label = "好友", modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .width(0.5.dp)
                    .background(Separator),
            )
            StatItem(value = groups, label = "群聊", modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .width(0.5.dp)
                    .background(Separator),
            )
            StatItem(value = moments, label = "动态", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatItem(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
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
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary,
        modifier = Modifier.padding(start = 32.dp, end = 16.dp, top = 16.dp, bottom = 6.dp),
    )
}

@Composable
private fun MenuCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        content()
    }
}

@Composable
private fun MenuItem(
    icon: String,
    title: String,
    subtitle: String? = null,
    trailing: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = icon,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1,
                )
            }
        }
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        } else {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
    HorizontalDivider(
        color = Separator,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 60.dp),
    )
}
