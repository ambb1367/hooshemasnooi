package ir.hoshemasnooi.app.data

import android.content.Context
import ir.hoshemasnooi.app.util.Http
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * مترجم متن انگلیسی به فارسی.
 *
 * راهبرد (به ترتیب اولویت):
 *  1) متن‌هایی که از پیش فارسی هستند بدون تغییر بازگردانده می‌شوند.
 *  2) چند سرویس ترجمه عمومی (بدون نیاز به کلید) به‌صورت پشت سر هم امتحان می‌شوند.
 *  3) در نبود اتصال، واژه‌نامه تخصصی هوش مصنوعی (آفلاین) به کار می‌رود.
 *
 * نتیجه‌ها برای جلوگیری از درخواست تکراری نگه‌داری می‌شوند.
 */
class Translator(private val context: Context) {

    private val glossary: List<Pair<String, String>> by lazy { loadGlossary() }
    private val memory = ConcurrentHashMap<String, String>()
    private val pref by lazy {
        context.getSharedPreferences("translations", Context.MODE_PRIVATE)
    }

    fun isProbablyPersian(text: String): Boolean {
        var persian = 0
        var latin = 0
        for (ch in text) {
            val code = ch.code
            if (code in 0x0600..0x06FF || code in 0xFB50..0xFDFF || code in 0xFE70..0xFEFF) {
                persian++
            } else if (ch in 'a'..'z' || ch in 'A'..'Z') {
                latin++
            }
        }
        return persian > 0 && persian >= latin
    }

    /** ترجمه متن؛ خروجی هرگز خالی نیست. در بدترین حالت، متن اصلی بازگردانده می‌شود. */
    fun translate(text: String): String {
        val clean = text.trim()
        if (clean.isEmpty()) return ""
        if (isProbablyPersian(clean)) return clean

        memory[clean]?.let { return it }
        pref.getString(clean, null)?.let {
            memory[clean] = it
            return it
        }

        val online = fetchOnline(clean)
        val result = if (!online.isNullOrBlank()) online else applyGlossary(clean)

        if (result.isNotBlank()) {
            memory[clean] = result
            pref.edit().putString(clean, result).apply()
        }
        return result
    }

    private fun fetchOnline(text: String): String? {
        val chunk = text.take(900)
        return googleTranslate(chunk) ?: myMemory(chunk)
    }

    private fun googleTranslate(text: String): String? {
        return try {
            val q = URLEncoder.encode(text, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single" +
                "?client=gtx&sl=auto&tl=fa&dt=t&q=$q"
            val body = Http.get(url) ?: return null
            val root = JSONArray(body)
            val segments = root.optJSONArray(0) ?: return null
            val sb = StringBuilder()
            for (i in 0 until segments.length()) {
                val seg = segments.optJSONArray(i) ?: continue
                val part = seg.optString(0)
                if (part.isNotBlank()) sb.append(part)
            }
            sb.toString().takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun myMemory(text: String): String? {
        return try {
            val q = URLEncoder.encode(text.take(480), "UTF-8")
            val body = Http.get("https://api.mymemory.translated.net/get?q=$q&langpair=en|fa")
                ?: return null
            val json = JSONObject(body)
            if (json.optInt("responseStatus", 0) != 200) return null
            val translated = json.optJSONObject("responseData")
                ?.optString("translatedText")
                ?.takeIf { it.isNotBlank() }
            if (translated.isNullOrBlank() || translated.contains("MYMEMORY WARNING", true)) {
                null
            } else {
                decodeHtml(translated)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun applyGlossary(text: String): String {
        var out = text
        for ((en, fa) in glossary) {
            if (out.contains(en, ignoreCase = true)) {
                out = out.replace(en, fa, ignoreCase = true)
            }
        }
        return out
    }

    private fun decodeHtml(input: String): String = input
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")

    private fun loadGlossary(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        try {
            val raw = context.assets.open("glossary.json").bufferedReader().use { it.readText() }
            val json = JSONObject(raw)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                list.add(key to json.getString(key))
            }
        } catch (_: Exception) {
        }
        return list.sortedByDescending { it.first.length }
    }
}
