package ir.hoshemasnooi.app.util

import java.net.HttpURLConnection
import java.net.URL

/**
 * ابزار سبک برای درخواست‌های HTTP بدون کتابخانه بیرونی.
 */
object Http {

    fun get(url: String, headers: Map<String, String> = emptyMap(), timeoutMs: Int = 15000): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android) HooshYar/1.0")
                setRequestProperty("Accept", "application/json, application/rss+xml, application/xml, text/xml, */*")
                for ((k, v) in headers) setRequestProperty(k, v)
            }
            val code = conn.responseCode
            if (code in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
