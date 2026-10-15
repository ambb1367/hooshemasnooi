package ir.hoshemasnooi.app.data

import android.content.Context
import ir.hoshemasnooi.app.model.Category
import ir.hoshemasnooi.app.model.Post
import ir.hoshemasnooi.app.util.Http
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * منایع دریافت مطالب.
 *
 * چون دسترسی رسمی به رابط برنامه‌نویسی توییتر (X) نیازمند کلید ویژه است،
 * برنامه از دو مسیر استفاده می‌کند:
 *   1) خوراک‌های RSS مبتنی بر Nitter برای حساب‌های تخصصی هوش مصنوعی.
 *   2) یک فایل JSON راه دور (قابل تنظیم) که می‌توان آن را روی GitHub میزبانی کرد.
 *
 * همه این‌ها با داده‌های ذخیره‌شده در برنامه ترکیب می‌شوند تا حتی بدون اینترنت نیز
 * برنامه پر و کاربردی باشد.
 */
object Sources {

    /** نمونه‌های جایگزین Nitter برای افزایش پایداری. */
    val nitterInstances = listOf(
        "https://nitter.net",
        "https://nitter.privacydev.net",
        "https://nitter.poast.org",
        "https://nitter.1d4.us",
        "https://nitter.kavin.rocks"
    )

    /** حساب‌های تخصصی هوش مصنوعی. */
    val accounts = listOf(
        "OpenAI", "GoogleDeepMind", "AIatMeta", "AnthropicAI", "huggingface",
        "GoogleAI", "NVIDIA", "Microsoft", "karpathy", "sama",
        "SundarPichai", "satyanadella", "lexfridman", "Midjourney",
        "StabilityAI", "arXiv_Daily", "MIT_CSAIL", "StanfordAILab",
        "allen_ai", "AI_Frontier", "TheAIObserver", "AIatGoogle"
    )

    private val dateFormats = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        "EEE, dd MMM yyyy HH:mm Z"
    )

    fun accountFeed(instance: String, account: String): String = "$instance/$account/rss"

    fun searchFeed(instance: String, query: String): String =
        "$instance/search/rss?f=tweets&q=$query"

    /**
     * تجزیه خوراک RSS و تبدیل آن به فهرست مطلب خام.
     * @return جفت نام حساب و متن مطلب
     */
    fun parseRss(xml: String): List<RawItem> {
        val items = mutableListOf<RawItem>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var inItem = false
            var title = ""
            var description = ""
            var link = ""
            var pubDate = ""
            var creator = ""
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                val name = parser.name?.lowercase(Locale.ROOT)
                when (event) {
                    XmlPullParser.START_TAG -> when (name) {
                        "item" -> {
                            inItem = true; title = ""; description = ""; link = ""
                            pubDate = ""; creator = ""
                        }
                        "title" -> if (inItem) title = parser.nextTextSafe()
                        "description" -> if (inItem) description = parser.nextTextSafe()
                        "link" -> if (inItem) link = parser.nextTextSafe()
                        "pubdate" -> if (inItem) pubDate = parser.nextTextSafe()
                        "creator", "dc:creator" -> if (inItem) creator = parser.nextTextSafe()
                    }
                    XmlPullParser.END_TAG -> if (name == "item") {
                        inItem = false
                        val text = cleanHtml(description).ifBlank { title }
                        if (text.isNotBlank()) {
                            items.add(
                                RawItem(
                                    author = creator.removePrefix("@").trim(),
                                    text = text,
                                    url = link,
                                    timestamp = parseDate(pubDate)
                                )
                            )
                        }
                    }
                }
                event = parser.next()
            }
        } catch (_: Exception) {
        }
        return items
    }

    fun parseJsonFeed(json: String): List<RawItem> {
        val items = mutableListOf<RawItem>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val text = obj.optString("text").ifBlank { obj.optString("textPersian") }
                if (text.isBlank()) continue
                items.add(
                    RawItem(
                        author = obj.optString("account"),
                        text = obj.optString("textOriginal").ifBlank { text },
                        url = obj.optString("url"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        existingPersian = obj.optString("textPersian"),
                        existingCategory = obj.optString("category")
                    )
                )
            }
        } catch (_: Exception) {
        }
        return items
    }

    private fun XmlPullParser.nextTextSafe(): String = try {
        nextText().trim()
    } catch (_: Exception) {
        ""
    }

    private fun parseDate(raw: String): Long {
        if (raw.isBlank()) return System.currentTimeMillis()
        for (fmt in dateFormats) {
            try {
                return SimpleDateFormat(fmt, Locale.US).parse(raw)?.time ?: continue
            } catch (_: Exception) {
            }
        }
        return System.currentTimeMillis()
    }

    fun cleanHtml(html: String): String {
        var s = html
        s = s.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        s = s.replace(Regex("<[^>]+>"), "")
        s = s.replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'")
            .replace("&nbsp;", " ").replace("&amp;", "&")
        return s.trim()
    }
}

data class RawItem(
    val author: String,
    val text: String,
    val url: String,
    val timestamp: Long,
    val existingPersian: String = "",
    val existingCategory: String = ""
)
