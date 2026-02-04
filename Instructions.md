# Mobile App Instruction Set: Vibe-On

This document provides a complete set of instructions for the mobile application's UI/UX architecture, navigation flow, and dynamic styling based on Material 3 principles.

## 1. Core Pages & Structure

### [Library Screen](file:///d:/Kushal/projects/vibe-main/vibe-on-android/app/src/main/java/moe/memesta/vibeon/ui/LibraryScreen.kt)
- **Top Bar**: A **floating pill** design containing the dynamic title ("Library", Artist Name, or Album Name) with a Search toggle and Back button. This element should appear elevated and centered at the top of the screen.
- **Tab Navigation**: Three main tabs:
  - **Tracks**: Circular list of all tracks.
  - **Albums**: 2-column grid of album covers.
  - **Artists**: Vertical list of artists with chevron indicators.
- **Global Search**: Floating/expandable search bar that filters the current view.
- **Persistent MiniPlayer**: Pill-shaped player anchored to the bottom.

### [Now Playing Screen](file:///d:/Kushal/projects/vibe-main/vibe-on-android/app/src/main/java/moe/memesta/vibeon/ui/NowPlayingScreen.kt)
- **Visual Hierarchy**: Large album art, track metadata, technical badges (FLAC, bitrate), progress bar, main controls, and output switching.
- **Background**: High-blur album art overlay with a dark vertical gradient to ensure legible controls.

## 2. Navigation Flow

- **Library -> Player**: Tapping the [MiniPlayer](file:///d:/Kushal/projects/vibe-main/vibe-on-android/app/src/main/java/moe/memesta/vibeon/ui/MiniPlayer.kt) or a track list item triggers a vertical slide-up transition to `NowPlayingScreen`.
- **Player -> Library**: Swipe down, tap the "Down Arrow" icon, or use the **Android system back gesture** to slide the player back into the `MiniPlayer`.
- **Internal Library**: Tapping an Artist or Album filtered the `Tracks` view. Back button restores the previous tab/state.

## 3. Dynamic Color System (Material 3)

The app follows the `Color.md` specification for Content-based Dynamic Colors:

- **Primary Source**: Extracted from the `coverUrl` of the current track using the `androidx.palette` library.
- **Color Roles**:
  - `Vibrant`: Map to `colorPrimary` and `colorAccent`. Used for the Play button and Progress Bar.
  - `DarkVibrant`: Map to secondary background elements.
  - `DarkMuted`: Used to seed the background gradient (interpolating to `VibeBackground`).
- **Luminance Guard**: Ensure `vibrantColor` has a minimum luminance (e.g., 0.6) for visibility against dark backgrounds. Use `ensureLuminance` utility to brighten if necessary.

## 4. Animations & Micro-interactions

- **Shared Elements**: The Album Art should appear to transition from the MiniPlayer/List to its large centered position in `NowPlaying`.
- **Squiggly Progress Bar**: The active track color follows a periodic sine wave path that "flows" as the track plays.
- **Button Feedback**: Haptic feedback on Play/Pause and Skip buttons. Scale animation (0.95x -> 1.0x) on tap.

## 5. Swipe Gestures

- **Dismissal**: Vertical swipe down on `NowPlayingScreen` to minimize to `MiniPlayer`.
- **Track Navigation**: Horizontal drag on the Album Art in `NowPlaying` to skip tracks (Left = Next, Right = Previous).
- **Seeking**: Horizontal drag or tap on the `SquigglyProgressBar` for precise temporal control.

## 6. Implementation Guidelines

- **Theme Alignment**: Always use `MaterialTheme.colorScheme` attributes instead of hardcoded hex values to support dynamic overrides.
- **Performance**: Perform `Palette` extraction on a background coroutine to prevent UI thread blocking during track changes.
- **State Management**: Use `PlaybackViewModel` for audio state and `LibraryViewModel` for content state to maintain a clean separation of concerns.
