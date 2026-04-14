# PixelPlayer Comparison Notes

This folder collects the PixelPlayer -> vibe-on-android comparison in topic files so the work can grow without turning into one oversized document.

Files:
- `transitions.md` - screen, sheet, and navigation motion patterns
- `animations.md` - interactive motion, progress, and haptics
- `smoothness.md` - startup, scroll stability, and responsiveness
- `caching-and-shapes.md` - shape caching, palette caching, and reusable surfaces

Priority order:
1. Keep vibe-on-android's stronger motion and reduced-motion system.
2. Borrow PixelPlayer's shape and cache discipline where it removes real jank.
3. Add profiling or baseline work only where it changes launch or scroll behavior.

Current conclusion:
- PixelPlayer is useful as a reference for cache reuse, baseline profiling, and some transition patterns.
- vibe-on-android is already ahead on motion tokens, reduced-motion support, and several interaction patterns.
- The most valuable transfer is not visual imitation; it is avoiding repeated work in lists, screens, and startup paths.
