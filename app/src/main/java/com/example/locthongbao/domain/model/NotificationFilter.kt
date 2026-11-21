package com.example.locthongbao.domain.model

import android.app.Notification
import android.content.Context
import com.example.locthongbao.R
import com.example.locthongbao.data.local.NotificationEntity
import java.text.Normalizer
import kotlin.collections.flatten

enum class FilterCategory {
    IGNORED,
    SPAM,
    PASSED
}
data class FilterResult(
    val category: FilterCategory,
    val reason: String
)

// Bỏ dấu + chữ thường: "Khuyến mãi" -> "khuyen mai"
private val MARKS = Regex("\\p{Mn}+")
fun String.normalizeVi(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD)
        .replace(MARKS, "")
        .replace('đ', 'd')

private val IGNORED_PACKAGES = setOf("android", "com.android.systemui")
private val SIZE_PROGRESS =
    Regex("\\d+([.,]\\d+)?\\s?[kmg]b?\\s*/\\s*\\d+([.,]\\d+)?\\s?[kmg]b?")

private fun ignoreReason(i: NotificationEntity): String? = when {
    i.isOngoing -> "Chạy ngầm"
    (i.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0 -> "Foreground service"
    i.isGroupSummary -> "Thông báo tổng"
    i.packageName in IGNORED_PACKAGES -> "Hệ thống"
    i.title.isBlank() && i.text.isBlank() -> "Trống"
    SIZE_PROGRESS.containsMatchIn(i.text.normalizeVi()) -> "Tiến trình tải"
    else -> null
}
object NotificationFilter {
    fun evaluate(context: Context, item: NotificationEntity): FilterResult {
        return try {
            val titleStr = item.title ?: ""
            val textStr = item.text ?: ""
            val combinedText = "$titleStr $textStr".lowercase()

            // 1. Nhóm IGNORED (Màu 1): Chặn cứng thông báo hệ thống, tiến trình ngầm, trống
            if (item.isOngoing) return FilterResult(FilterCategory.IGNORED, "Chạy ngầm")
            if (item.packageName == "android" || item.packageName.startsWith("com.android")) {
                return FilterResult(FilterCategory.IGNORED, "Hệ thống")
            }
            if (titleStr.isBlank() && textStr.isBlank()) {
                return FilterResult(FilterCategory.IGNORED, "Trống")
            }
            if (item.isGroupSummary) return FilterResult(FilterCategory.IGNORED, "Thông báo tổng")



            // 2. Lớp Whitelist (An toàn tuyệt đối) -> Xếp vào PASSED (Màu 3)
            val safeKeywords = context.resources.getStringArray(R.array.whitelist_keywords)
            if (safeKeywords.any { combinedText.contains(it) }) {
                return FilterResult(FilterCategory.PASSED, "An toàn (Whitelist)")
            }

            // 3. Nhóm SPAM (Màu 2): Dính từ khóa rác / quảng cáo (Blacklist)
            val allBlacklists = arrayOf(
                context.resources.getStringArray(R.array.blacklist_explicit),
                context.resources.getStringArray(R.array.blacklist_ecommerce),
                context.resources.getStringArray(R.array.blacklist_finance),
                context.resources.getStringArray(R.array.blacklist_telecom),
                context.resources.getStringArray(R.array.blacklist_urgency),
                context.resources.getStringArray(R.array.blacklist_social)
            ).flatten()

            if (allBlacklists.any { combinedText.contains(it) }) {
                return FilterResult(FilterCategory.SPAM, "Quảng cáo / Rác")
            }

            // 4. Mặc định bình thường -> Xếp vào PASSED (Màu 3)
            FilterResult(FilterCategory.PASSED, "Bình thường")

        } catch (e: Exception) {
            // Nếu có lỗi, cho qua để không sập app
            FilterResult(FilterCategory.PASSED, "Lỗi bộ lọc (Cho phép qua)")
        }
    }
}


