# EstateExpense Android App - AI Agent Guidelines

## Architecture Overview
- **Framework**: Jetpack Compose + Material3 for UI
- **Language**: Kotlin
- **Build System**: Gradle with version catalog (`gradle/libs.versions.toml`)
- **Package**: `com.santhomach.estateexpense`
- **Entry Point**: `MainActivity.kt` - extends `ComponentActivity`, uses `setContent` for Compose

## Key Components
- **UI Theme**: Located in `app/src/main/java/com/santhomach/estateexpense/ui/theme/`
  - `Theme.kt`: `EstateExpenseTheme` composable with dynamic color support (API 31+)
  - `Color.kt`: Custom color definitions (Purple80/40, PurpleGrey80/40, Pink80/40)
  - `Type.kt`: Typography using default Material3 styles
- **Main Activity**: `MainActivity.kt` - wraps content in `Scaffold` with `EstateExpenseTheme`

## Build Configuration
- **Compile SDK**: 36 (with minorApiLevel 1)
- **Min SDK**: 36
- **Target SDK**: 36
- **Java Compatibility**: 11
- **Compose Enabled**: `buildFeatures { compose = true }`
- **Dependencies**: Managed via BOM (`androidx.compose:compose-bom:2026.02.01`)

## Development Workflows
- **Build**: `./gradlew build`
- **Run Tests**: `./gradlew test` (unit) and `./gradlew connectedAndroidTest` (instrumented)
- **Clean**: `./gradlew clean`
- **Assemble Debug APK**: `./gradlew assembleDebug`

## Code Patterns
- **Composable Functions**: Use `@Composable` for UI components, e.g., `Greeting` in `MainActivity.kt`
- **Previews**: Add `@Preview` annotations for design-time previews, e.g., `GreetingPreview`
- **Theme Application**: Wrap composables in `EstateExpenseTheme { ... }`
- **Scaffold Usage**: Use `Scaffold` for basic layout with `innerPadding` handling
- **Color Schemes**: Prefer dynamic colors when available, fallback to custom light/dark schemes

## File Structure Conventions
- **Source Code**: `app/src/main/java/com/santhomach/estateexpense/`
- **Resources**: `app/src/main/res/` (standard Android resource folders)
- **Tests**: Unit in `app/src/test/`, instrumented in `app/src/androidTest/`
- **Gradle Config**: Root `build.gradle.kts` for plugins, app `build.gradle.kts` for module config

## Dependencies
- Core: `androidx.core:core-ktx`, `androidx.lifecycle:lifecycle-runtime-ktx`
- Compose: BOM-managed UI, Material3, Activity Compose integration
- Testing: JUnit 4.13.2, Espresso, Compose UI testing

## Notes
- High SDK requirements (36) suggest targeting cutting-edge Android features
- Version catalog used for dependency management - reference via `libs.library.name`
- No custom ProGuard rules (minify disabled in release)</content>
<parameter name="filePath">C:\Users\Sanal\AndroidStudioProjects\estateExpense\AGENTS.md
