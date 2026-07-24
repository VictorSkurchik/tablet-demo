# Tablet Demo

![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-API%2028%2B-3DDC84?logo=android&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2026.06.01-4285F4?logo=jetpackcompose&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9.6.1-02303A?logo=gradle&logoColor=white)
![Tests](https://img.shields.io/badge/Tests-98%20enforced-brightgreen)
![Coverage](https://img.shields.io/badge/Line%20coverage-95.35%25-brightgreen)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](./LICENSE)

Tablet Demo is a tablet-only Android application for generating, exploring, selecting, and editing a table of random string values. The project is implemented with Jetpack Compose and split into UI, domain, and data layers.

The tablet experience adapts at runtime to compact, medium, expanded, large,
and extra-large application windows, including resizes and separating foldable
hinges, while preserving form, table, and editor state during in-process
resizing and Activity recreation. Restoring a generated table after process
death remains outside the current scope. The manifest enforces a minimum 600 dp
smallest width without disabling resizing or multi-window support.

## Original requirements

The application will consist of 2 screens:

**First screen:**
Two fields in which we enter the number of rows and columns. (maximum limit - 6 columns and 1000 rows)


**Second screen:** 
We build a table, the size of which is specified on the first screen. We load random data into this table (data format is string format).
Single click on a cell should change the color of the cell (one click changes the color to green, another click returns the color back).  Double click allows to change the data in the cell. 

Technical requirements: **Only for tablets, Jetpack Compose, Modularization (UI, Domain, Data. Also, random data is taken from the data layer).**


## Screenshots

The screenshots below were recaptured on 24 July 2026 from the debug build on a
Medium Tablet API 35 emulator running Android 15 at 2560 × 1600.

### Setup

The setup screen validates the supported range before enabling table generation.

<p align="center">
  <img src="./docs/screenshots/setup-screen.png" alt="Setup screen" width="100%">
</p>

### Table and cell editor

The table screen displays an 8 × 4 data set, several selected cells, and the
active cell editor. The on-screen keyboard is dismissed so the screenshot
contains only the application UI.

<p align="center">
  <img src="./docs/screenshots/table-edit-screen.png" alt="Table screen with selected cells and the cell editor" width="100%">
</p>

## Architecture

The application follows a layered modular structure with dependencies pointing toward the domain layer.

| Module | Responsibility | Direct project dependencies |
| --- | --- | --- |
| `:app` | Application entry point, navigation, Android resources, and dependency-injection assembly | `:ui`, `:domain`, `:data` |
| `:ui` | Compose screens, adaptive tablet layout, reusable components, unidirectional state, and view models | `:domain` |
| `:domain` | Table models, limits, repository contract, validation, and generation use cases | None |
| `:data` | Random string generation and the repository implementation hidden behind the domain contract | `:domain` |
| `:benchmark` | Macrobenchmark scenario, maximum-table journey, and baseline profile generation | Targets `:app` |

```text
:benchmark ──targets──> :app
                        ├──> :ui ─────> :domain
                        ├──> :data ───> :domain
                        └─────────────> :domain
```

## Technology stack

- Kotlin 2.4.10 and Java 17 bytecode
- Android Gradle Plugin 9.3.1 and Gradle 9.6.1
- Jetpack Compose with the Compose BOM 2026.06.01
- Material 3 and Material 3 Adaptive supporting-pane layouts
- AndroidX WindowManager 1.5.1 for fold posture and separating-hinge information
- Navigation Compose with type-safe serializable destinations
- Kotlin coroutines, structured cancellation, lifecycle-aware immutable
  `StateFlow` screen state, and `TextFieldState` for high-frequency text input
- Kotlin immutable persistent collections for stable Compose inputs and
  logarithmic single-cell updates
- Koin for dependency injection
- JUnit 4, Compose UI Test, Espresso 3.7.0, Espresso Device 1.1.0,
  WindowManager Testing, and UI Automator
- JaCoCo for combined JVM and device-test coverage
- AndroidX Macrobenchmark and Baseline Profiles
- AndroidX Profile Installer 1.4.1
- KtLint and Detekt for static analysis
- LeakCanary in debug builds

## Adaptive UI contract

The UI follows the Android adaptive-app guidance within the supported
tablet-only distribution scope:

- `currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true)` is the single
  source of current window size classes and display features. Layout decisions
  do not use the physical device type or natural orientation.
- Material 3 Adaptive V2 width and height classes select the pane policy. A
  compact-height medium window stays single-pane; expanded and wider windows
  can show the main and supporting panes together.
- `SupportingPaneScaffold` provides canonical pane navigation and transitions.
  Separating vertical, horizontal, off-centre, multiple, and mixed hinges are
  excluded from pane content.
- Standard Activity recreation handles density and other configuration
  changes. `SavedStateHandle`, saveable navigation and scroll state,
  `TextFieldState.Saver`, and saveable focus restoration preserve input and
  editor continuity.
- Within its `sw600dp` tablet distribution scope, the application follows the
  window-size, posture, and continuity recommendations relevant to Android
  adaptive Tier 2. This is not a formal claim of full Tier 2 compliance across
  every screen type.

The implementation follows the official guidance for
[trifolds and landscape foldables](https://developer.android.com/develop/adaptive-apps/guides/foldables/trifolds-and-landscape-foldables),
[window size classes](https://developer.android.com/develop/adaptive-apps/guides/use-window-size-classes),
and [adaptive app quality](https://developer.android.com/docs/quality-guidelines/adaptive-app-quality/tier-2).

## Testing

The project contains 102 automated scenarios: 56 JVM unit tests, 42 device UI
tests, and 4 separately executed macrobenchmark or baseline-profile scenarios.
All 98 JVM and UI tests are enforced by the build and CI.

Implemented test types:

- Domain unit tests for table invariants and validation rules
- Data unit tests for random-string contracts, repository behavior, immutable
  output, and verified background-dispatcher use
- View-model unit tests for setup validation, table loading, failure/retry, in-flight
  cancellation, selection, and editing
- Unit tests for scroll-thumb geometry
- Dependency-injection graph verification
- Compose instrumentation tests for number fields, mouse/touch single and
  double clicks, Space/NumPad Enter/F2 cell interaction, compact setup
  validation, and the editor pane
- Accessibility contracts for localized errors, loading/error announcements, table collection semantics, selection state, gesture alternatives, keyboard traversal, touch targets, contrast, and large font scale
- Regression tests for keyboard-aware setup content and editor-pane height
- Adaptive-layout tests for compact, medium, expanded, large, and extra-large
  policies; medium-width/compact-height behavior; synthetic display resizing;
  injected tabletop and separating-hinge geometry; and state preservation
  across resize and Activity recreation
- End-to-end instrumentation journeys covering setup, table creation, selection,
  editing, localization, and access to the final cell of a 1,000 × 6 table
- Macrobenchmark and baseline-profile scenarios for a maximum-size 1,000 × 6 table

The suite focuses on invariants, state transitions, failure/cancellation paths,
adaptive behavior, and touch, mouse, and keyboard event paths. Thin delegation
and framework implementation details are intentionally not tested as
independent contracts.
Espresso Device display-size changes and injected WindowManager features verify
application logic deterministically; real fold/unfold, split-screen, freeform
resize, and outer/inner-display density transitions remain device-lab checks
rather than being overstated as emulator coverage.

Run all JVM and connected-device tests and enforce the combined coverage floors:

```bash
./gradlew coverageVerification --dependency-verification strict
```

### Continuous integration

The [CI workflow](./.github/workflows/ci.yml) runs for pull requests and pushes
targeting `develop` or `main`, and can also be started manually. It reports
three independent checks:

- `quality` validates the Gradle Wrapper, enforces strict dependency
  verification and lock files, runs KtLint, Detekt, all JVM unit tests, Android
  Lint, release R8/profile checks, and validates the merged release manifest
  security contract.
- `android-ui-tests` runs the application and UI instrumentation suites on an
  Android 15 Pixel Tablet emulator, enforces the combined coverage floors, and
  executes the minified maximum-table macrobenchmark.
- `compatibility-smoke` builds and opens a valid table on tablet emulators at
  the supported API boundary (API 28) and the current compile API (API 37).

The `baseline-profile` workflow runs for pull requests, pushes to `develop` or
`main`, manual dispatches, and a weekly schedule; it regenerates both profiles
and fails on a stale checked-in result. The manually dispatched
`physical-benchmark` workflow accepts only `main`, requires the
`physical-benchmark` environment, and runs macrobenchmarks on a dedicated
self-hosted tablet. Repository administrators must configure environment
protection and reviewers, restrict the runner group to this repository and
workflow, and keep the runner isolated and clean between jobs.

All three CI checks should be required by the `main` branch ruleset before
merging.

### Coverage

The generated report is available at `build/reports/jacoco/coverageReport/html/index.html`.

| Coverage metric | Result |
| --- | ---: |
| Instructions | 93.86% |
| Lines | 95.35% |
| Branches | 78.96% |
| Methods | 91.25% |
| Classes | 98.72% |

The build fails below 87% instructions, 73% branches, 88% lines, 84% methods,
or 96% classes.

## Security

- The release application requests no network or dangerous runtime
  permissions. Cleartext traffic is explicitly disabled.
- Android Auto Backup and device-to-device transfer are denied for every
  storage domain. The release gate checks both the merged manifest and the data
  extraction rules.
- `verifyReleaseArtifacts` allowlists release permissions and exported
  components, rejects debuggable/test-only/shell-profileable output, and checks
  R8 mapping, startup optimization, and packaged Baseline Profiles in the APK
  and AAB.
- All resolvable project configurations use strict dependency locking;
  per-module and settings lock files are checked in together with SHA-256
  dependency verification metadata. Repository content filters reduce
  cross-repository substitution, and CI validates the Gradle Wrapper.
- GitHub Actions are pinned to immutable commit SHAs with persisted checkout
  credentials disabled. Dependabot monitors both Gradle and Actions
  dependencies.
- Unsigned release validation artifacts are uploaded separately on successful
  CI runs with three-day retention. Release signing and store publishing remain
  outside this repository.

When intentionally changing dependencies, regenerate locks and checksums on a
trusted machine, review the diff, and then rerun the strict build:

```bash
./gradlew test lint :app:verifyReleaseArtifacts \
  :app:assembleDebugAndroidTest :ui:assembleDebugAndroidTest \
  :benchmark:assembleBenchmark \
  --write-locks --write-verification-metadata sha256 \
  --no-configuration-cache

./gradlew ktlintCheck detekt test lint :app:verifyReleaseArtifacts \
  :app:assembleDebugAndroidTest :ui:assembleDebugAndroidTest \
  :benchmark:assembleBenchmark \
  --dependency-verification strict
```

## Benchmarks

### Physical Samsung SM-S931B — 24 July 2026

A real-device debug/release pass was performed on the connected Samsung
SM-S931B running Android 16 (API 36, arm64-v8a). The physical panel was running
at 120 Hz. For the test only, ADB simulated a portrait tablet display:

| Display state | `wm size` | `wm density` | Effective window |
| --- | --- | --- | --- |
| Before the test | Physical 1080 × 2340, no size override | Physical 480, override 450 | Phone profile |
| Tablet test profile | Override 1600 × 2560 | Override 320 | `sw800dp`, 800 × 1280 dp |
| After the test | Physical 1080 × 2340, no size override | Physical 480, override 450 | Original phone profile |

Automatic rotation was also restored to its recorded values
(`wm user-rotation free`, `accelerometer_rotation=1`, `user_rotation=0`).
The temporary device-side trace directory was removed after the run.

#### Builds and installation

The debug, release, benchmark application, and benchmark test APKs all built
successfully. The unsigned release APK was post-signed for this local device
run with the default Android debug keystore:

| Variant | Installed artifact | Verification | Single cold launch |
| --- | --- | --- | ---: |
| Debug | `app/build/outputs/apk/debug/app-debug.apk` | Debuggable; includes JaCoCo and LeakCanary | 562 ms |
| Release | `app/build/outputs/apk/release/app-release-default-keystore.apk` | Zipaligned, APK Signature Scheme v3, not debuggable or profileable | 219 ms |

The release test certificate is the standard `androiddebugkey` from
`$HOME/.android/debug.keystore` (`CN=Android Debug`, SHA-256
`3bbb90a6111f70acb742c7c148b5b953b21cca1614fca10c938695603a4b5562`).
This is a local test artifact, not a production signing configuration. The
cold-launch figures above are single Activity Manager samples and should not be
treated as a statistical startup benchmark.

#### Maximum-table journey

Both installed variants manually completed the same journey: enter 1,000 rows
and 6 columns, build the table, verify the `Table · 1000 × 6` screen, and
perform five rapid vertical swipes inside the grid.

#### Isolated rapid-scroll measurement

`dumpsys gfxinfo` was reset immediately before the five-swipe burst. The
release figures are the meaningful product result; debug is shown to quantify
the expected cost of coverage and leak-detection instrumentation.

| Frame metric | Debug | Signed minified release |
| --- | ---: | ---: |
| Frames rendered | 24 | 340 |
| Janky frames | 18 (75.00%) | 21 (6.18%) |
| P50 | 53 ms | 7 ms |
| P90 | 121 ms | 16 ms |
| P95 | 125 ms | 22 ms |
| P99 | 150 ms | 40 ms |

| Process memory | Debug, before → after | Release, before → after |
| --- | ---: | ---: |
| Total PSS | 229,181 → 253,800 KB | 160,988 → 125,197 KB |
| Total RSS | 360,148 → 386,120 KB | 293,720 → 259,732 KB |

The release run does not show a leak signal: memory fell after the burst as
graphics caches were reclaimed. The release median fits the 8.33 ms frame
budget of a 120 Hz display, but P90/P95 do not, and 6.18% jank is slightly above
the report's aggressive ≤5% target. The maximum-table scroll is therefore
usable and dramatically better than debug, but it is not yet a clean 120 Hz
result; the long-tail frames remain the optimization target.

#### Physical-device Macrobenchmark

`TableMacrobenchmark.maximumTableStartupAndScroll` passed all five cold
iterations. The rule uses `CompilationMode.Partial()` and ART confirmed
`speed-profile` compilation. These frame metrics cover the complete mixed
journey—cold startup, input, construction of 6,000 cells, and scrolling—so they
must not be read as scroll-only numbers.

| Startup metric, 5 runs | Minimum | Median | Maximum |
| --- | ---: | ---: | ---: |
| Time to initial display | 153.6 ms | 154.6 ms | 169.1 ms |
| Time to full display | 1,760.9 ms | 1,813.1 ms | 1,821.3 ms |
| Measured frame count | 290 | 293 | 299 |

| Frame metric | P50 | P90 | P95 | P99 |
| --- | ---: | ---: | ---: | ---: |
| CPU frame duration | 4.9 ms | 7.6 ms | 9.7 ms | 43.5 ms |
| Frame overrun | −0.1 ms | 2.6 ms | 7.6 ms | 46.7 ms |

Frame count was stable across runs (1.32% coefficient of variation), while the
positive P90 frame overrun and high P99 confirm a tail-jank problem under the
combined worst-case journey. Logcat also contains Adreno Vulkan shader
compilation failures during the cold iterations; they correlate with the run
but are not, by themselves, proof of the root cause.

The source JSON and five generated Perfetto traces are under
`benchmark/build/outputs/connected_android_test_additional_output/benchmarkRelease/connected/SM-S931B - 16/`.

#### Recomposition assessment

An exact per-composable recomposition count is intentionally not claimed. The
recorded benchmark trace contains frame, CPU, scheduler, and rendering data,
but this build did not expose Compose runtime-tracing names, and a Layout
Inspector counter capture was not completed. Frame count is not a valid proxy
for recomposition count.

For this screen, normal scrolling would keep the route, scaffold, and table
container at zero recompositions after reset; newly entering lazy-grid cells
may be composed, while the scroll indicator should redraw without recomposing
the whole table. Consequently, per-composable recomposition normality remains
unverified. The measured release frame tail already shows that the
maximum-load path deserves further profiling even though no runaway memory
growth was observed.

#### Test outcome and benchmark regression

- Manual debug and signed-release journeys passed without a crash or ANR.
- The physical-device Macrobenchmark passed (1 test, 5 measured iterations).
- The two Compose `AppJourneyTest` smoke journeys timed out after 5 seconds
  while waiting for the table title on this One UI/API 36 profile. Manual
  execution of the same journeys succeeded, and logcat contained no app crash
  or ANR, so this is recorded as a physical test-harness failure rather than an
  application failure. The HTML report is at
  `app/build/reports/androidTests/connected/debug/index.html`.
- The portrait two-pane profile exposed a Macrobenchmark gesture bug: the
  swipe used the full display height rather than the table bounds.
  `TableJourney.flingTable()` now derives both Y coordinates from the
  scrollable grid's visible bounds. The physical Macrobenchmark passed after
  that correction.

### Pixel Tablet emulator reference

The reference below was remeasured on 24 July 2026 on a Pixel Tablet emulator
running Android 15 at 2560 × 1600 and 60 Hz, with system animations disabled.

#### Debug build

The development build includes JaCoCo and LeakCanary and does not use R8 optimization.

| Cold startup | Average | Median |
| --- | ---: | ---: |
| Activity Manager, 5 runs | 819 ms | 765 ms |

#### Release build

The release-like build uses R8, resource shrinking, `CompilationMode.Partial`, the Baseline Profile, and the Startup Profile. ART confirmed `speed-profile` compilation, and R8 marked the primary `classes.dex` as startup-optimized.

##### Setup screen cold startup

| Startup metric, 10 runs | Minimum | Median | Maximum |
| --- | ---: | ---: | ---: |
| Time to initial display | 104.4 ms | 119.5 ms | 196.3 ms |

##### Maximum table startup and scrolling

| Startup metric, 5 runs | Minimum | Median | Maximum |
| --- | ---: | ---: | ---: |
| Time to initial display | 107.1 ms | 121.9 ms | 161.2 ms |
| Time to full display | 1,400.5 ms | 1,415.7 ms | 2,187.9 ms |
| Measured frame count | 95 | 98 | 122 |

| Frame metric | P50 | P90 | P95 | P99 |
| --- | ---: | ---: | ---: | ---: |
| CPU frame duration | 4.0 ms | 7.2 ms | 13.6 ms | 39.5 ms |
| Frame overrun | −11.1 ms | −7.5 ms | −1.1 ms | 26.0 ms |

Regenerate the checked-in Baseline and Startup Profiles with:

```bash
./gradlew :app:generateReleaseBaselineProfile \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile \
  --dependency-verification strict
```

Run all macrobenchmarks on a connected tablet with:

```bash
./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark \
  --dependency-verification strict
```

## Out of Scope

The following improvements are intentionally outside the current requirements:

- Process-death restoration for the generated table, edits, and selected cells
- Persistent storage, import/export, and sharing of table data
- Undo/redo, copy/paste, bulk selection, sorting, filtering, and search
- Phone-first and desktop-first layouts
- Firebase services and integrations
- Third-party product analytics (for example, Amplitude)
- Release signing and store publishing

## Git workflow

The project uses a feature-based Git Flow:

1. Create `feature/<name>` from `develop`.
2. Keep commits focused and use Conventional Commits.
3. Push the feature branch and merge it into `develop` with a dedicated merge commit.
4. Verify and push `develop`.
5. Open a pull request from `develop` to `main` and merge it after the required CI checks pass.

Feature branches are never merged directly into `main`, and shared branches must not be force-pushed.

## Build and verification

Build the debug APK:

```bash
./gradlew :app:assembleDebug --dependency-verification strict
```

Build the unsigned release APK and the optimized benchmark APK:

```bash
./gradlew :app:assembleRelease :app:assembleBenchmark \
  --dependency-verification strict
```

Run static analysis:

```bash
./gradlew ktlintCheck detekt lint --dependency-verification strict
```

Build and validate the release APK, AAB, R8 mapping, and packaged profile:

```bash
./gradlew :app:verifyReleaseArtifacts --dependency-verification strict
```

## License

Copyright © 2026 Victor Skurchik.

Licensed under the [Apache License 2.0](./LICENSE).
