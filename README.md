# Compose Driver

Compose Driver is a tool designed to allow AI tools (and other external agents) to interact with any
Jetpack Compose UI (Android and Desktop) via a simple HTTP API. It essentially wraps your Composable
in a test harness and exposes an HTTP server to control it.

## Features

- **Cross-Platform**: Works with Android and JVM (Desktop) Compose.
- **Zero Code Changes**: Integrates via a Gradle Settings plugin, requiring no changes to your
  existing application code.
- **HTTP Control**: Exposes a REST-like API to:
    - Take screenshots of the entire screen or specific nodes.
    - Perform gestures (click, scroll, swipe, text input).
    - Inspect the UI tree.
    - Wait for idle states or specific nodes.
    - Reset the UI state.

## Installation

Add the plugin to your `settings.gradle.kts`:

```kotlin
plugins {
    id("com.github.jdemeulenaere.compose.driver") version "0.1.0"
}

composeDriver {
    android()
    desktop()
}
```

This plugin automatically creates two new generic projects (`:compose-driver-android` and
`:compose-driver-desktop`) in your build, configured to run your Composables within the driver
environment.

Optionally, you can configure the generated project names and environment:

```kotlin
composeDriver {
    android {
        name = "compose-driver-android"
        robolectric {
            sdk = 36
            qualifiers = "w410dp-h920dp-xhdpi" // see https://robolectric.org/device-configuration/
        }
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

To run a specific Composable with the driver enabled, use the `run` task on the generated project
and pass the fully qualified name of the Composable via `compose.driver.composable` system property.

**Android:**

> [!NOTE]
> The Android server runs in a robolectric test environment, so start-up is a bit slow and can take
> a few seconds before the server is ready.

```bash
./gradlew :compose-driver-android:run -Dcompose.driver.composable=com.example.mypackage.MyScreenKt.MyScreen
```

**Desktop:**

```bash
./gradlew :compose-driver-desktop:run -Dcompose.driver.composable=com.example.mypackage.MyScreenKt.MyScreen
```

Once running, the server will be available at `http://localhost:8080`.

## Interaction Example

Here is an example sequence of commands to automate a login flow:

```bash
# 1. Wait for the login screen to appear
curl "http://localhost:8080/waitForNode?tag=login_screen"

# 2. Enter username
curl "http://localhost:8080/textInput?tag=username_field&text=myuser"

# 3. Enter password
curl "http://localhost:8080/textInput?tag=password_field&text=mypassword"

# 4. Click login button
curl "http://localhost:8080/click?tag=login_button"

# 5. Wait for home screen
curl "http://localhost:8080/waitForNode?tag=home_screen"

# 6. Take a screenshot
curl "http://localhost:8080/screenshot" > screenshot.png
```

## API Reference

> [!IMPORTANT]
> Most endpoints accept a `tag` parameter to target a specific semantic node (tagged using
`Modifier.testTag()`); if omitted, the root node is targeted.

### Inspection & State

- `GET /status`: Returns "ok" if server is running.
- `GET /printTree`: Returns the accessibility semantic tree.
- `GET /screenshot`: Returns a PNG screenshot.
- `GET /waitForIdle`: Waits for Compose to be idle.
- `GET /waitForNode`: Waits for a node to exist. Default timeout 5s.

### Interaction

- `GET /click`: Clicks on the node.
- `GET /doubleClick`: Double clicks on the node.
- `GET /longClick`: Long clicks on the node.
- `GET /textInput?text=...`: Enters text into the node.
- `GET /textReplacement?text=...`: Replaces text in the node.
- `GET /textClearance`: Clears text in the node.
- `GET /navigateBack`: Triggers a back navigation event.

### Gestures

- `GET /swipe?direction=LEFT|RIGHT|UP|DOWN`: Performs a swipe gesture.
- `GET /pointerInput/down?x=...&y=...`: Sends pointer down event.
- `GET /pointerInput/moveBy?x=...&y=...`: Moves pointer by delta.
- `GET /pointerInput/moveTo?x=...&y=...`: Moves pointer to coordinate.
- `GET /pointerInput/up`: Sends pointer up event.

### Lifecycle

- `GET /reset?composable=...`: Resets the UI content. You can optionally switch to a different
  Composable by providing the `composable` parameter.

## How it works

The `compose-driver` plugin generates a separate Gradle project that depends on your code and the
`driver-core` library. It uses `ComposeUiTest` (the standard Compose testing library) to render your
content and drive the UI clock. The embedded Ktor server listens for commands and bridges them to
`ComposeUiTest` actions.
