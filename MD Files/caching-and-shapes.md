# Caching and Shapes

This is the most direct PixelPlayer transfer. PixelPlayer caches expensive smooth corner shapes and bounded color extraction results. That is a good pattern to copy when the same visual primitive appears over and over in lists, cards, and sheets.

## PixelPlayer pattern

- `ShapeCache` stores reusable `AbsoluteSmoothCornerShape` instances for common radii.
- `ColorRoles.kt` uses a bounded `LruCache` for extracted seed colors.
- The code treats these values as shared UI assets, not as throwaway recompositions.

## What vibe-on-android already has

- `ThemeShapes.kt` already defines a clear shape vocabulary for the app.
- `GeometryShapes.kt` already contains custom shapes, so the project is not afraid of non-standard surfaces.
- The widget layer already uses `LruCache`, which proves the app is comfortable with bounded caches.
- Current core surfaces still lean on standard rounded shapes, which keeps the UI simple and cheap to draw.

## What to adopt

1. If you start using smooth corner shapes in many repeated components, centralize them in one cache object.
2. Reuse the same radii for common tiers such as compact chips, cards, sheets, and pills.
3. Keep color and bitmap caches bounded. If the cache can grow without limit, it is a bug waiting to happen.
4. Only use custom path-based shapes where the shape itself matters. If a regular rounded corner is enough, use the simpler option.

## Suggested implementation targets

- Album cards and playlist cards.
- Chips, pills, and compact buttons.
- Dialog and sheet surfaces.
- Any list item that would otherwise create the same smooth outline on every item composition.

## Risks

- Smooth corner outlines are more expensive than plain rounded rectangles, so avoid creating them repeatedly inside item lambdas.
- Custom path-based shapes in `GeometryShapes.kt` should stay rare if they are not cached or reused.
- If a shape is only used once or twice, a cache adds complexity without much value.

## Recommended follow-up

- Introduce a shared smooth-shape cache only if the design system starts using those shapes broadly.
- Keep the existing `ThemeShapes` vocabulary as the default surface language.
- Treat `GeometryShapes.kt` as a place for special cases, not as a place to generate repeated outlines on demand.
