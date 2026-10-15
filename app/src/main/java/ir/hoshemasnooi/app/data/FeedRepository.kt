package ir.hoshemasnooi.app.data

import android.content.Context
import ir.hoshemasnooi.app.model.Category
import ir.hoshemasnooi.app.model.Post
import ir.hoshemasnooi.app.util.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class FeedRepository(private val context: Context) {

    private val translator = Translator(context)
    private val prefs = context.getSharedPreferences("feed", Context.MODE_PRIVATE)

    /** خوراک JSON راه دور دلخواه (اختیاری). خالی یعنی استفاده از آینه‌های پیش‌فرض. */
    var remoteFeedUrl: String
        get() = prefs.getString(KEY_REMOTE_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_REMOTE_URL, value.trim()).apply()

    /** آدرس‌هایی که برای دریافت خوراک از پیش ترجمه‌شده امتحان می‌شوند. */
    private fun remoteMirrors(): List<String> {
        val custom = remoteFeedUrl.trim()
        return if (custom.isNotEmpty()) listOf(custom) else DEFAULT_FEED_MIRRORS
    }

    var translateEnabled: Boolean
        get() = prefs.getBoolean(KEY_TRANSLATE, true)
        set(value) = prefs.edit().putBoolean(KEY_TRANSLATE, value).apply()

    suspend fun initialLoad(): List<Post> = withContext(Dispatchers.IO) {
        val cached = loadCached()
        val seed = loadSeed()
        merge(cached, seed)
    }

    suspend fun refresh(): RefreshResult = withContext(Dispatchers.IO) {
        val collected = mutableListOf<Post>()
        var fetchedAny = false
        var lastError: String? = null

        // ۱) خوراک از پیش ترجمه‌شده (JSON) از آینه‌ها (اولویت بالا).
        var gotJson = false
        for (mirror in remoteMirrors()) {
            if (mirror.isBlank()) continue
            val json = Http.get(mirror) ?: continue
            val items = Sources.parseJsonFeed(json)
            if (items.isNotEmpty()) {
                fetchedAny = true
                gotJson = true
                items.forEach { raw ->
                    toPost(raw, raw.author, raw.author.ifBlank { "منبع آنلاین" })
                        ?.let { collected.add(it) }
                }
                break
            }
        }
        if (!gotJson) lastError = "دریافت خوراک آنلاین ناموفق بود"

        // ۲) خوراک‌های عمومی RSS/Atom — همیشه همزمان بررسی می‌شوند تا تویت‌های شخصی جدید بیایند.
        for (source in Sources.feedSources) {
            val xml = Http.get(source.url) ?: continue
            val items = Sources.parseFeed(xml)
            if (items.isNotEmpty()) {
                fetchedAny = true
                // حداکثر ۱۵ مطلب تازه از هر منبع برای جلوگیری از شلوغی
                items.take(15).forEach { raw ->
                    toPost(raw, source.name, source.name)?.let { collected.add(it) }
                }
            }
        }

        val existing = merge(loadCached(), loadSeed())
        val merged = merge(existing, collected)
        saveCache(merged)

        RefreshResult(
            added = (merged.size - existing.size).coerceAtLeast(0),
            total = merged.size,
            networkOk = fetchedAny,
            error = lastError
        )
    }

    fun translateIfNeeded(text: String): String =
        if (translateEnabled) translator.translate(text) else text

    fun isPersian(text: String): Boolean = translator.isProbablyPersian(text)

    private fun toPost(raw: RawItem, fallbackAccount: String, source: String): Post? {
        val text = raw.text.trim()
        if (text.length < 25) return null
        if (looksLikeUrl(text)) return null

        val account = raw.author.ifBlank { fallbackAccount }
        val category = if (raw.existingCategory.isNotBlank())
            Category.fromId(raw.existingCategory)
        else Classifier.classify(text)

        val originalIsPersian = translator.isProbablyPersian(text)
        val persian = when {
            raw.existingPersian.isNotBlank() -> raw.existingPersian
            originalIsPersian -> text
            else -> translateIfNeeded(text)
        }

        return Post(
            id = makeId(account, text, raw.url),
            account = account,
            authorName = account,
            category = category,
            textOriginal = text,
            textPersian = persian,
            timestamp = raw.timestamp,
            url = raw.url.ifBlank { searchUrl(text) },
            sourceName = source,
            translatedFrom = !originalIsPersian
        )
    }

    private fun looksLikeUrl(text: String): Boolean {
        val t = text.trim()
        return t.startsWith("http") && !t.contains(" ")
    }

    private fun searchUrl(text: String): String =
        "https://www.google.com/search?q=" + java.net.URLEncoder.encode(text.take(120), "UTF-8")

    private fun makeId(account: String, text: String, url: String): String {
        if (url.isNotBlank()) return url.hashCode().toString()
        return (account + "|" + text.take(120)).hashCode().toString()
    }

    private fun merge(a: List<Post>, b: List<Post>): List<Post> {
        val map = LinkedHashMap<String, Post>()
        for (p in a) map[p.id] = p
        for (p in b) if (!map.containsKey(p.id)) map[p.id] = p
        return map.values.sortedByDescending { it.timestamp }
    }

    private fun loadSeed(): List<Post> = try {
        val raw = context.assets.open("seed_feed.json").bufferedReader().use { it.readText() }
        parsePosts(raw)
    } catch (_: Exception) {
        emptyList()
    }

    private fun loadCached(): List<Post> {
        val raw = prefs.getString(KEY_CACHE, null) ?: return emptyList()
        return parsePosts(raw)
    }

    private fun saveCache(posts: List<Post>) {
        prefs.edit().putString(KEY_CACHE, serializePosts(posts.take(500))).apply()
    }

    private fun parsePosts(json: String): List<Post> = try {
        val arr = JSONArray(json)
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            Post(
                id = o.optString("id"),
                account = o.optString("account"),
                authorName = o.optString("authorName").ifBlank { o.optString("account") },
                category = Category.fromId(o.optString("category")),
                textOriginal = o.optString("textOriginal"),
                textPersian = o.optString("textPersian"),
                timestamp = o.optLong("timestamp"),
                url = o.optString("url"),
                sourceName = o.optString("sourceName"),
                translatedFrom = o.optBoolean("translatedFrom", true)
            )
        }.filter { it.id.isNotBlank() }
    } catch (_: Exception) {
        emptyList()
    }

    private fun serializePosts(posts: List<Post>): String {
        val arr = JSONArray()
        for (p in posts) {
            arr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("account", p.account)
                    put("authorName", p.authorName)
                    put("category", p.category.id)
                    put("textOriginal", p.textOriginal)
                    put("textPersian", p.textPersian)
                    put("timestamp", p.timestamp)
                    put("url", p.url)
                    put("sourceName", p.sourceName)
                    put("translatedFrom", p.translatedFrom)
                }
            )
        }
        return arr.toString()
    }

    companion object {
        /** آینه‌های خوراک از پیش ترجمه‌شده؛ raw، سپس CDN و در پایان GitHub Pages. */
        val DEFAULT_FEED_MIRRORS = listOf(
            "https://raw.githubusercontent.com/ambb1367/hooshemasnooi/master/docs/feed.json",
            "https://cdn.jsdelivr.net/gh/ambb1367/hooshemasnooi@master/docs/feed.json",
            "https://ambb1367.github.io/hooshemasnooi/feed.json"
        )
        private const val KEY_CACHE = "cache_posts_v1"
        private const val KEY_REMOTE_URL = "remote_url"
        private const val KEY_TRANSLATE = "translate_enabled"
    }
}

data class RefreshResult(
    val added: Int,
    val total: Int,
    val networkOk: Boolean,
    val error: String?
)
