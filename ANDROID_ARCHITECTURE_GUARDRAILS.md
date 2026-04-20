# Android Architecture Guardrails

This checklist tracks architectural rules introduced during the phased migration.

## DI Guardrails

- Use one DI entry strategy per feature path. Current migration uses Koin bootstrap with existing Hilt compatibility.
- New ViewModels should prefer explicit constructor dependencies and be registered in Koin modules.
- Avoid adding new ad-hoc singletons in UI code.

## Navigation Guardrails

- Prefer route models/constants in `ui/navigation/Routes.kt` over raw string literals.
- Keep navigation wiring inside `ui/navigation/*` and pass callbacks to screens.

## Presentation (MVI) Guardrails

- New screen slices should define:
  - `<Screen>State`
  - `<Screen>Action`
  - `<Screen>Event`
  - `<Screen>ViewModel`
  - `<Screen>Root` and `<Screen>` split
- One-time side effects should use event streams and `ObserveAsEvents`.
- Prefer lifecycle-aware flow collection (`collectAsStateWithLifecycle`).

## Data/Error Guardrails

- Repositories should return typed `Result<Success, Error>` and `EmptyResult<Error>`.
- Map user-facing errors to `UiText`.
- Do not throw expected operational errors for normal control flow.

## Testing Guardrails

- Unit tests should use:
  - JUnit5
  - Turbine for flow/state assertions
  - AssertK for assertions
  - coroutines-test dispatchers
- Keep JUnit4 compatibility until legacy tests are fully migrated.

## Verification Commands

- Build debug APK: `./gradlew.bat :app:assembleDebug`
- Run unit tests: `./gradlew.bat :app:testDebugUnitTest`
- Install debug APK: `./gradlew.bat installDebug`
