package ir.hoshemasnooi.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Calendar
import java.util.concurrent.TimeUnit

object Format {

    fun relativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        if (diff < 0) return "همین حالا"
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        return when {
            minutes < 1 -> "همین حالا"
            minutes < 60 -> "$minutes دقیقه پیش"
            hours < 24 -> "$hours ساعت پیش"
            days < 30 -> "$days روز پیش"
            else -> {
                val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                val y = cal.get(Calendar.YEAR)
                val m = cal.get(Calendar.MONTH) + 1
                val d = cal.get(Calendar.DAY_OF_MONTH)
                formatJalaliApprox(y, m, d)
            }
        }
    }

    /** نمایش ساده تاریخ میلادی؛ تبدیل دقیق شمسی در نسخه‌های بعدی. */
    private fun formatJalaliApprox(y: Int, m: Int, d: Int): String =
        String.format("%04d/%02d/%02d", y, m, d)
}

object Intents {
    fun openUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun share(context: Context, text: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری"))
        } catch (_: Exception) {
        }
    }

    fun copy(context: Context, text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("متن", text))
        } catch (_: Exception) {
        }
    }
}
