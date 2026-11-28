package com.example.locthongbao.ui.screens.blocknotifications

import android.app.Notification
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.locthongbao.data.local.NotificationEntity

import com.example.locthongbao.ui.components.buttons.EdgeDockedActionButton
import com.example.locthongbao.ui.MainViewModel
import com.example.locthongbao.ui.components.AppNotificationGroup
import com.example.locthongbao.ui.components.SwipeableGroupCard
import com.example.locthongbao.ui.components.groupByApp
import com.example.locthongbao.util.getAppIcon
import kotlinx.coroutines.launch

// Màu nền xám đồng bộ cho badge bộ lọc, tên app và thẻ app đang được chọn
private val SharedBadgeGray = Color(0xFFE0E0E0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedNotificationsScreen(
    viewModel: MainViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showConfirmDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    // null = Tất cả; non-null = packageName của app đang được ưu tiên lên đầu
    var selectedPackageFilter by remember { mutableStateOf<String?>(null) }

    val blockedNotifications by viewModel.blockedNotifications.collectAsState()
    val list = blockedNotifications
    val hasItems = !list.isNullOrEmpty()
    val listState = rememberLazyListState()

    val baseGroups = remember(list) { list?.groupByApp().orEmpty() }

    // Nếu app đang lọc không còn trong danh sách (ví dụ vừa bị xoá) thì reset về Tất cả
    LaunchedEffect(baseGroups, selectedPackageFilter) {
        if (selectedPackageFilter != null && baseGroups.none { it.packageName == selectedPackageFilter }) {
            selectedPackageFilter = null
        }
    }

    // Đưa nhóm app được chọn lên đầu; khi chọn "Tất cả" giữ nguyên thứ tự theo thời gian mới nhất
    val displayedGroups = remember(baseGroups, selectedPackageFilter) {
        val targetPkg = selectedPackageFilter ?: return@remember baseGroups
        val (matched, others) = baseGroups.partition { it.packageName == targetPkg }
        matched + others
    }

    val activeFilterLabel = remember(baseGroups, selectedPackageFilter) {
        if (selectedPackageFilter == null) {
            "Tất cả"
        } else {
            baseGroups.find { it.packageName == selectedPackageFilter }?.appName ?: "Tất cả"
        }
    }

    var revealedPackageName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(listState.isScrollInProgress, showFilterDialog, showConfirmDialog) {
        if ((listState.isScrollInProgress || showFilterDialog || showConfirmDialog) && revealedPackageName != null) {
            revealedPackageName = null
        }
    }

    // Hạ nhẹ (20.dp) và lệch phải vừa đủ (14.dp) để không bị cắt viền dưới của khung Dialog
    val rightHandedDialogModifier = Modifier
        .fillMaxWidth()
        .offset(x = 14.dp, y = 20.dp)

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            modifier = rightHandedDialogModifier,
            title = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Xoá tất cả thông báo",
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = { Text("Bạn có chắc muốn xoá toàn bộ thông báo trong danh sách này không?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.clearBlockedNotifications()
                    }
                ) {
                    Text("Xoá")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Huỷ")
                }
            }
        )
    }

    if (showFilterDialog) {
        BlockedAppFilterDialog(
            groups = baseGroups,
            selectedPackageName = selectedPackageFilter,
            modifier = rightHandedDialogModifier,
            onDismiss = { showFilterDialog = false },
            onSelectPackage = { pkg ->
                selectedPackageFilter = pkg
                showFilterDialog = false
                // Đưa nhóm lên đầu và tự cuộn lên đầu danh sách để thấy ngay
                coroutineScope.launch {
                    listState.animateScrollToItem(index = 0)
                }
            }
        )
    }

    // Thêm state lưu nhóm đang mở xem tất cả
    var detailGroup by remember { mutableStateOf<AppNotificationGroup?>(null) }

    // Tự đóng trang chi tiết nếu nhóm bị xoá hết thông báo từ bên ngoài
    LaunchedEffect(baseGroups, detailGroup) {
        val currentPkg = detailGroup?.packageName ?: return@LaunchedEffect
        if (baseGroups.none { it.packageName == currentPkg }) {
            detailGroup = null
        }
    }

    val activeDetailGroup = detailGroup
    if (activeDetailGroup != null) {
        val groupItems = remember(list, activeDetailGroup.packageName) {
            list
                .orEmpty()
                .filter { it.packageName == activeDetailGroup.packageName }
                .sortedByDescending { it.postTime }
        }

        // Lưu ý: dùng NotificationListScreen đã có title, onBack, showAppHeaderOnItem, onDeleteItem
        // (hoặc nếu bạn chưa thêm các tham số đó vào NotificationListScreen thì thêm vào trước)
        com.example.locthongbao.ui.components.NotificationListScreen(
            notifications = groupItems,
            emptyMessage = "Không còn thông báo nào trong nhóm này",
            title = "${activeDetailGroup.appName} (${groupItems.size})",
            onBack = { detailGroup = null },
            showAppHeaderOnItem = false,
            onClearAll = {
                viewModel.deleteBlockedGroup(activeDetailGroup.packageName)
                detailGroup = null
            },
            onDeleteItem = { item ->
                viewModel.deleteNotificationById(item.id)
                if (groupItems.size == 1) {
                    detailGroup = null
                }
            },
            onToggleSpamItem = { item ->
                viewModel.onToggleSpamItem(item)
                if (groupItems.size == 1) {
                    detailGroup = null
                }
            },
            isBlockedScreen = true
        )
        return
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "Đã lọc",
                    //color = Color(0xFF1B5E20)
                )
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                //containerColor = Color(0xFFF8FCF9),
                titleContentColor = Color(0xFFF44336)
            )
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Đã lọc: ${list?.size ?: 0} thông báo",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFFF44336)
                )
                Spacer(modifier = Modifier.height(8.dp))

                when {
                    list == null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    list.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Chưa có thông báo bị chặn",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 112.dp, top = 4.dp)
                        ) {
                            items(
                                items = displayedGroups,
                                key = { group -> group.packageName }
                            ) { group ->
                                SwipeableGroupCard(
                                    group = group,
                                    isRevealed = revealedPackageName == group.packageName,
                                    onRevealChange = { pkg, revealed ->
                                        revealedPackageName = if (revealed) pkg else null
                                    },
                                    containerColor = Color(0xFFFFFAFA),
                                    onViewAll = { selectedGroup ->
                                        revealedPackageName = null
                                        detailGroup = selectedGroup
                                    },
                                    onDeleteGroup = { targetGroup ->
                                        revealedPackageName = null
                                        viewModel.deleteBlockedGroup(targetGroup.packageName)
                                    },
                                   // isBlockedScreen = true
                                )
                            }
                        }
                    }
                }
            }

            // Cụm 2 nút dính sát mép phải: "Lọc ..." ở trên, "Xóa" ở dưới
            if (hasItems) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 12.dp)
                ) {
                    EdgeDockedActionButton(
                        textColor = Color.Black,
                        onClick = { showFilterDialog = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Lọc",
                                color = Color.Black,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )

                            // Chỉ phần tên bộ lọc ($activeFilterLabel) có nền xám bên trong nút
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .background(
                                        color = SharedBadgeGray,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = activeFilterLabel,
                                    color = Color.Black,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    EdgeDockedActionButton(
                        textColor = MaterialTheme.colorScheme.error,
                        onClick = { showConfirmDialog = true }
                    ) {
                        Text(
                            text = "Xóa",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
 private fun BlockedAppFilterDialog(
    groups: List<AppNotificationGroup>,
    selectedPackageName: String?,
    onDismiss: () -> Unit,
    onSelectPackage: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val totalCount = remember(groups) { groups.sumOf { it.count } }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier.heightIn(max = 420.dp),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp)
            ) {
                Text(
                    text = "Lọc theo ứng dụng",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Thẻ "Tất cả"
                    item(key = "filter_all") {
                        val isAllSelected = selectedPackageName == null
                        BlockedFilterAppChipCard(
                            appName = "Tất cả",
                            count = totalCount,
                            isSelected = isAllSelected,
                            leadingContent = null,
                            onClick = { onSelectPackage(null) }
                        )
                    }

                    // Các thẻ ứng dụng đã có thông báo bị chặn
                    items(
                        items = groups,
                        key = { it.packageName }
                    ) { group ->
                        val isSelected = selectedPackageName == group.packageName
                        val iconBitmap = remember(group.packageName) {
                            getAppIcon(group.packageName, context.packageManager)
                        }

                        BlockedFilterAppChipCard(
                            appName = group.appName,
                            count = group.count,
                            isSelected = isSelected,
                            onClick = { onSelectPackage(group.packageName) },
                            leadingContent = {
                                if (iconBitmap != null) {
                                    Image(
                                        bitmap = iconBitmap,
                                        contentDescription = group.appName,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Surface(
                                        modifier = Modifier.size(20.dp),
                                        shape = RoundedCornerShape(5.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = group.appName.take(1).uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Nút Đóng
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Đóng")
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockedFilterAppChipCard(
    appName: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    leadingContent: (@Composable () -> Unit)? = null
) {
    val cardColor = if (isSelected) {
        SharedBadgeGray
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = cardColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (leadingContent != null) {
                leadingContent()
            }

            Text(
                text = appName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "$count thông báo",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}