package ir.hoshemasnooi.app.model

/**
 * یک مطلب (توییت) هوش مصنوعی.
 *
 * @param id شناسه یکتا (بر پایه لینک یا حساب و زمان)
 * @param account نام کاربری توییتری (بدون @)
 * @param authorName نام نمایشی
 * @param category دسته‌بندی موضوعی
 * @param textOriginal متن اصلی (معمولاً انگلیسی)
 * @param textPersian ترجمه فارسی
 * @param timestamp زمان انتشار به میلی‌ثانیه
 * @param url لینک مطلب در توییتر (X)
 * @param sourceName نام منبع (مثلاً Nitter یا دیتاست داخلی)
 * @param translatedFrom آیا ترجمه از متن اصلی انجام شده است
 */
data class Post(
    val id: String,
    val account: String,
    val authorName: String,
    val category: Category,
    val textOriginal: String,
    val textPersian: String,
    val timestamp: Long,
    val url: String,
    val sourceName: String,
    val translatedFrom: Boolean = true
) {
    val displayText: String
        get() = textPersian.ifBlank { textOriginal }

    val isPersianOriginal: Boolean
        get() = !translatedFrom
}
