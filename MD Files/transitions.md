# Transitions

PixelPlayer's transition code is useful mostly as a consistency reference. It centralizes many screen and sheet animations around a small set of shared patterns: horizontal slide plus fade for navigation, vertical lift for detail views, and slightly different enter/exit timing to avoid a flat feel. The strongest lesson is not the exact numbers. It is the habit of routing transitions through a few named helpers instead of scattering ad hoc tween values through screens.

## What PixelPlayer does well

- Uses shared transition helpers for repeated navigation flows.
- Keeps enter and exit motion asymmetric, which makes the UI feel less mechanical.
- Applies the same general motion language to sheets, dialogs, and screen changes.
- Uses transition-specific durations instead of one global animation length for everything.

## What vibe-on-android already does better

- `app/src/main/java/moe/memesta/vibeon/ui/navigation/Transitions.kt` already uses `MotionTokens` instead of raw values.
- Reduced-motion support already exists through `MotionAccessibility.kt`.
- The motion language is already more expressive, with vertical enter/exit and sheet-specific transitions.
- The code already separates navigation motion from local component motion, which is the right direction.

## What to borrow

1. Keep the helper-based approach and extend it only where it reduces repetition.
2. Prefer semantic names like `sheetEnterTransition` or `verticalExitTransition` over raw timing in call sites.
3. Use a small number of motion families: horizontal navigation, vertical overlays, and bottom-sheet motion.
4. When adding new screens, define the transition once and reuse it everywhere that route appears.

## What not to copy directly

- Hard-coded durations with no reduced-motion branch.
- One-size-fits-all slide distances for every surface.
- Inline transition specs inside individual screens when a shared helper would do.

## Recommended follow-up

- Audit all navigation entry points and move any remaining one-off transitions into the shared transition file.
- Keep the current `MotionTokens` approach as the source of truth.
- If a new surface needs motion, decide whether it belongs in horizontal navigation, vertical lift, or sheet motion before writing code.
