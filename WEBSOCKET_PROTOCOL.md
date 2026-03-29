# VIBE-ON Android — WebSocket Protocol Reference

> Endpoint: `ws://<pc-ip>:5000/control`

This document describes the WebSocket protocol from the **Android client's perspective** — what it sends, what it receives, and how it processes each message.

---

## Connection

The client connects to `ws://<pc-ip>:5000/control` using OkHttp with a 15-second ping interval.

Reconnect policy:
- Exponential backoff with bounded delay and jitter to avoid reconnect storms on unstable networks.
- Automatic reconnect is skipped for `401 Unauthorized` responses (invalid control token) so pairing/auth issues are surfaced instead of retried forever.

### Discovery

The server is discovered via mDNS: `_vibe-on._tcp.local.` on the local network. The resolved IP + port 5000 is used.

### Handshake

Immediately after the WebSocket opens, the client sends:

```json
{
  "type": "hello",
  "clientName": "Android",
  "protocolVersion": "1.1",
  "capabilities": [
    "lyrics.romaji",
    "library.paged",
    "playlists.basic",
    "queue.sync",
    "playback.output-switch"
  ]
}
```

The server replies with a burst of messages:

```json
{
  "type": "connected",
  "clientId": "uuid-string",
  "protocolVersion": "1.1",
  "serverCapabilities": ["lyrics.romaji", "library.paged", "playlists.basic", "queue.sync", "playback.output-switch"],
  "negotiatedCapabilities": ["lyrics.romaji", "library.paged", "playlists.basic", "queue.sync", "playback.output-switch"]
}
{ "type": "mediaSession", ... }
{ "type": "status", ... }
{ "type": "queueUpdate", ... }
```

After the `connected` message is processed, the client also sends `getStatus` and `getPlaylists` to force a fresh state pull and minimize reconnect race windows.

---

## Messages the Client Sends

| Type | Fields | Purpose |
|------|--------|---------|
| `hello` | `clientName` | Identify client on connect |
| `getStatus` | — | Request full state refresh |
| `play` | — | Start/resume playback |
| `pause` | — | Pause playback |
| `resume` | — | Resume playback |
| `stop` | — | Stop playback |
| `next` | — | Next track |
| `previous` | — | Previous track |
| `seek` | `positionSecs` | Seek to position |
| `setVolume` | `volume` (0.0–1.0) | Set volume |
| `toggleShuffle` | — | Toggle shuffle |
| `cycleRepeat` | — | Cycle repeat mode |
| `playTrack` | `path` | Play a specific file |
| `playAlbum` | `album`, `artist` | Play all tracks from album |
| `playArtist` | `artist` | Play all tracks by artist |
| `setQueue` | `paths` (string array) | Replace queue |
| `addToQueue` | `path` | Append track to queue |
| `toggleFavorite` | `path` | Toggle favorite status |
| `getLibrary` | — | Request full library |
| `getLyrics` | — | Request current track lyrics |
| `startMobilePlayback` | — | Switch output to mobile |
| `stopMobilePlayback` | — | Switch output to desktop |
| `mobilePositionUpdate` | `positionSecs` | Report playback position |
| `getPlaylists` | — | List all playlists |
| `getPlaylistTracks` | `playlist_id` | Get playlist tracks |
| `addToPlaylist` | `playlist_id`, `path` | Add track to playlist |
| `removeFromPlaylist` | `playlist_id`, `playlist_track_id` | Remove from playlist |
| `reorderPlaylistTracks` | `playlist_id`, `track_ids` | Reorder playlist |
| `ping` | — | Keepalive |

### Client send helpers

The `WebSocketClient` class provides typed send methods:

| Method | Sends |
|--------|-------|
| `sendPlay()` | `play` |
| `sendPause()` | `pause` |
| `sendResume()` | `resume` |
| `sendStop()` | `stop` |
| `sendNext()` | `next` |
| `sendPrevious()` | `previous` |
| `sendSeek(secs)` | `seek` + positionSecs |
| `sendVolume(vol)` | `setVolume` + volume |
| `sendToggleShuffle()` | `toggleShuffle` |
| `sendCycleRepeat()` | `cycleRepeat` |
| `sendGetStatus()` | `getStatus` |
| `sendGetLibrary()` | `getLibrary` |
| `sendGetLyrics()` | `getLyrics` |
| `sendPlayTrack(path)` | `playTrack` + path |
| `sendPlayAlbum(album, artist)` | `playAlbum` + album + artist |
| `sendPlayArtist(artist)` | `playArtist` + artist |
| `sendAddToQueue(path)` | `addToQueue` + path |
| `sendSetQueue(paths)` | `setQueue` + paths |
| `sendToggleFavorite(path)` | `toggleFavorite` + path |
| `sendStartMobilePlayback()` | `startMobilePlayback` |
| `sendStopMobilePlayback()` | `stopMobilePlayback` |
| `sendMobilePositionUpdate(secs)` | `mobilePositionUpdate` + positionSecs |
| `sendGetPlaylists()` | `getPlaylists` |
| `sendGetPlaylistTracks(id)` | `getPlaylistTracks` + playlist_id |
| `sendAddToPlaylist(id, path)` | `addToPlaylist` + playlist_id + path |
| `sendRemoveFromPlaylist(id, tid)` | `removeFromPlaylist` + playlist_id + playlist_track_id |
| `sendReorderPlaylistTracks(id, ids)` | `reorderPlaylistTracks` + playlist_id + track_ids |

---

## Messages the Client Receives

Each incoming message is dispatched by its `"type"` field to a handler method:

### `connected`
```json
{ "type": "connected", "clientId": "uuid" }
```
**Action:** Stores the client ID. Triggers `onConnected` state.

### `mediaSession`
```json
{
  "type": "mediaSession",
  "trackId": "path", "title": "...", "artist": "...",
  "album": "...", "duration": 240.5,
  "coverUrl": "/cover/...",
  "isPlaying": true, "position": 42.5,
  "timestamp": 1710000000000
}
```
**Action:** Updates `_mediaSession` StateFlow. Cover URL is resolved to absolute URL. This is the primary source for "what's playing" UI.

### `status`
```json
{
  "type": "status",
  "volume": 0.75, "shuffle": false,
  "repeatMode": "off", "output": "desktop"
}
```
**Action:** Updates `_playerStatus` StateFlow.

### `PlaybackState`
```json
{
  "type": "PlaybackState",
  "is_playing": true, "position": 42.5, "volume": 0.75
}
```
**Action:** Updates `_playbackState` StateFlow. Lightweight periodic update used to keep position slider in sync.

### `queueUpdate`
```json
{
  "type": "queueUpdate",
  "queue": [ { "path": "...", "title": "...", ... } ],
  "currentIndex": 0
}
```
**Action:** Updates `_queue` and `_currentQueueIndex` StateFlows.

### `handoffPrepare`
```json
{
  "type": "handoffPrepare",
  "sample": 1940400,
  "url": "http://192.168.1.100:5000/stream/..."
}
```
**Action:** Updates `_streamUrl` and `_streamSample` StateFlows. The URL is **normalized** — the host is rewritten to match the WebSocket connection IP (handles cases where the server reports `127.0.0.1` or localhost).

**Mobile streaming flow:**
1. `PlaybackViewModel` observes `streamUrl`
2. Creates ExoPlayer `MediaItem` from the URL
3. If sample > 0 and NOT a FLAC file, seeks to `sample / 44100` seconds
4. FLAC files skip seeking to avoid decode corruption

### `streamStopped`
```json
{ "type": "streamStopped" }
```
**Action:** Clears `_streamUrl` to `null`. `PlaybackViewModel` stops ExoPlayer.

### `lyrics`
```json
{
  "type": "lyrics",
  "trackPath": "...", "hasSynced": true,
  "syncedLyrics": "[00:12.34] ...",
  "plainLyrics": "...",
  "instrumental": false
}
```
**Action:** Updates `_lyrics` StateFlow.

### `library`
```json
{
  "type": "library",
  "tracks": [ { "path": "...", "title": "...", ... } ]
}
```
**Action:** Updates `_libraryTracks` StateFlow. Tracks are parsed via `JsonParsingUtils.toTrackInfo()`.

### `playlists`
```json
{
  "type": "playlists",
  "playlists": [ { "id": "uuid", "name": "...", "trackCount": 12 } ]
}
```
**Action:** Updates `_playlists` StateFlow.

### `playlistTracks`
```json
{
  "type": "playlistTracks",
  "playlistId": "uuid",
  "tracks": [ ... ]
}
```
**Action:** Updates `_playlistTracks` StateFlow map.

### `statsUpdated`
```json
{ "type": "statsUpdated", "timestamp": 1710000000000 }
```
**Action:** Updates `_statsTimestamp` StateFlow.

### `ack`
```json
{ "type": "ack", "action": "toggleFavorite" }
```
**Action:** Logged for debugging. Confirms a modifying action succeeded.

### `error`
```json
{ "type": "error", "message": "No track playing" }
```
**Action:** Updates `_error` StateFlow. UI can observe and show toast/snackbar.

### `pong`
```json
{ "type": "pong" }
```
**Action:** No-op (OkHttp handles keepalive internally).

---

## State Flows (Observable)

The `WebSocketClient` exposes these `StateFlow` properties for Compose UI:

| StateFlow | Type | Updated by |
|-----------|------|------------|
| `connectionState` | `ConnectionState` enum | connect/disconnect events |
| `mediaSession` | `MediaSessionData?` | `mediaSession` messages |
| `playerStatus` | `PlayerStatus?` | `status` messages |
| `playbackState` | `PlaybackStateData?` | `PlaybackState` messages |
| `queue` | `List<QueueItem>` | `queueUpdate` messages |
| `currentQueueIndex` | `Int` | `queueUpdate` messages |
| `lyrics` | `LyricsData?` | `lyrics` messages |
| `libraryTracks` | `List<TrackInfo>` | `library` messages |
| `playlists` | `List<PlaylistInfo>` | `playlists` messages |
| `playlistTracks` | `Map<String, List<TrackInfo>>` | `playlistTracks` messages |
| `streamUrl` | `String?` | `handoffPrepare` / `streamStopped` |
| `streamSample` | `Long` | `handoffPrepare` |
| `error` | `String?` | `error` messages |
| `statsTimestamp` | `Long` | `statsUpdated` messages |

---

## PlaybackViewModel — Streaming Architecture

`PlaybackViewModel` bridges `WebSocketClient` → ExoPlayer:

```
WebSocketClient.streamUrl changes
  ↓
PlaybackViewModel observes via collectLatest
  ↓
  ├── url != null → create MediaItem, prepare ExoPlayer, seek, play
  │                 start periodic mobilePositionUpdate (1s interval)
  │
  └── url == null → stop ExoPlayer, release resources
```

### Key behaviors

- **FLAC seek skip:** FLAC streams don't seek on handoff (causes decode errors). Position starts from 0.
- **Auto-next:** When ExoPlayer reaches `STATE_ENDED`, waits 1.5s cooldown then sends `next`.
- **Offline songs:** If the stream URL contains `offline://`, loads from local storage instead of HTTP.
- **Deferred streams:** If `handoffPrepare` arrives before ExoPlayer is attached, the URL is stored and applied when the player surface becomes available.

---

## Audio Stream

The audio is streamed over HTTP (not WebSocket):

```
GET http://<ip>:5000/stream/<url-encoded-path>
```

- Returns raw audio data (FLAC, MP3, etc.)
- ExoPlayer handles format detection and decoding
- The stream URL host is **always normalized** to the WebSocket connection IP

---

## Error Handling & Reconnection

- On WebSocket failure: `connectionState` → `Disconnected`
- The UI layer is responsible for triggering reconnection
- OkHttp automatically sends WebSocket pings every 15 seconds
- Server sends `pong` keepalives every 30 seconds independently
- Network errors during streaming are handled by ExoPlayer's retry logic
