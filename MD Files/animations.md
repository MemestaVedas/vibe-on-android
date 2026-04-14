# Animations

This is the main transfer area from PixelPlayer. PixelPlayer uses animation in a disciplined way: not only for motion, but for perceived responsiveness. It smooths progress updates, caches expensive state, and wraps haptic behavior so the UI can stay consistent. vibe-on-android already has a stronger motion system, so the goal is to borrow the useful parts without flattening the current style.

## What PixelPlayer teaches

- Smooth progress is better than direct jumps. It uses `animateFloatAsState` for progress and alpha in several places.
- Expensive UI state should be cached or precomputed when possible.
- Haptics can be centralized behind a small policy surface.
- Transition and micro-animation code should be easy to find and easy to reuse.

## What vibe-on-android already does well

- `MotionTokens` already separates duration, easing, and spring behavior.
- `bouncyClickable` already gives press feedback with motion plus haptics.
- `ButtonStyles.kt` already respects reduced motion in interactive components.
- `NowPlayingScreen.kt` already animates art, progress, and layered interaction states.
- `VibeLoadingIndicators.kt` already keeps loading UI in one place, which makes polish easier to improve later.

## What to borrow

1. Keep animating state changes that users watch directly: playback progress, scrubbing, panel alpha, and selection state.
2. Add more animation helpers only when they remove repeated code or keep motion consistent.
3. If interaction-heavy screens grow, consider a shared haptic policy layer so repeated taps, presses, and drags can be tuned in one place.
4. Use animation for feedback, not decoration. If the motion does not improve understanding or perceived speed, skip it.

## What to be careful about

- Do not copy a wrapper that hides useful haptic types or removes nuance from the current interaction model.
- Do not add animation to every state change. If the state changes constantly or the user is dragging, direct updates can be better than animated ones.
- Keep reduced-motion checks on any infinite or decorative animation.

## Good candidates in vibe-on-android

- Button press feedback across cards, chips, and primary actions.
- Playback progress, seek feedback, and loading indicators.
- Art and header emphasis on the now playing screen.
- Staggered list reveals only when they help the user read the page.

## Recommended follow-up

- Review `bouncyClickable` and decide whether a short haptic cooldown is needed for repeated taps.
- Keep the current motion tokens as the default animation language.
- Add new animation helpers only if they can be reused by more than one screen.
