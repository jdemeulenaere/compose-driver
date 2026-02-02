---
name: compose_driver
description: Drive the Compose Driver sample app to test UI logic. Use it whenever you need to verify that Composables work as expected.
---

# Compose Driver Skill

## Description

The Compose Driver Skill enables AI agents to "see" and "control" any Jetpack Compose UI (Android or
Desktop) via a standardized HTTP API. It wraps a target Composable in a test harness and exposes a
local server that translates HTTP requests into `ComposeUiTest` actions.

## Running the Driver

The driver must be running for the agent to interact with the UI. Before starting the driver, **always
ensure previous instances are killed** to avoid port conflicts (the server uses port 8080).

**Desktop (Recommended for speed for JVM & Multiplatform apps):**

```bash
pkill -f "compose-driver-desktop" || true; ./gradlew :compose-driver-desktop:run -Dcompose.driver.composable=io.github.jdemeulenaere.compose.driver.sample.desktop.DesktopApplicationKt.DesktopApplication
```

**Android (Robolectric):**

> [!NOTE]
> The Android server will run in a Robolectric test environment, so start-up is expected to be
> slower (~5s on modern hardware).

```bash
pkill -f "compose-driver-android" || true; ./gradlew :compose-driver-android:run -Dcompose.driver.composable=io.github.jdemeulenaere.compose.driver.sample.android.AndroidApplicationKt.AndroidApplication
```

* **Server Address:** `http://localhost:8080`
* **Target:** `compose.driver.composable` must be the fully qualified name of the Composable
  function (e.g., `package.FileKt.ComposableName`).
* **Wait for Ready:** After starting the command, poll `GET /status` until it returns "ok".

## API Reference (Agent Actions)

The agent interacts with the UI by sending HTTP GET requests.

**Common Parameters:**

* `tag` (Optional): The `Modifier.testTag` of the target node. If omitted, the action applies to the
  **root** node.
* `gifDurationMs` (Optional): If provided (max 5000ms), the server records the interaction and
  returns a GIF instead of a text response. Requires `ffmpeg`.

### 1. Observation (Seeing the UI)

| Action                | Endpoint      | Parameters | Description                                                                                                         |
|:----------------------|:--------------|:-----------|:--------------------------------------------------------------------------------------------------------------------|
| **Inspect Hierarchy** | `/printTree`  | None       | Returns the accessibility/semantic tree of the UI as text. Use this to find `testTag`s and understand UI structure. |
| **Take Screenshot**   | `/screenshot` | `tag`      | Returns a PNG image of the specific node (or full screen if `tag` is omitted).                                      |
| **Check Status**      | `/status`     | None       | Returns "ok" if the driver is ready.                                                                                |

### 2. Synchronization (Waiting)

| Action            | Endpoint       | Parameters                              | Description                                                                                                                      |
|:------------------|:---------------|:----------------------------------------|:---------------------------------------------------------------------------------------------------------------------------------|
| **Wait for Node** | `/waitForNode` | `tag` (req), `timeout` (opt, def: 5000) | Blocks until a node with the specified tag exists. **Crucial** to use before interacting with elements that load asynchronously. |
| **Wait for Idle** | `/waitForIdle` | None                                    | Blocks until the Compose UI is idle (no pending layout/draw passes).                                                             |

### 3. Interaction (Control)

| Action            | Endpoint           | Parameters          | Description                                                              |
|:------------------|:-------------------|:--------------------|:-------------------------------------------------------------------------|
| **Click**         | `/click`           | `tag`               | Performs a click on the element.                                         |
| **Double Click**  | `/doubleClick`     | `tag`               | Performs a double-click.                                                 |
| **Long Click**    | `/longClick`       | `tag`               | Performs a long-press.                                                   |
| **Input Text**    | `/textInput`       | `tag`, `text` (req) | Types text into a focused or specified text field.                       |
| **Replace Text**  | `/textReplacement` | `tag`, `text` (req) | Replaces the text content directly (simulates pasting/programmatic set). |
| **Clear Text**    | `/textClearance`   | `tag`               | Clears the text in a field.                                              |
| **Navigate Back** | `/navigateBack`    | None                | Triggers the system "Back" button event.                                 |

### 4. Gestures

| Action           | Endpoint               | Parameters                  | Description                                |
|:-----------------|:-----------------------|:----------------------------|:-------------------------------------------|
| **Swipe**        | `/swipe`               | `tag`, `direction` (req)    | Swipes `UP`, `DOWN`, `LEFT`, or `RIGHT`.   |
| **Pointer Down** | `/pointerInput/down`   | `x`, `y` (req), `pointerId` | Sends a pointer down event at coordinates. |
| **Pointer Move** | `/pointerInput/moveTo` | `x`, `y` (req), `pointerId` | Moves the pointer to absolute coordinates. |
| **Pointer Up**   | `/pointerInput/up`     | `pointerId`                 | Releases the pointer.                      |

### 5. Lifecycle

| Action       | Endpoint | Parameters         | Description                                                                                        |
|:-------------|:---------|:-------------------|:---------------------------------------------------------------------------------------------------|
| **Reset UI** | `/reset` | `composable` (opt) | Resets the UI state. Can optionally switch to a completely different Composable class dynamically. |

## Usage Examples for Agents

**Scenario: Login Flow**

1. **Inspect:** `GET /printTree` -> Identify tags `username_field`, `password_field`,
   `login_button`.
2. **Wait:** `GET /waitForNode?tag=username_field`
3. **Act:**
    * `GET /textInput?tag=username_field&text=myuser`
    * `GET /textInput?tag=password_field&text=mypass`
    * `GET /click?tag=login_button`
4. **Verify:** `GET /waitForNode?tag=home_screen_title`

**Scenario: Debugging an Animation**

1. **Record:** `GET /click?tag=animate_btn&gifDurationMs=1000`
    * *Result:* Returns a GIF file showing the click and the subsequent 1 second of animation.