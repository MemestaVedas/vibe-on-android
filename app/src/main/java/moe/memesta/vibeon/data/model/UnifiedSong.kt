package moe.memesta.vibeon.data.model

/**
 * Represents a song that may exist locally on device, on the server, or both.
 * The deduplication logic in [moe.memesta.vibeon.ui.OfflineSongsViewModel]
 * matches offline MediaStore entries against server library tracks by
 * normalized (title + artist) and produces this unified view.
 */
data class UnifiedSong(
    val id: Long,
    val title: String,
    val artist: String,
    val albumId: Long = 0,
    val duration: Long = 0,
    /** Content URI for local playback via ExoPlayer (null if server-only). */
    val localUri: String? = null,
    /** Absolute server path for remote playback (null if offline-only). */
    val serverPath: String? = null,
    /** Album art URL from the server library entry (null if offline-only). */
    val coverUrl: String? = null,
    /** True if a matching local file exists on this device. */
    val isOfflineAvailable: Boolean = localUri != null
)

/** Normalize a track title/artist for fuzzy matching against server entries. */
fun normalizeTrackString(s: String): String = s
    .lowercase()
    .replace(Regex("""\(feat\.?[^)]*\)"""), "")
    .replace(Regex("""\(ft\.?[^)]*\)"""), "")
    .replace(Regex("""\(remaster(ed)?[^)]*\)""", RegexOption.IGNORE_CASE), "")
    .replace(Regex("""\s*-\s*(single|ep|album|remaster(ed)?)\s*$""", RegexOption.IGNORE_CASE), "")
    .replace(Regex("""[^a-z0-9\s]"""), "")
    .replace(Regex("""\s+"""), " ")
    .trim()
