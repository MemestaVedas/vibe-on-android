package moe.memesta.vibeon.torrent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

data class NyaaSearchResult(
    val title: String,
    val size: String,
    val seeds: Int,
    val leechers: Int,
    val downloads: Int,
    val date: String,
    val magnetLink: String,
    val torrentUrl: String,
    val viewUrl: String,
    val category: String
)

enum class NyaaCategory(val param: String, val label: String) {
    AUDIO("2_0", "Audio"),
    ALL("0_0", "All")
}

enum class NyaaSort(val param: String, val label: String) {
    SEEDERS("seeders", "Seeders"),
    SIZE("size", "Size"),
    DATE("id", "Date"),
    DOWNLOADS("downloads", "Downloads")
}

enum class NyaaOrder(val param: String, val label: String) {
    DESC("desc", "Desc"),
    ASC("asc", "Asc")
}

object NyaaSearchService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun search(
        query: String,
        category: NyaaCategory = NyaaCategory.AUDIO,
        sort: NyaaSort = NyaaSort.SEEDERS,
        order: NyaaOrder = NyaaOrder.DESC
    ): List<NyaaSearchResult> = withContext(Dispatchers.IO) {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "https://nyaa.si/?f=0&c=${category.param}&q=$encodedQuery&s=${sort.param}&o=${order.param}"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android) VibeonApp/1.0")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Nyaa returned HTTP ${response.code}")
        }

        val html = response.body?.string() ?: throw Exception("Empty response from Nyaa")
        parseResults(html)
    }

    private fun parseResults(html: String): List<NyaaSearchResult> {
        val doc = Jsoup.parse(html)
        val rows = doc.select("tr.default, tr.success, tr.danger")
        val results = mutableListOf<NyaaSearchResult>()

        for (row in rows) {
            try {
                val tds = row.select("td")
                if (tds.size < 8) continue

                val categoryEl = tds[0].selectFirst("a")
                val categoryTitle = categoryEl?.attr("title") ?: "Audio"

                // Title: last link in td[1]
                val titleLinks = tds[1].select("a")
                val titleEl = titleLinks.lastOrNull() ?: continue
                val title = titleEl.text().trim()
                val viewHref = titleEl.attr("href")
                val viewUrl = "https://nyaa.si$viewHref"

                // Torrent + magnet links in td[2]
                val torrentEl = tds[2].selectFirst("a[href$='.torrent']")
                val magnetEl = tds[2].selectFirst("a[href^='magnet:']")
                val torrentUrl = torrentEl?.let { "https://nyaa.si${it.attr("href")}" } ?: ""
                val magnetLink = magnetEl?.attr("href") ?: ""
                if (magnetLink.isEmpty()) continue

                val size = tds[3].text().trim()
                val date = tds[4].text().trim()
                val seeds = tds[5].text().trim().toIntOrNull() ?: 0
                val leechers = tds[6].text().trim().toIntOrNull() ?: 0
                val downloads = tds[7].text().trim().toIntOrNull() ?: 0

                results.add(
                    NyaaSearchResult(
                        title = title,
                        size = size,
                        seeds = seeds,
                        leechers = leechers,
                        downloads = downloads,
                        date = date,
                        magnetLink = magnetLink,
                        torrentUrl = torrentUrl,
                        viewUrl = viewUrl,
                        category = categoryTitle
                    )
                )
            } catch (_: Exception) {
                // skip malformed rows
            }
        }
        return results
    }
}
