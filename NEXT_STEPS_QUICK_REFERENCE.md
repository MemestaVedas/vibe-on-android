# Quick Reference: Next Steps for Offline Sync

## Immediate TODO (Phase 2)

### 1. Update UI Layers to Use Dedup
**Files to update**: `ArtistDetailScreen.kt`, `LibraryScreen.kt`, any track list UI

**Change**:
```kotlin
// Before:
trackDao.getAllTracks()

// After:
trackDao.getTracksDeduped()  // No more duplicates!
```

### 2. Add Source Selector UI
**Location**: Track card / detail view

**Show**:
```
🔷 Angel Attack (2 sources available)
   [📱 Play from Cache] [🖥️ Stream from PC]
```

### 3. Test Migration
```bash
# v3 database → v4
# MIGRATION_3_4 should auto-run on first launch
# Verify: All tracks still play, canonicalId populated
```

---

## Phase 3: Download Manager

### Create HTTP Download
File: `src/main/java/moe/memesta/vibeon/download/HttpDownloadManager.kt`

```kotlin
class HttpDownloadManager(val host: String, val port: Int) {
    suspend fun downloadTrack(trackPath: String, savePath: String): Boolean {
        val url = "http://$host:$port/stream/{trackPath}"
        // Use OkHttp + writeToFile
        // Update TrackEntity.localPath when done
        // Mark source = "mobile"
    }
    
    suspend fun downloadAlbum(albumName: String, artist: String): List<TrackEntity> {
        // Fetch album tracks via /library endpoint
        // Queue downloads sequentially or parallel
        // Return list of downloaded TrackEntities
    }
}
```

### Create Folder Picker UI
File: `src/main/java/moe/memesta/vibeon/ui/LibrarySyncScreen.kt`

```kotlin
@Composable
fun LibrarySyncScreen(
    wsClient: WebSocketClient,
    downloadManager: HttpDownloadManager
) {
    // Expandable tree of PC albums/artists
    // Checkboxes to select
    // [Download Selected] button
    // Progress indicator
}
```

---

## Verification Checklist

### Build & Compile
- [ ] No compilation errors in Android Studio
- [ ] All imports resolve (check `DatabaseMigrations.kt`, `TrackEntity.kt`)

### Database Migration
- [ ] Launch app on v4 database
- [ ] No crashes during migration
- [ ] Check logcat: "Successfully migrated database"
- [ ] Run: `adb shell "sqlite3 /data/data/.../vibe_on_library.db 'PRAGMA table_info(tracks);'"`
  - Should show new columns: `source`, `canonicalId`, `localPath`

### Functional Tests
- [ ] PC sync → canonicalIds appear in database
- [ ] `getTracksDeduped()` returns ~half the tracks (dedup working)
- [ ] `getTrackSources()` shows both PC and mobile versions
- [ ] Skip songs 5x rapidly → no dropped commands

### UI Testing
- [ ] Library screen loads (no crashes)
- [ ] Search still works
- [ ] Artist/album grouping still works
- [ ] Playing PC tracks still works

---

## Debugging Tips

### Check Database via Command Line
```bash
adb shell
sqlite3 /data/data/moe.memesta.vibeon/databases/vibe_on_library.db

# See all tracks with new fields
SELECT id, title, artist, source, canonicalId, localPath FROM tracks LIMIT 5;

# Find duplicates
SELECT canonicalId, COUNT(*) FROM tracks GROUP BY canonicalId HAVING COUNT(*) > 1;

# See migration status
PRAGMA table_info(tracks);
```

### Enable Debug Logging
In `TrackDao`, `WebSocketClient`:
```kotlin
Log.d("Dedup", "Track: $title, canonicalId=$canonicalId, source=$source")
```

### Test Dedup Query
```kotlin
// In test
val deduped = trackDao.getTracksDeduped().firstOrNull()
assertEquals(sourceOfPc, deduped.source)  // Should prefer PC
```

---

## Future Enhancements

### v5 Features
- [ ] Smarter canonicalId (normalize artist/album diacritics)
- [ ] Track fingerprinting (audio hash for exact matches)
- [ ] Auto-cache favorites when phone is on WiFi + charging
- [ ] PC app sync: upload playback stats back to desktop app

### v6 Features
- [ ] Third-party API integration (Spotify/Apple Music metadata)
- [ ] Playlist sync between devices
- [ ] Offline queue persistence

---

## Performance Optimization Notes

If database grows to 10,000+ songs:

1. **Pagination**: Use `getTracksPage()` instead of `getAllTracks()`
2. **Index**: Create partial index on `(canonicalId, source)` for faster dedup
3. **Lazy Load**: Load cover art on-demand, not at list render
4. **Batch Insert**: Already chunking by 100, good for 10K songs

---

## Rollout Strategy

### Phase 1 (Current Branch)
- ✅ Schema + queries ready
- ✅ Bug fix deployed
- 🚀 Ready for testing

### Phase 2 (Next PR)
- UI integration
- User testing
- Performance validation

### Phase 3 (Stable Release)
- Download manager
- Folder picker
- Beta testing with users

