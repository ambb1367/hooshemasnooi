package ir.hoshemasnooi.app.data

import org.json.JSONArray
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * منابع دریافت مطالب.
 *
 * مسیر اصلی: یک خوراک JSON از پیش ترجمه‌شده که به‌صورت خودکار توسط گردش‌کار
 * GitHub ساخته می‌شود (نگاه کنید به tools/aggregate.py). این خوراک روی چند آینه
 * میزبانی می‌شود تا در شبکه‌های محدود نیز در دسترس باشد.
 *
 * مسیر پشتیبان: خوراک‌های RSS/Atom عمومی که روی خود دستگاه تجزیه و ترجمه می‌شوند.
 */
object Sources {

    data class FeedSource(val name: String, val url: String)

    /** خوراک‌های عمومی پشتیبان (RSS/Atom). شامل منابع رسمی و افراد حقیقی مرتبط با هوش مصنوعی. */
    val feedSources = listOf(
        FeedSource(
            "Google News",
            "https://news.google.com/rss/search?q=artificial+intelligence+OR+AI+model+OR+LLM+OR+agentic+AI&hl=en-US&gl=US&ceid=US:en"
        ),
        FeedSource("Hugging Face", "https://huggingface.co/blog/feed.xml"),
        FeedSource("TechCrunch", "https://techcrunch.com/category/artificial-intelligence/feed/"),
        FeedSource("The Verge", "https://www.theverge.com/rss/ai-artificial-intelligence/index.xml"),
        FeedSource("OpenAI", "https://openai.com/news/rss.xml"),
        FeedSource("arXiv", "https://export.arxiv.org/rss/cs.AI"),
        FeedSource("Hacker News", "https://news.ycombinator.com/rss"),
        FeedSource("Karpathy (X)", "https://news.google.com/rss/search?q=site:x.com+Andrej+Karpathy+AI+OR+LLM&hl=en-US&gl=US&ceid=US:en"),
        FeedSource("Yann LeCun (X)", "https://news.google.com/rss/search?q=site:x.com+Yann+LeCun+AI&hl=en-US&gl=US&ceid=US:en"),
        FeedSource("Andrew Ng (X)", "https://news.google.com/rss/search?q=site:x.com+Andrew+Ng+AI&hl=en-US&gl=US&ceid=US:en"),
        FeedSource("Simon Willison (X)", "https://news.google.com/rss/search?q=site:x.com+simonwillison+AI+OR+LLM&hl=en-US&gl=US&ceid=US:en"),
        FeedSource("Ethan Mollick (X)", "https://news.google.com/rss/search?q=site:x.com+Ethan+Mollick+AI&hl=en-US&gl=US&ceid=US:en"),
        FeedSource("Sebastian Raschka (X)", "https://news.google.com/rss/search?q=site:x.com+Sebastian+Raschka+AI&hl=en-US&gl=US&ceid=US:en")
    )

    private val dateFormats = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        "EEE, dd MMM yyyy HH:mm Z"
    )

    /**
     * تجزیه خوراک RSS یا Atom و تبدیل آن به فهرست مطلب خام.
     */
    fun parseFeed(xml: String): List<RawItem> {
        val items = mutableListOf<RawItem>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var inItem = false
            var title = ""
            var body = ""
            var link = ""
            var pubDate = ""
            var creator = ""
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                val name = parser.name?.lowercase(Locale.ROOT)
                when (event) {
                    XmlPullParser.START_TAG -> when (name) {
                        "item", "entry" -> {
                            inItem = true; title = ""; body = ""; link = ""
                            pubDate = ""; creator = ""
                        }
                        "title" -> if (inItem) title = parser.nextTextSafe()
                        "description", "summary", "content" ->
                            if (inItem && body.isBlank()) body = parser.nextTextSafe()
                        "link" -> if (inItem) {
                            val href = parser.getAttributeValue(null, "href")
                            if (!href.isNullOrBlank()) {
                                if (link.isBlank()) link = href.trim()
                            } else {
                                val t = parser.nextTextSafe()
                                if (t.isNotBlank()) link = t
                            }
                        }
                        "pubdate", "published", "updated" ->
                            if (inItem && pubDate.isBlank()) pubDate = parser.nextTextSafe()
                        "creator" -> if (inItem) creator = parser.nextTextSafe()
                        "source" -> if (inItem && creator.isBlank()) creator = parser.nextTextSafe()
                    }
                    XmlPullParser.END_TAG -> if (name == "item" || name == "entry") {
                        inItem = false
                        val text = cleanHtml(body).ifBlank { title }
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
                val text = obj.optString("textOriginal").ifBlank { obj.optString("textPersian") }
                if (text.isBlank()) continue
                items.add(
                    RawItem(
                        author = obj.optString("account").ifBlank { obj.optString("authorName") },
                        text = text,
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
