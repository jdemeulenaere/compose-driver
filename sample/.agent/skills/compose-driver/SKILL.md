---
name: compose_driver
description: Drive the Compose Driver sample app to test UI logic. Use it whenever you need to verify that Composables work as expected or to capture screenshots of a composable.
---

# Compose Driver Skill

## Description

The Compose Driver Skill enables AI agents to see and control any Jetpack Compose UI (Android or
Desktop) via a standardized HTTP API. It wraps a target Composable in a test harness and exposes a
local server that translates HTTP requests into `ComposeUiTest` actions.

## Running the Driver

The driver must be running for the agent to interact with the UI. Before starting the driver,
**always ensure previous instances are killed** to avoid port conflicts (the server uses port 8080).

* **Server Address:** `http://localhost:8080`
* **Target:** `compose.driver.composable` must be the fully qualified name of the Composable
  function (e.g., `package.FileKt.ComposableName`).
* **Wait for Ready:** After starting the command, poll `GET /status` until it returns "ok".

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

## API Reference (Agent Actions)

The agent interacts with the UI by sending HTTP GET requests.

**Common Parameters:**

These parameters can be used on all endpoints, applying the operation to the matching node.

* `nodeTag` (Optional): The `Modifier.testTag` of the target node.
* `nodeText` (Optional): Matches a node by its text content.
* `nodeTextSubstring` (Optional): If `true`, `nodeText` matches as a substring (default: `false`,
  exact match).
* `nodeTextIgnoreCase` (Optional): If `true`, `nodeText` matching is case-insensitive (default:
  `false`).
* If both `nodeTag` and `nodeText` are provided, the node must match **both**. If neither is
  provided, the action applies to the **root** node.
* `gifDurationMs` (Optional): If provided (max 5000ms), the server records the interaction and
  returns a GIF instead of a text response. Requires `ffmpeg`.

### 1. Observation (Seeing the UI)

| Action                | Endpoint      | Description                                                                                                         |
|:----------------------|:--------------|:--------------------------------------------------------------------------------------------------------------------|
| **Inspect Hierarchy** | `/printTree`  | Returns the accessibility/semantic tree of the UI as text. Use this to find `testTag`s and understand UI structure. |
| **Take Screenshot**   | `/screenshot` | Returns a PNG image of the specific node (or full screen if no node params).                                        |
| **Check Status**      | `/status`     | Returns "ok" if the driver is ready.                                                                                |

### 2. Synchronization (Waiting)

| Action            | Endpoint       | Additional parameters      | Description                                                                                                        |
|:------------------|:---------------|:---------------------------|:-------------------------------------------------------------------------------------------------------------------|
| **Wait for Node** | `/waitForNode` | `timeout` (opt, def: 5000) | Blocks until a matching node exists. **Crucial** to use before interacting with elements that load asynchronously. |
| **Wait for Idle** | `/waitForIdle` |                            | Blocks until the Compose UI is idle (no pending layout/draw passes).                                               |

### 3. Interaction (Control)

| Action            | Endpoint           | Additional parameters                                            | Description                                                                               |
|:------------------|:-------------------|:-----------------------------------------------------------------|:------------------------------------------------------------------------------------------|
| **Click**         | `/click`           |                                                                  | Performs a click on the element.                                                          |
| **Double Click**  | `/doubleClick`     |                                                                  | Performs a double-click.                                                                  |
| **Long Click**    | `/longClick`       |                                                                  | Performs a long-press.                                                                    |
| **Input Text**    | `/textInput`       | `text` (req)                                                     | Types text into a focused or specified text field.                                        |
| **Replace Text**  | `/textReplacement` | `text` (req)                                                     | Replaces the text content directly (simulates pasting/programmatic set).                  |
| **Clear Text**    | `/textClearance`   |                                                                  | Clears the text in a field.                                                               |
| **Key Event**     | `/keyEvent`        | `key` (req), `action` (opt, def: `press`), `modifiers` (opt)    | Sends a raw keyboard event. See **Key Events** section below.                             |
| **Navigate Back** | `/navigateBack`    |                                                                  | Triggers the system "Back" button event.                                                  |
| **Scroll To**     | `/scrollTo`        |                                                                  | Scrolls to make the element visible in the viewport, so that it can then be clicked, etc. |

#### Key Events

The `/keyEvent` endpoint sends raw keyboard events to a Compose UI node.

**Parameters:**

* `key` (required): Property name from `androidx.compose.ui.input.key.Key` (e.g., `A`, `Enter`,
  `DirectionUp`, `ShiftLeft`, `F1`).
* `action` (optional, default: `press`):
    * `press` — sends key down + key up (a full key press).
    * `down` — sends only key down (hold the key).
    * `up` — sends only key up (release the key).
* `modifiers` (optional): Comma-separated list of modifier key names to hold down during a `press`
  action (e.g., `ShiftLeft`, `CtrlLeft,ShiftLeft`). Only applies when `action=press`.

**Examples:**

```
GET /keyEvent?key=Enter
GET /keyEvent?key=A&nodeTag=myTextField
GET /keyEvent?key=A&modifiers=ShiftLeft
GET /keyEvent?key=C&modifiers=CtrlLeft
GET /keyEvent?key=Delete&modifiers=CtrlLeft,ShiftLeft
GET /keyEvent?key=ShiftLeft&action=down
GET /keyEvent?key=ShiftLeft&action=up
```

### 4. Gestures

| Action           | Endpoint               | Parameters                  | Description                                |
|:-----------------|:-----------------------|:----------------------------|:-------------------------------------------|
| **Swipe**        | `/swipe`               | `direction` (req)           | Swipes `UP`, `DOWN`, `LEFT`, or `RIGHT`.   |
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
2. **Wait:** `GET /waitForNode?nodeTag=username_field`
3. **Act:**
    * `GET /textInput?nodeTag=username_field&text=myuser`
    * `GET /textInput?nodeTag=password_field&text=mypass`
    * `GET /click?nodeText=login&nodeTextIgnoreCase=true&nodeTextSubstring=true`
4. **Verify:** `GET /waitForNode?nodeTag=home_screen_title`

**Scenario: Debugging an Animation**

1. **Record:** `GET /click?nodeTag=animate_btn&gifDurationMs=1000`
    * *Result:* Returns a GIF file showing the click and the subsequent 1 second of animation.

**Scenario: Interacting with Off-Screen List Items**

If `printTree` shows an element exists but is not visible because it is in a scrollable container
and outside of the current viewport, you **must** scroll to it before interacting with it (e.g.
clicking).

1. **Inspect:** `GET /printTree` -> Find the tag `target_item`.
2. **Scroll:** `GET /scrollTo?nodeTag=target_item` -> This brings the item into the viewport.
3. **Act:** `GET /click?nodeTag=target_item`

**Scenario: Keyboard Shortcuts**

1. **Focus:** `GET /click?nodeTag=editor` -> Click to focus the text editor.
2. **Select all:** `GET /keyEvent?key=A&modifiers=CtrlLeft&nodeTag=editor`
3. **Copy:** `GET /keyEvent?key=C&modifiers=CtrlLeft&nodeTag=editor`
4. **Navigate:** `GET /keyEvent?key=DirectionDown&nodeTag=editor` -> Move cursor down.
5. **Submit:** `GET /keyEvent?key=Enter&nodeTag=editor`