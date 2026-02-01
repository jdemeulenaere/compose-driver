# Compose Driver Sample

This project demonstrates how to use the **Compose Driver** to instrument and drive Jetpack Compose
UI on Android and Desktop (JVM).

## Project Structure

- **`android/`**: Android library module.
- **`desktop/`**: Desktop (JVM) application module.
- **`multiplatform/`**: Shared UI code used by both platforms.

## Running the Driver

The Compose Driver plugin automatically creates run tasks for you.

### Desktop

Run the sample on Desktop:

```bash
./gradlew :compose-driver-desktop:run -Dcompose.driver.composable=io.github.jdemeulenaere.compose.driver.sample.desktop.DesktopApplicationKt.DesktopApplication
```

### Android

Run the sample on Android (via Robolectric):

```bash
./gradlew :compose-driver-android:run -Dcompose.driver.composable=io.github.jdemeulenaere.compose.driver.sample.android.AndroidApplicationKt.AndroidApplication
```

## Interacting with the Driver

Once the driver is running (default port `8080`), you can interact with it using HTTP requests in
your browser or using `curl`.

**Check Status:**

```bash
curl http://localhost:8080/status
```

**Print UI Tree:**

```bash
curl http://localhost:8080/printTree
```

**Click Example:**

```bash
# Click the button with testTag "button"
curl "http://localhost:8080/click?tag=button"
```
