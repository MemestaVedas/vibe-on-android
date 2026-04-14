# Smoothness

Smoothness is bigger than animation. It includes startup latency, scroll stability, cache behavior, and whether the UI keeps showing useful content while work is still finishing. PixelPlayer is valuable here because it treats launch and scroll performance as first-class problems instead of side effects.

## What PixelPlayer teaches

- It keeps a `ShapeCache` for expensive smooth corner shapes so repeated list items do not rebuild the same outlines.
- It uses a bounded color cache for album-art derived colors instead of recalculating them on every pass.
- It has benchmark and baseline profile infrastructure, which means launch and scroll work can be measured instead of guessed.
- It preserves some UI state during loading and sync flows so the screen does not go blank.

## What vibe-on-android already does well

- The app already has baseline profile support in the `baselineprofile` module.
- Widget artwork caches already use `LruCache`, which is the right shape for bounded memory use.
- The motion stack already respects reduced motion, which helps avoid unnecessary visual noise.
- Home screen code already uses nested scroll and preserved data patterns to reduce visual churn during refresh.
- The startup notes in this repo already warn against eager initialization in `VibeonApp`, `MainActivity`, and `AppNavHost`.

## What to borrow

1. Add a shape cache if smooth corners become common in lists or cards.
2. Keep caches bounded. If a feature stores derived colors, bitmaps, or art metadata, use an LRU or other explicit limit.
3. Measure launch and scroll behavior before and after changes that touch startup, library sync, or screen composition.
4. Preserve the last good data while refresh is in progress if a screen would otherwise flash empty.

## Where this matters most

- Home and library lists that reuse the same card shapes many times.
- Now playing surfaces with repeated rounded content.
- Album art and color extraction paths.
- Any startup work that can wait until the first frame is on screen.

## What not to copy blindly

- Unbounded caches.
- Heavy shape creation inside list item composition without reuse.
- Eager initialization that happens before the UI can draw anything useful.
- Animation used as a substitute for actual responsiveness.

## Recommended follow-up

- Decide whether vibe-on-android should introduce a shared smooth-shape cache before expanding use of squircle-style surfaces.
- Review startup work again after any new repository or playback initialization is added.
- Keep profiling in the loop whenever a change could affect cold start or list scroll performance.
