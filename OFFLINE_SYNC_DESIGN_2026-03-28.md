# Vibe-On Android: Offline Library Sync & Deduplication Design
**Date**: 2026-03-28  
**Status**: In Progress (Quick Wins Complete)

---

## Problem Statement

The Android app has three critical issues with local/offline song management:

1. **Deduplication**: Same songs from PC library and mobile downloads appear as separate entries
2. **Dependency**: Mobile library depends on PC being online; can't reliably play offline songs independently
3. **Streaming Bug**: Song changes don't sync properly when streaming from PC (race condition in skip debounce)

---

## Solution Architecture

### Part 1: Quick Win - Streaming Bug Fix ✅ DONE

**Issue**: Skip debounce was too aggressive (700ms), causing missed state updates.

**Fix Implemented**:
- Reduced `SKIP_DEBOUNCE_MS` from 700ms to 400ms
- Added track state tracking: only block rapid skips if **still on same track**
- Enhanced logging to track skip flow for debugging

**Location**: `WebSocketClient.kt` lines 79, 107-127, 399-417

**Impact**: Immediate improvement in song change responsiveness during streaming.

---

### Part 2: Deduplication Framework ✅ IN PROGRESS

**Key Insight**: Tracks are identical if they have same **title + artist + album**, regardless of source.

**Implementation**:

#### A. Enhanced Database Schema (v3→v4)

Added three new fields to `TrackEntity`:

```kotlin
data class TrackEntity(
    // ... existing fields ...
    val source: String = "pc"              // "pc" or "mobile"
    val canonicalId: String                // SHA256(title|artist|album).substring(0,16)
    val localPath: String? = null          // File path if downloaded/cached
)
```

**Migration** (`DatabaseMigrations.kt`):
- v3→v4: Adds columns with safe defaults
- Populates `canonicalId` for existing tracks
- Creates index for dedup queries

#### B. Dedup Queries in TrackDao

New methods:
- `getTracksDeduped()`: Single view → one entry per unique track (PC version preferred)
- `getTrackSources(canonicalId)`: All versions of a track (PC + mobile)
- `getDuplicateGroupIds()`: Identify which tracks have duplicates
- `mergeTracksByMetadata()`: Smart merge during sync

#### C. Canonical ID Generation

```kotlin
fun generateCanonicalId(title: String, artist: String, album: String): String {
    val normalized = "$title|$artist|$album".lowercase().trim()
    val bytes = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray())
    return bytes.substring(0, 16)  // 16-char hex string
}
```

**Example**:
- PC: `/music/Evangelion_OST/01_Angel_Attack.mp3` → `canonicalId: "f2a9c3b1e4d5f8g2"`
- Mobile: `/Vibe-On/Evangelion_01.m4a` → same `canonicalId`
- **Display**: Single entry in UI, user can switch between sources

---

### Part 3: Unified Offline Library (NOT YET IMPLEMENTED)

**Goal**: Single library view with PC + mobile songs merged intelligently.

#### Data Flow

```
PC Connected             PC Disconnected
     ↓                          ↓
[WebSocket Sync]         [LibraryRepository]
     ↓                          ↓
[LibraryRepository.refreshLibrary()]
     ↓
[TrackDao.saveTracksToDb() + mergeTracksByMetadata()]
     ↓
[Room Database with source + canonicalId]
     ↓
[getTracksDeduped() for UI]  ←  Single unified view
     ↓                           (prev: two separate lists)
[Display Layer]
```

#### How Dedup Works

When PC syncs library to mobile:

1. **Detect duplicates**: `TrackDao.getDuplicateGroupIds()`
2. **Prefer PC metadata**: Higher quality (more fields, accurate romaji, etc.)
3. **Keep mobile localPath**: If downloaded on mobile, preserve file path
4. **Show in UI as one entry**:
   - Title, artist, album from PC
   - Play button: Auto-select best source (mobile if offline, PC if online)

**Example UI**:
```
┌─ Evangelion OST ─────────────────┐
│ • Angel Attack (PC + Mobile)      │  ← shows both sources available
│   [📱 Play Cached] [🖥️ Stream]    │
│ • Serenity (PC only)              │
│   [🖥️ Stream]                     │
└───────────────────────────────────┘
```

---

### Part 4: Folder Picker & Manual Sync (DESIGN PHASE)

**Goal**: User selects albums/artists from PC → auto-download to mobile (B+C requirements).

#### UI Flow

```
Settings → Library Sync
    ↓
Browse Folders: [▸ Artist] [▸ Album]  ← Expandable tree from PC
    ├─ Evangelion TV
    │  └─ 📥 3 songs (download)
    ├─ Neon Genesis (already synced)
    │  └─ ✓ 14 songs
    └─ Maintenance
       └─ [📥 Sync All] button
    ↓
[Select] → [Download to Mobile]
    ↓
Stores in: ~/Music/Vibe-On/
Metadata: TrackEntity(source="mobile", localPath="/Music/Vibe-On/...")
```

#### Implementation Points

1. **PC Endpoint**: Expose folder structure via REST API
   - `GET /library/artists` → list with song counts
   - `GET /library/albums?artist=X` → albums for artist
   - `GET /stream/{path}?range=bytes=...` → already supports this

2. **Mobile Download Manager**:
   - Extend `TorrentDownloadManager` → add HTTP download option
   - Store metadata with `source="mobile"` and `localPath`
   - Implement pause/resume, progress tracking

3. **Sync Scheduler** (future):
   - Mark favorites → auto-sync overnight
   - Batch download to save bandwidth
   - Sync on Wi-Fi only option

---

## Database Schema Changes

### TrackEntity v4

```
┌─ Original Fields ─────────────────────────────────┐
│ id (PK)              | path on PC or cache ID     │
│ title, artist, album | track metadata             │
│ duration, year, ...  | additional fields          │
├─ New Dedup Fields (v4) ──────────────────────────┤
│ source               | "pc" or "mobile"           │
│ canonicalId          | SHA256(title|artist|album) │
│ localPath            | file path if cached        │
└──────────────────────────────────────────────────┘
```

### Indexes (for performance)

- `(canonicalId)` → fast dedup lookups
- `(source)` → filter PC vs mobile
- `(album, artist)` → existing, kept

---

## Testing Strategy

### Unit Tests (Room Tests)

```kotlin
@Test
fun testCanonicalIdGeneration() {
    val id1 = TrackEntity.generateCanonicalId("Angel Attack", "Evangelion", "NGE")
    val id2 = TrackEntity.generateCanonicalId("angel attack", "evangelion", "nge")
    assertEquals(id1, id2)  // Case-insensitive
}

@Test
fun testDedupQuery() {
    // Insert PC version
    trackDao.insertTrack(TrackEntity(..., canonicalId="f2a9c3b1", source="pc"))
    // Insert mobile version
    trackDao.insertTrack(TrackEntity(..., canonicalId="f2a9c3b1", source="mobile"))
    
    val deduped = trackDao.getTracksDeduped().firstOrNull()
    assertEquals("pc", deduped?.source)  // Prefers PC
}
```

### Integration Tests

- Sync PC library → verify canonicalIds match duplicates
- Switch between PC streaming + mobile offline → verify source selection
- Download song → update localPath → verify offline playback

---

## Rollout Plan

### Phase 1 (Current) ✅
1. ✅ Fix streaming skip bug
2. ✅ Add dedup database fields + migration
3. ✅ Implement dedup queries

### Phase 2 (Next)
4. Update `LibraryRepository` to call `mergeTracksByMetadata()`
5. Update UI display layers to use `getTracksDeduped()`
6. Add tests

### Phase 3 (Future)
7. Implement folder picker UI
8. Add manual download manager
9. Implement favorite-based auto-sync

---

## Files Modified

| File | Change |
|------|--------|
| `WebSocketClient.kt` | Fix skip debounce + state tracking |
| `TrackEntity.kt` | Add source, canonicalId, localPath |
| `LibraryDatabase.kt` | Bump version to 4, add migration |
| `DatabaseMigrations.kt` | Create MIGRATION_3_4 |
| `TrackDao.kt` | Add dedup + merge queries |

---

## Next Steps

1. **Data layer finalization**: Update `LibraryRepository` to use new dedup methods
2. **UI updates**: Modify track list displays to show unified library
3. **Download manager**: Add HTTP download capability (not just torrents)
4. **Testing**: Comprehensive Room tests + integration testing
5. **Rollout**: Gradual rollout with migration safeguards

---

## Success Criteria

- ✅ Song changes sync reliably (skip debounce fixed)
- ⏳ Unified library: One entry per unique song in offline view
- ⏳ Manual sync: User can browse PC folders and download albums
- ⏳ Smart fallback: Auto-select best source (mobile if offline, PC if online)
