#!/bin/bash
set -e

# Ensure we are in the sample directory
SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
PROJECT_ROOT="$SCRIPT_DIR/../../../.."
LOG_FILE=$(mktemp)

cd "$PROJECT_ROOT"

echo "Starting Desktop Application via Compose Driver..."
echo "Logs will be written to $LOG_FILE"

# Start the gradle task in the background using the plugin-generated project.
# We pass the composable to run.
./gradlew :compose-driver-desktop:run -Dcompose.driver.composable=io.github.jdemeulenaere.compose.driver.sample.desktop.DesktopApplicationKt.DesktopApplication > "$LOG_FILE" 2>&1 &
PID=$!

echo "Application started with PID $PID."
echo "Waiting for Compose Driver to be ready on port 8080..."

# Poll loop
MAX_RETRIES=60
count=0
while [ $count -lt $MAX_RETRIES ]; do
  if curl -s http://localhost:8080/status | grep -q "ok"; then
    echo "Compose Driver is ready!"
    exit 0
  fi
  sleep 1
  count=$((count + 1))
done

echo "Timeout waiting for Compose Driver to start."
echo "Last few lines of log:"
tail -n 10 "$LOG_FILE"
# Kill the hung process
kill $PID || true
exit 1
