package com.example.locthongbao.domain.model

import android.content.Context
import com.example.locthongbao.R
import com.example.locthongbao.data.local.NotificationEntity
import java.text.Normalizer

enum class FilterCategory {
    IGNORED,
    SPAM,
    PASSED
}

data class FilterResult(
    val category: FilterCategory,
    val reason: String
)

private val MARKS = Regex("\\p{Mn}+")

fun String.normalizeVi(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD)
        .replace(MARKS, "")
        .replace('đ', 'd')
        .replace(Regex("\\s+"), " ")
        .trim()

object NotificationFilter {

    fun evaluate(context: Context, item: NotificationEntity): FilterResult {
        return try {
            val titleRaw = item.title.orEmpty()
            val textRaw = item.text.orEmpty()

            // 1. Nhóm IGNORED: thông báo hệ thống, chạy ngầm, thông báo nhóm, rỗng nội dung
            if (item.isOngoing) {
                return FilterResult(FilterCategory.IGNORED, "Chạy ngầm")
            }
            if (item.packageName == "android" || item.packageName.startsWith("com.android")) {
                return FilterResult(FilterCategory.IGNORED, "Hệ thống")
            }
            if (titleRaw.isBlank() && textRaw.isBlank()) {
                return FilterResult(FilterCategory.IGNORED, "Trống")
            }
            if (item.isGroupSummary) {
                return FilterResult(FilterCategory.IGNORED, "Thông báo tổng")
            }

            val normalizedContent = "$titleRaw $textRaw".normalizeVi()

            // 2. Lớp Whitelist: ưu tiên giữ an toàn thông báo quan trọng (OTP, giao dịch, đơn hàng)
            val safeKeywords = context.resources.getStringArray(R.array.whitelist_keywords)
            val matchedSafe = safeKeywords.any { kw ->
                val nKw = kw.normalizeVi()
                nKw.isNotEmpty() && normalizedContent.contains(nKw)
            }
            if (matchedSafe) {
                return FilterResult(FilterCategory.PASSED, "An toàn (Whitelist)")
            }

            // 3. Nhóm SPAM: đối chiếu các nhóm từ khóa quảng cáo / rác
            val blacklistGroups = listOf(
                "Thẻ quảng cáo" to R.array.blacklist_explicit,
                "TMĐT / Khuyến mãi" to R.array.blacklist_ecommerce,
                "Tài chính / Vay" to R.array.blacklist_finance,
                "Viễn thông" to R.array.blacklist_telecom,
                "Thôi thúc / FOMO" to R.array.blacklist_urgency,
                "Mạng xã hội" to R.array.blacklist_social
            )

            for ((groupName, arrayResId) in blacklistGroups) {
                val keywords = context.resources.getStringArray(arrayResId)
                val matched = keywords.any { kw ->
                    val nKw = kw.normalizeVi()
                    nKw.isNotEmpty() && normalizedContent.contains(nKw)
                }
                if (matched) {
                    return FilterResult(FilterCategory.SPAM, "Quảng cáo / Rác ($groupName)")
                }
            }

            // 4. Mặc định hợp lệ
            FilterResult(FilterCategory.PASSED, "Bình thường")
        } catch (_: Exception) {
            FilterResult(FilterCategory.PASSED, "Lỗi bộ lọc (Cho phép qua)")
        }
    }
}