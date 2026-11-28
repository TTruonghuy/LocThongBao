package com.example.locthongbao.domain.model

import android.app.Notification
import android.content.Context
import com.example.locthongbao.R
import com.example.locthongbao.data.local.NotificationEntity
import java.text.Normalizer

enum class FilterCategory { IGNORED, SPAM, PASSED }

data class FilterResult(val category: FilterCategory, val reason: String)

private val MARKS = Regex("\\p{Mn}+")

// Bỏ dấu, chữ thường: dùng cho luật bảo vệ và cụm nhiều từ
fun String.normalizeVi(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD)
        .replace(MARKS, "")
        .replace('đ', 'd')
        .replace(Regex("\\s+"), " ")
        .trim()

// Giữ dấu (NFC), chữ thường: dùng cho từ khóa đơn, tránh nhầm "giá"/"giác", "mưa"/"mua"
private fun String.canon(): String =
    Normalizer.normalize(this, Normalizer.Form.NFC).lowercase()

private const val DECISIVE = 4                       // trọng số >= 4 là rác luôn
private const val B_START = "(?<![\\p{L}\\p{N}])"    // ranh giới từ theo chữ cái Unicode
private const val B_END = "(?![\\p{L}\\p{N}])"

private class Kw(val raw: String, val weight: Int, val group: String) {
    private val exact = Regex(B_START + Regex.escape(raw.canon()) + B_END)
    // Cụm từ 2 chữ trở lên: so khớp thêm trên bản bỏ dấu (tin nhắn không dấu)
    private val loose: Regex? =
        if (raw.trim().contains(' '))
            Regex(B_START + Regex.escape(raw.normalizeVi()) + B_END) else null

    fun matches(canonText: String, looseText: String): Boolean =
        exact.containsMatchIn(canonText) || loose?.containsMatchIn(looseText) == true
}

private class Rules(val hard: List<Kw>, val soft: List<Kw>, val spam: List<Kw>)

// Mỗi dòng: "từ khóa" hoặc "từ khóa|trọng số"
private fun Context.kws(id: Int, group: String): List<Kw> =
    resources.getStringArray(id).mapNotNull { line ->
        val parts = line.split('|')
        val word = parts[0].trim()
        if (word.isEmpty()) null
        else Kw(word, parts.getOrNull(1)?.trim()?.toIntOrNull() ?: DECISIVE, group)
    }

object NotificationFilter {

    private val IGNORED_PACKAGES = setOf("android", "com.android.systemui")
    private val AD_SOURCES = setOf(
        "com.miui.msa.global", "com.miui.systemadsolution", "com.xiaomi.mipicks"
    )
    private val PROTECTED_CATEGORIES = setOf(
        Notification.CATEGORY_CALL, Notification.CATEGORY_MESSAGE, Notification.CATEGORY_EMAIL,
        Notification.CATEGORY_ALARM, Notification.CATEGORY_REMINDER, Notification.CATEGORY_EVENT,
        Notification.CATEGORY_NAVIGATION, Notification.CATEGORY_TRANSPORT
    )
    private val SIZE_PROGRESS =
        Regex("\\d+([.,]\\d+)?\\s?[kmg]b?\\s*/\\s*\\d+([.,]\\d+)?\\s?[kmg]b?")

    // ----- Mã bảo mật: cần "tín hiệu" VÀ "hình dạng mã" (xem lượt trước) -----
    private val CODE_CUES = Regex(
        "\\botp\\b|ma xac (thuc|minh|nhan)|\\bxac (thuc|minh)\\b|ma dang nhap|ma bao mat|" +
                "mat khau|verification|\\bverify\\b|\\bpin\\b|khong chia se|tuyet doi khong|" +
                "het han sau|hieu luc trong"
    )
    private val DIGIT_CODE =
        Regex("(?<![\\d.,])\\d{4,8}(?!\\d|[.,]\\d|\\s?%|\\s?(?:d|vnd|k|tr|dong)\\b)")
    private val ALNUM_CODE = Regex("\\b(?=[A-Z0-9]*\\d)(?=[A-Z0-9]*[A-Z])[A-Z0-9]{5,10}\\b")
    private val LETTER_CODE = Regex("\\b[A-Z]{4,8}\\b")

    private fun securityCodeReason(original: String, loose: String): String? {
        val cue = CODE_CUES.find(loose)?.value ?: return null
        if (cue == "otp") return "OTP"
        val hasToken = DIGIT_CODE.containsMatchIn(loose) ||
                ALNUM_CODE.containsMatchIn(original) ||
                LETTER_CODE.containsMatchIn(original)
        return if (hasToken) "mã bảo mật ($cue)" else null
    }

    @Volatile private var cached: Rules? = null

    private fun rules(ctx: Context): Rules = cached ?: synchronized(this) {
        cached ?: Rules(
            hard = ctx.kws(R.array.whitelist_hard, "An toàn"),
            soft = ctx.kws(R.array.whitelist_soft, "An toàn mềm"),
            spam = listOf(
                R.array.blacklist_explicit to "Thẻ quảng cáo",
                R.array.blacklist_ecommerce to "TMĐT / Khuyến mãi",
                R.array.blacklist_finance to "Tài chính / Vay",
                R.array.blacklist_telecom to "Viễn thông",
                R.array.blacklist_urgency to "Thôi thúc / FOMO",
                R.array.blacklist_social to "Mạng xã hội"
            ).flatMap { (id, group) -> ctx.kws(id, group) }
        ).also { cached = it }
    }

    fun isAdSource(pkg: String?): Boolean = pkg?.trim()?.lowercase() in AD_SOURCES

    private fun ignoreReason(i: NotificationEntity): String? = when {
        i.isOngoing -> "Chạy ngầm"
        (i.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0 -> "Foreground service"
        i.isGroupSummary -> "Thông báo tổng"
        i.packageName in IGNORED_PACKAGES -> "Hệ thống"
        i.title.isBlank() && i.text.isBlank() -> "Trống"
        SIZE_PROGRESS.containsMatchIn(i.text.normalizeVi()) -> "Tiến trình tải"
        else -> null
    }

    fun evaluate(context: Context, item: NotificationEntity): FilterResult {
        return try {
            if (item.isManualOverride) {
                val category = if (item.isFiltered) FilterCategory.SPAM else FilterCategory.PASSED
                val reason = item.filterReason ?: if (item.isFiltered) {
                    "Người dùng đánh dấu: Rác"
                } else {
                    "Người dùng đánh dấu: Bình thường"
                }
                return FilterResult(category, reason)
            }
            // 1) Rác cấu trúc
            ignoreReason(item)?.let { return FilterResult(FilterCategory.IGNORED, it) }

            val original = "${item.title} ${item.text}"
            val canon = original.canon()
            val loose = original.normalizeVi()
            val r = rules(context)

            // 2) AN TOÀN CỨNG: luôn thắng
            if (item.category in PROTECTED_CATEGORIES)
                return FilterResult(FilterCategory.PASSED, "Bảo vệ: category=${item.category}")
            securityCodeReason(original, loose)?.let {
                return FilterResult(FilterCategory.PASSED, "Bảo vệ: $it")
            }
            r.hard.firstOrNull { it.matches(canon, loose) }?.let {
                return FilterResult(FilterCategory.PASSED, "An toàn: khớp \"${it.raw}\"")
            }

            // 3) RÁC: từ quyết định (trọng số >= 4) thắng luôn cả từ an toàn mềm
            val hits = r.spam.filter { it.matches(canon, loose) }.distinctBy { it.raw }
            hits.firstOrNull { it.weight >= DECISIVE }?.let {
                return FilterResult(FilterCategory.SPAM, "Rác (${it.group}): khớp \"${it.raw}\"")
            }

            // 4) CHẤM ĐIỂM: từ yếu cộng, từ an toàn mềm trừ 1 mỗi từ
            val softHits = r.soft.filter { it.matches(canon, loose) }.distinctBy { it.raw }
            val score = hits.sumOf { it.weight } - softHits.size
            if (score >= DECISIVE) {
                val detail = hits.joinToString("+") { "${it.raw}(${it.weight})" }
                return FilterResult(FilterCategory.SPAM, "Rác: điểm $score [$detail]")
            }

            val note = if (hits.isNotEmpty() || softHits.isNotEmpty())
                " (điểm $score)" else ""
            FilterResult(FilterCategory.PASSED, "Bình thường$note")
        } catch (e: Exception) {
            FilterResult(FilterCategory.PASSED, "Lỗi bộ lọc (cho qua)")
        }
    }
}