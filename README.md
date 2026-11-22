# GreenAudit

An offline-first Android app for surveying city parks: browse parks on a list and a map, draw a new park's boundary by tapping its corners, check in only when you're physically inside it with a good GPS fix, record a survey, and take a stamped photo — all of it working in airplane mode and syncing later.

## Build and run

```
./gradlew assembleDebug
```

Builds cleanly on a fresh checkout — no API key, secret, or manual file is required. The map uses [osmdroid](https://github.com/osmdroid/osmdroid) (OpenStreetMap tiles), which is keyless, specifically so a clean checkout never needs a `local.properties` entry or a CI secret to show a working map.

Install and run: `./gradlew installDebug`, or open the project in Android Studio and run the `app` configuration. minSdk 26, targetSdk 36.

### Demo account

```
Email:    surveyor@greenaudit.app
Password: GreenAudit123
```

This is the only account the mock auth accepts (see `domain/auth/DemoCredentials.kt`). The session is persisted in DataStore and survives an app restart; logging out clears it and wipes all local park/survey/photo data (then reseeds the demo parks, so the app isn't left empty for the next login).

### Testing the geofenced check-in

The 22 seeded demo parks are real Jaipur park locations (see "What I'd do next" — a fixed city was the simplest way to seed "real boundary coordinates," but it means you can only check in on a *seeded* park if you are physically in Jaipur. To test the check-in/survey/camera flow from anywhere else: tap **+** on the parks screen, let the map center on your own GPS fix (there's a my-location button if it doesn't), tap out a small boundary around yourself, save it, then open it and check in — you'll be inside the park you just drew.

## Architecture

```
ui/            Jetpack Compose screens + per-screen ViewModels (Hilt-injected)
domain/
  gis/         Pure-Kotlin geodesic math: distance, bearing, area, perimeter,
               point-in-polygon. No Android dependency - unit-testable on the JVM.
  survey/      CheckInEligibilityUseCase - the "can I start a survey here" rule.
  repository/  Repository interfaces the UI programs against.
  auth/        Demo credential constant.
data/
  local/       Room: entities, DAOs, AppDatabase, enum/polygon TypeConverters,
               first-run seed data.
  repository/  Repository implementations - wrap DAOs, compute area/perimeter
               via GeoMath at write time, stamp new/changed rows PENDING_*.
  remote/      SyncApi interface + FakeSyncApi (see "Offline & sync" below).
  sync/        SyncManager - pushes PENDING_* rows through SyncApi.
  auth/        SessionManager (DataStore-backed login session).
  location/    LocationTracker - Fused Location Provider wrapped as a Flow.
  photo/       PhotoStampUtil - burns the stamp into a captured JPEG.
di/            Hilt modules wiring all of the above.
```

Layering: `ui` depends on `domain` (interfaces + pure math) and on Hilt-provided `data` implementations; `domain` has no dependency on `data` or `ui`. Within `data`, entities are reused directly as the type repositories expose (no separate "domain model" layer) — a deliberate scope cut for a few-day project; see below.

Each required screen maps to one ViewModel + one or more composables under `ui/`: `login`, `parks` (List and Map are tabs of one screen, per the wireframe), `mappark` (draw a new park), `survey` (check-in + form), `camera` (CameraX capture).

## How area, direction, and "inside the park" are calculated

All of it lives in `domain/gis/GeoMath.kt`, `PlanarProjection.kt`, `PointInPolygon.kt`, `AreaUnits.kt` — pure Kotlin, covered by the four required unit-test suites under `app/src/test/.../domain/`.

- **Distance & bearing**: haversine great-circle distance, and initial bearing bucketed into one of 8 compass points (e.g. "210 m, north-east" in the map bottom sheet).
- **Area & perimeter**: the spherical-excess formula (same family as Google Maps Utils' `SphericalUtil.computeArea`) for area, and haversine summed edge-by-edge for perimeter — not flat shoelace-on-degrees, which is wrong off the equator since a degree of longitude shrinks by `cos(latitude)`.
- **Point-in-polygon**: the boundary is projected onto a local tangent plane (equirectangular projection around the polygon's first point, in `PlanarProjection.kt`) and a standard ray-casting test runs on the flat result. This keeps the inside/outside check and the "how far outside" distance (`distanceToPolygonBoundaryMeters`, used in the check-in gate's message) consistent with each other.
- **Check-in rule** (`CheckInEligibilityUseCase`): eligible only if point-in-polygon is true **and** GPS accuracy is ≤25 m; if outside, the message reports distance to the nearest edge rather than a generic "no."
- **Self-intersection rejection (bonus, optional)**: `PolygonSelfIntersection.kt` checks every pair of non-adjacent edges of a drawn boundary for a crossing (same tangent-plane projection as point-in-polygon). Wired into the map-a-new-park screen — "Save park" is disabled with a warning while the boundary crosses itself.

## Offline & sync

Every write (`ParkRepository.saveNewPark`, `SurveyRepository.checkIn`/`submit`, `PhotoRepository.addPhoto`) goes to Room first and is stamped `SyncState.PENDING_CREATE`/`PENDING_UPDATE`. Nothing in the app ever blocks on a network call, so everything above — mapping a park, checking in, filling a survey, taking photos — works identically in airplane mode.

`SyncManager.syncNow()` (wired to the "Sync now" / sync icon on the parks screen) reads every row with a non-`SYNCED` state and pushes it through `SyncApi`. There's no real backend: `FakeSyncApi` stands in for one, reading `BuildConfig.BASE_URL` (set per build type, never hardcoded in Kotlin) and simulating a real network — a random 250–900 ms delay and a ~10% induced failure — before flipping the row to `SYNCED` on success. A failed push just leaves the row pending for the next "Sync now".

**Background sync (bonus, optional)**: every write that leaves a row `PENDING_*` (`saveNewPark`, `updateStatus`, `markSurveyed`, `checkIn`, `submit`, `addPhoto`) also calls `BackgroundSyncScheduler.scheduleSync()`, which enqueues a `SyncWorker` (a `CoroutineWorker` wired through Hilt's `HiltWorkerFactory`, see `GreenAuditApp.workManagerConfiguration`) with a `NetworkType.CONNECTED` constraint via `WorkManager`. Because it's enqueued with WorkManager rather than held in memory, it survives the app being backgrounded, killed, or the device rebooting, and fires `SyncManager.syncNow()` automatically the moment connectivity returns — no need to tap "Sync now" at all unless you want to force an immediate attempt.

**Conflict policy** (same park edited on two phones while both are offline): last-write-wins by `updatedAtMillis` — whichever device's "Sync now" reaches the (fake) backend last overwrites the other's copy. This is the cut I made deliberately rather than building real merge/CRDT logic: it's simple, predictable, and matches what the brief asks for ("explain how conflicts work," not "solve them perfectly"). With more time, I'd move to a per-field last-write-wins (so two surveyors editing different fields of the same park don't clobber each other) or a small CRDT for the boundary/status fields specifically.

## What I cut / would do next with more time

Required items 1–6 are all implemented and were walked end-to-end on a physical device (see commit history), and are the main basis for review per the brief ("four features done well beat seven half-done"). Two items from the **optional bonus list** are also done, attempted only after 1–6 were solid: rejecting self-intersecting boundaries (`domain/gis/PolygonSelfIntersection.kt`, wired into the map-a-new-park screen) and automatic WorkManager background sync (above). No instrumented/Robolectric test was added for `SyncWorker` — the project's test setup is pure-JVM unit tests, and a `WorkManagerTestInitHelper` harness felt out of scope for a bonus feature; this is a deliberate cut, not an oversight.

What I'd add next, roughly in priority order:
- **Walk mode** and the remaining bonuses (GeoJSON export, offline map tiles, dark mode) — not attempted; the brief is explicit that 1–6 solid beats 1–6 plus half-done bonuses.
- **A proper domain model layer** separate from the Room entities (entities are reused directly as what repositories return — fine for an app this size, but would start to hurt if the schema needed to diverge from what the UI wants).
- **DAO/repository/sync instrumented tests** — the four required unit-test suites (area/perimeter, point-in-polygon, distance/bearing, check-in rule) are done and green; Room/Hilt test coverage for the newer data layer is the next thing I'd add.
- **A GitHub Actions workflow** building the APK and running tests on every push (listed as a bonus).
- Instrumented/Robolectric coverage for `SyncWorker` and the WorkManager scheduling path.
- Seeding from the device's *actual* starting city instead of a fixed Jaipur list, so the "check in on a seeded park" path works for anyone, anywhere, out of the box.

## AI use

**Tool:** [Claude Code](https://claude.com/claude-code) (Anthropic), used throughout this project.

- **Architecture and planning** — package layout (`ui`/`domain`/`data`/`di`), Hilt module boundaries, the map SDK choice (osmdroid, to avoid an API key), and the sync/conflict design were worked out with Claude Code before writing code.
- **Implementation** — most of the Kotlin/Compose code (Room entities/DAOs, repositories, ViewModels, Compose screens, the osmdroid map integration, CameraX capture, the photo-stamping utility) was generated by Claude Code and then compiled, run, and fixed as issues came up, rather than taken on faith.
- **On-device debugging** — the app was installed and walked through end-to-end on a physical device (login, draw a park, check in, submit a survey, take a stamped photo, sync, logout), which surfaced three real bugs (two FloatingActionButton/layout overlaps and a map that couldn't find the user's own location) that are fixed in the commit history.
- **This README** was also written by Claude Code, from the actual code and the on-device test results.
- The pre-existing GIS domain layer (haversine distance/bearing, geodesic area/perimeter, point-in-polygon, the check-in eligibility rule) and its four unit-test suites were built the same way, in an earlier session.

