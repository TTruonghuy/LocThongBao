package com.example.locthongbao.ui.components.buttons

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.locthongbao.R
import com.example.locthongbao.ui.screens.applist.AppListScreen
import com.example.locthongbao.ui.screens.blocknotifications.BlockedNotificationsScreen
import com.example.locthongbao.ui.screens.notifications.MainDashboardScreen
import com.example.locthongbao.ui.screens.settings.SettingsScreen

enum class BottomTab(val title: String) {
    NOTIFICATION("Thông báo"),
    BLOCK_NOTIFICATION("Đã chặn"),
    APP_LIST("Ứng dụng"),
    SETTINGS("Cài đặt")
}

@Composable
fun AnimatedBottomNavItem(
    label: String,
    @DrawableRes selectedIconRes: Int,
    @DrawableRes unselectedIconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    baseIconSize: Dp = 24.dp,
    offsetYSelected: Dp = (-3).dp

) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "IconScale"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
        animationSpec = tween(durationMillis = 300),
        label = "IconColor"
    )

    // Trượt lên nhẹ có độ nảy khi được chọn, về 0.dp khi bỏ chọn
    val offsetY by animateDpAsState(
        targetValue = if (isSelected) offsetYSelected else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "IconOffsetY"
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = if (isSelected) selectedIconRes else unselectedIconRes),
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier
                .size(baseIconSize)
                .scale(scale)
                .offset(y = offsetY)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun MainAppContainer() {
    var currentTab by remember { mutableStateOf(BottomTab.NOTIFICATION) }

    Scaffold(
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(

                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .height(64.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    AnimatedBottomNavItem(
                    label = BottomTab.APP_LIST.title,
                    selectedIconRes = R.drawable.ic_applist,
                    unselectedIconRes = R.drawable.ic_applist,
                    isSelected = currentTab == BottomTab.APP_LIST,
                    onClick = { currentTab = BottomTab.APP_LIST },
                    modifier = Modifier.weight(1f)
                )
                    AnimatedBottomNavItem(
                        label = BottomTab.NOTIFICATION.title,
                        selectedIconRes = R.drawable.ic_notification_list,
                        unselectedIconRes = R.drawable.ic_notification_list,
                        isSelected = currentTab == BottomTab.NOTIFICATION,
                        onClick = { currentTab = BottomTab.NOTIFICATION },
                        modifier = Modifier.weight(1f)
                    )
                    AnimatedBottomNavItem(
                        label = BottomTab.BLOCK_NOTIFICATION.title,
                        selectedIconRes = R.drawable.ic_blockednotifications_list,
                        unselectedIconRes = R.drawable.ic_blockednotifications_list,
                        isSelected = currentTab == BottomTab.BLOCK_NOTIFICATION,
                        onClick = { currentTab = BottomTab.BLOCK_NOTIFICATION },
                        baseIconSize = 27.dp,
                        modifier = Modifier.weight(1f)

                    )

                    AnimatedBottomNavItem(
                        label = BottomTab.SETTINGS.title,
                        selectedIconRes = R.drawable.ic_settings,
                        unselectedIconRes = R.drawable.ic_settings,
                        isSelected = currentTab == BottomTab.SETTINGS,
                        onClick = { currentTab = BottomTab.SETTINGS },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                BottomTab.NOTIFICATION -> MainDashboardScreen()
                BottomTab.BLOCK_NOTIFICATION -> BlockedNotificationsScreen()
                BottomTab.APP_LIST -> AppListScreen()
                BottomTab.SETTINGS -> SettingsScreen()
            }
        }
    }
}