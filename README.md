# Compose Driver

**Make AI Agents see and control your Compose UI.**

Compose Driver enables AI agents and automated tools to interact with any Jetpack Compose UI (
Android and Desktop) through a simple HTTP API. It wraps your Composable in a test harness, exposing
a server that translates HTTP requests into `ComposeUiTest` actions.

**Try it out:** Clone this repository and open the [sample/](sample/) directory with your favorite
AI coding tool.

## Features

- **Cross-Platform**: Supports Android and JVM (Desktop) Compose.
- **Zero Code Changes**: Integrates via a Gradle Settings plugin. No production code changes
  required.
- **AI-Native API**: REST-like API designed for agents to "see" (screenshot/tree) and "act"
  (click/swipe).
- **Observability**: Record GIFs of interactions and capture screenshots on demand.
- **Lightning Fast**: Uses virtual clock time on the host, executing complex flows in tens of
  milliseconds.
- **Headless**: Does not require a browser, GUI or device, making it perfect for background or cloud
  agents.

## Installation

Add the plugin to your `settings.gradle.kts`.

```kotlin
plugins {
    id("io.github.jdemeulenaere.compose.driver") version "0.5.0"
}

composeDriver {
    // Enable the platforms you need
    android()
    desktop()
}
```

The plugin will automatically create two new subprojects (`:compose-driver-android` and
`:compose-driver-desktop`) in your build, configured to run your Composables within the driver
environment. `:compose-driver-android` will depend on all your Android and Multiplatform subprojects
that use Compose, and `:compose-driver-desktop` will depend on all JVM and Multiplatform subprojects
that use Compose.

## Configuration

You can customize the generated driver projects using the `composeDriver` block.

```kotlin
composeDriver {
    android {
        name = "compose-driver-android"
        robolectric {
            sdk = 36
            qualifiers = "w410dp-h920dp-xhdpi" // see https://robolectric.org/device-configuration/
        }

        // Manually add dependencies (e.g. Compose BOM)
        dependencies {
            add("implementation", platform("androidx.compose:compose-bom:2025.01.00"))
        }

        // Resolve dependency ambiguity for flavored projects (e.g. "nowinandroid")
        missingDimensionStrategy("contentType", "demo")
    }
    desktop {
        name = "compose-driver-desktop"
        width = 1024
        height = 768
        density = 1.0f
    }
}
```

## Usage

### 1. Run the Driver

To start the driver, use the generated `run` task. You must specify the Composable you want to drive
using the `compose.driver.composable` system property.

**Desktop**

```bash
./gradlew :compose-driver-desktop:run -Dcompose.driver.composable=com.example.app.MainKt.MainScreen
```

**Android**

```bash
./gradlew :compose-driver-android:run -Dcompose.driver.composable=com.example.app.MainKt.MainScreen
```

> [!NOTE]
> Android runs via Robolectric and starting the server might take a few seconds. For this reason, I
> recommend using the Desktop driver when working on multiplatform code.

### 2. Interact via HTTP

Once running, the server listens at `http://localhost:8080`.

**Example: Automated Login Flow**

```bash
# 1. Wait for login screen
curl "http://localhost:8080/waitForNode?nodeTag=login_screen"

# 2. Input credentials
curl "http://localhost:8080/textInput?nodeTag=username&text=admin"
curl "http://localhost:8080/textInput?nodeTag=password&text=secret"

# 3. Click on the "Login" button and record the transition (requires ffmpeg)
curl "http://localhost:8080/click?nodeText=login&nodeTextIgnoreCase=true&nodeTextSubstring=true&gifDurationMs=2000" > login_flow.gif
```

## API Reference

> [!IMPORTANT]
> Most endpoints accept node selection parameters:
> - `nodeTag`: Targets a specific `Modifier.testTag()`.
> - `nodeText`: Matches a node by its text content.
> - `nodeTextSubstring`: If `true`, `nodeText` matches as a substring (default: `false`, exact
    match).
> - `nodeTextIgnoreCase`: If `true`, `nodeText` matching is case-insensitive (default: `false`).
>
> If both `nodeTag` and `nodeText` are provided, the node must match **both**. If neither is
> provided,
> the action applies to the root node.

### Core & Observability

| Method | Endpoint       | Params    | Description                                                  |
|:-------|:---------------|:----------|:-------------------------------------------------------------|
| `GET`  | `/status`      |           | Check if server is ready. Returns "ok".                      |
| `GET`  | `/printTree`   |           | Returns the semantic node tree as text.                      |
| `GET`  | `/screenshot`  |           | Returns a PNG screenshot of the target node or root.         |
| `GET`  | `/waitForIdle` |           | Waits for the UI to be idle (no pending changes).            |
| `GET`  | `/waitForNode` | `timeout` | Waits for a matching node to exist. **Default timeout:** 5s. |

### Interaction

| Method | Endpoint           | Params           | Description                       |
|:-------|:-------------------|:-----------------|:----------------------------------|
| `GET`  | `/click`           |                  | Click a node.                     |
| `GET`  | `/doubleClick`     |                  | Double-click a node.              |
| `GET`  | `/longClick`       |                  | Long-press a node.                |
| `GET`  | `/textInput`       | **`text`** (req) | Enter text into a field.          |
| `GET`  | `/textReplacement` | **`text`** (req) | Replace existing text in a field. |
| `GET`  | `/textClearance`   |                  | Clear text from a field.          |
| `GET`  | `/navigateBack`    |                  | Trigger the system "Back" action. |
| `GET`  | `/scrollTo`        |                  | Scrolls to the node.              |

### Gestures

| Method | Endpoint               | Params                          | Description                             |
|:-------|:-----------------------|:--------------------------------|:----------------------------------------|
| `GET`  | `/swipe`               | **`direction`** (req)           | Swipe `UP`, `DOWN`, `LEFT`, or `RIGHT`. |
| `GET`  | `/pointerInput/down`   | **`x`, `y`** (req), `pointerId` | Send pointer down event at (x,y).       |
| `GET`  | `/pointerInput/moveBy` | **`x`, `y`** (req), `pointerId` | Move pointer by delta (x,y).            |
| `GET`  | `/pointerInput/moveTo` | **`x`, `y`** (req), `pointerId` | Move pointer to absolute (x,y).         |
| `GET`  | `/pointerInput/up`     | `pointerId`                     | Send pointer up event.                  |

### Lifecycle

| Method | Endpoint | Params       | Description                                                            |
|:-------|:---------|:-------------|:-----------------------------------------------------------------------|
| `GET`  | `/reset` | `composable` | Reset the UI state. Optionally switch to a different Composable class. |

### GIF generation

All endpoints (like `/click`, `/swipe`, etc.) also **accept an optional `gifDurationMs`
parameter**. If provided, the server will record a GIF of the interaction for the specified
duration (max 5,000 ms) and return it instead of the standard "ok" response.

> [!NOTE]
> **Note**: This feature requires `ffmpeg` to be installed on the host machine and available in the
> system PATH.