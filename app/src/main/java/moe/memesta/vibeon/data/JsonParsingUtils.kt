package moe.memesta.vibeon.data

import org.json.JSONObject

/**
 * Shared utilities for parsing JSON responses from the VIBE-ON server.
 * Eliminates duplication across WebSocketClient and MusicStreamClient.
 */

/** Resolves a raw cover URL to an absolute HTTP URL, using [baseUrl] as base. */
fun resolveCoverUrl(rawUrl: String?, baseUrl: String): String? {
    val url = rawUrl?.takeIf { it.isNotEmpty() && it != "null" } ?: return null
    return if (url.startsWith("http")) url
    else if (url.startsWith("/")) "$baseUrl$url"
    else "$baseUrl/$url"
}

/** Parses a JSON track object into a [TrackInfo], resolving cover URL against [baseUrl]. */
fun JSONObject.toTrackInfo(baseUrl: String): TrackInfo {
    val rawCover = optString("coverUrl", null)?.takeIf { it != "null" && it.isNotEmpty() }
        ?: optString("cover_url", null)?.takeIf { it != "null" && it.isNotEmpty() }
    return TrackInfo(
        path = getString("path"),
        title = getString("title"),
        artist = getString("artist"),
        album = getString("album"),
        duration = optDouble("durationSecs", 0.0),
        coverUrl = resolveCoverUrl(rawCover, baseUrl),
        year = extractYearFromJson(this),
        discNumber = optInt("discNumber", -1).takeIf { it != -1 },
        trackNumber = optInt("trackNumber", -1).takeIf { it != -1 },
        titleRomaji = optString("titleRomaji", null).takeIf { it != "null" },
        titleEn = optString("titleEn", null).takeIf { it != "null" },
        artistRomaji = optString("artistRomaji", null).takeIf { it != "null" },
        artistEn = optString("artistEn", null).takeIf { it != "null" },
        albumRomaji = optString("albumRomaji", null).takeIf { it != "null" },
        albumEn = optString("albumEn", null).takeIf { it != "null" },
        playlistTrackId = optLong("playlistTrackId", -1L).takeIf { it != -1L }
    )
}

private fun extractYearFromJson(json: JSONObject): Int? {
    val directYear = json.optInt("year", -1).takeIf { it in 1000..2999 }
        ?: json.optInt("releaseYear", -1).takeIf { it in 1000..2999 }
    if (directYear != null) return directYear

    val rawDate = json.optString("releaseDate", "")
        .ifEmpty { json.optString("date", "") }
        .ifEmpty { json.optString("originalDate", "") }
    val yearPrefix = rawDate.take(4).toIntOrNull()
    return yearPrefix?.takeIf { it in 1000..2999 }
}

/** Parses a JSON queue item object into a [QueueItem], resolving cover URL against [baseUrl]. */
fun JSONObject.toQueueItem(baseUrl: String): QueueItem {
    val rawCover = optString("coverUrl", null)?.takeIf { it != "null" && it.isNotEmpty() }
        ?: optString("cover_url", null)?.takeIf { it != "null" && it.isNotEmpty() }
    return QueueItem(
        path = getString("path"),
        title = getString("title"),
        artist = getString("artist"),
        album = getString("album"),
        duration = optDouble("durationSecs", 0.0),
        coverUrl = resolveCoverUrl(rawCover, baseUrl),
        titleRomaji = optString("titleRomaji", null).takeIf { it != "null" },
        titleEn = optString("titleEn", null).takeIf { it != "null" },
        artistRomaji = optString("artistRomaji", null).takeIf { it != "null" },
        artistEn = optString("artistEn", null).takeIf { it != "null" },
        albumRomaji = optString("albumRomaji", null).takeIf { it != "null" },
        albumEn = optString("albumEn", null).takeIf { it != "null" }
    )
}
