# Tablet Demo

![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-API%2028%2B-3DDC84?logo=android&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2026.06.01-4285F4?logo=jetpackcompose&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9.6.1-02303A?logo=gradle&logoColor=white)
![Tests](https://img.shields.io/badge/Tests-96%20passing-brightgreen)
![Coverage](https://img.shields.io/badge/Line%20coverage-95.3%25-brightgreen)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](./LICENSE)

Tablet Demo is a tablet-only Android application for generating, exploring, selecting, and editing a table of random string values. The project is implemented with Jetpack Compose and split into UI, domain, and data layers.

The tablet experience adapts at runtime to compact and expanded window sizes,
split/multi-window resizing, tabletop posture, and separating foldable hinges
while preserving form, table, and editor state.

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
| `:ui` | Compose screens, adaptive tablet layout, reusable components, MVI state, and view models | `:domain` |
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
- Kotlin coroutines, structured cancellation, and a single immutable `StateFlow` MVI state
- Koin for dependency injection
- JUnit 4, Turbine, Compose UI Test, Espresso 3.7.0, Espresso Device 1.1.0,
  WindowManager Testing, and UI Automator
- JaCoCo for combined JVM and device-test coverage
- AndroidX Macrobenchmark and Baseline Profiles
- AndroidX Profile Installer 1.4.1
- KtLint and Detekt for static analysis
- LeakCanary in debug builds

## Testing

The project contains 100 automated scenarios: 59 JVM unit tests, 37 device UI
tests, and 4 separately executed macrobenchmark or baseline-profile scenarios.
All 96 JVM and UI tests are enforced by the build.

Implemented test types:

- Domain unit tests for table models, validation rules, and generation use cases
- Data unit tests for random strings and repository behavior
- View-model unit tests for setup validation, table loading, failure/retry, in-flight
  cancellation, selection, and editing
- Unit tests for scroll-thumb geometry
- Dependency-injection graph verification
- Compose instrumentation tests for number fields, mouse/touch and Enter/F2 cell
  interaction, compact setup validation, and the editor pane
- Accessibility contracts for localized errors, loading/error announcements, table collection semantics, selection state, gesture alternatives, keyboard traversal, touch targets, contrast, and large font scale
- Regression tests for keyboard-aware setup content and editor-pane height
- Adaptive-layout tests for compact/expanded resizing, tabletop posture,
  multiple and mixed separating hinges, and state preservation across
  multi-window and split-window transitions
- End-to-end instrumentation journeys covering setup, table creation, selection,
  editing, localization, and access to the final cell of a 1,000 × 6 table
- Macrobenchmark and baseline-profile scenarios for a maximum-size 1,000 × 6 table

Run all JVM and connected-device tests and enforce the combined coverage floors:

```bash
./gradlew coverageVerification
```

### Continuous integration

The [CI workflow](./.github/workflows/ci.yml) runs for pull requests and pushes targeting `develop` or `main`, and can also be started manually. It reports two independent checks:

- `quality` runs KtLint, Detekt, all JVM unit tests, Android Lint, release R8
  verification, and checks that the APK/AAB contain a Baseline Profile.
- `android-ui-tests` runs the application and UI instrumentation suites on an
  Android 15 Pixel Tablet emulator and enforces the combined coverage floors.

The scheduled `baseline-profile` workflow regenerates profiles and fails on a
stale checked-in result. The manually dispatched `physical-benchmark` workflow
runs macrobenchmarks on a dedicated self-hosted tablet.

Both checks should be required by the `main` branch ruleset before merging.

### Coverage

The generated report is available at `build/reports/jacoco/coverageReport/html/index.html`.

| Coverage metric | Result |
| --- | ---: |
| Instructions | 93.46% |
| Lines | 95.26% |
| Branches | 78.50% |
| Methods | 90.40% |
| Classes | 98.73% |

The build fails below 87% instructions, 73% branches, 88% lines, 84% methods,
or 96% classes.

<p align="center">
  <img src="./docs/screenshots/test-coverage.png" alt="Combined JaCoCo coverage summary" width="100%">
</p>

## Benchmarks

Remeasured on 24 July 2026 on a Pixel Tablet emulator running Android 15 at
2560 × 1600 and 60 Hz, with system animations disabled.

### Debug build

The development build includes JaCoCo and LeakCanary and does not use R8 optimization.

| Cold startup | Average | Median |
| --- | ---: | ---: |
| Activity Manager, 5 runs | 715 ms | 718 ms |

### Release build

The release-like build uses R8, resource shrinking, `CompilationMode.Partial`, the Baseline Profile, and the Startup Profile. ART confirmed `speed-profile` compilation, and R8 marked the primary `classes.dex` as startup-optimized.

#### Setup screen cold startup

| Startup metric, 10 runs | Minimum | Median | Maximum |
| --- | ---: | ---: | ---: |
| Time to initial display | 105.9 ms | 115.9 ms | 247.9 ms |

#### Maximum table startup and scrolling

| Startup metric, 5 runs | Minimum | Median | Maximum |
| --- | ---: | ---: | ---: |
| Time to initial display | 114.1 ms | 145.6 ms | 182.3 ms |
| Time to full display | 1,502.6 ms | 1,578.0 ms | 1,652.0 ms |
| Measured frame count | 105 | 110 | 132 |

| Frame metric | P50 | P90 | P95 | P99 |
| --- | ---: | ---: | ---: | ---: |
| CPU frame duration | 5.2 ms | 10.4 ms | 20.7 ms | 39.9 ms |
| Frame overrun | −9.8 ms | −1.4 ms | 12.1 ms | 31.0 ms |

Regenerate the checked-in Baseline and Startup Profiles with:

```bash
./gradlew :app:generateReleaseBaselineProfile \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile
```

Run all macrobenchmarks on a connected tablet with:

```bash
./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark
```

## Out of Scope

The following improvements are intentionally outside the current requirements:

- Process-death restoration for the generated table, edits, and selected cells
- Persistent storage, import/export, and sharing of table data
- Undo/redo, copy/paste, bulk selection, sorting, filtering, and search
- Phone-first and desktop-first layouts
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
./gradlew :app:assembleDebug
```

Build the unsigned release APK and the optimized benchmark APK:

```bash
./gradlew :app:assembleRelease :app:assembleBenchmark
```

Run static analysis:

```bash
./gradlew ktlintCheck detekt lint
```

Build and validate the release APK, AAB, R8 mapping, and packaged profile:

```bash
./gradlew :app:verifyReleaseArtifacts
```

## License

Copyright © 2026 Victor Skurchik.

Licensed under the [Apache License 2.0](./LICENSE).
