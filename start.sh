
#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

BROWSER_SCRIPT="$PROJECT_ROOT/scraper/src/main/java/com/example/browser_fetch.py"
PYTHON="$PROJECT_ROOT/scraper/.venv-nodriver/bin/python"
FLUTTER_DIR="$PROJECT_ROOT/app"

BROWSER_PID=""
SPRING_PID=""
FLUTTER_PID=""

cleanup() {
    echo "Stopping ficScraper..."

    for pid in "$FLUTTER_PID" "$SPRING_PID" "$BROWSER_PID"; do
        if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
            kill "$pid" 2>/dev/null || true
        fi
    done

    wait 2>/dev/null || true
}

trap cleanup EXIT INT TERM

# 1. Start the Python browser service.
echo "Starting Python browser service..."
"$PYTHON" "$BROWSER_SCRIPT" &
BROWSER_PID=$!

# 2. Start Spring Boot.
echo "Starting Spring Boot..."
cd "$PROJECT_ROOT/scraper"
mvn spring-boot:run &
SPRING_PID=$!

# 3. Start Flutter.
echo "Starting Flutter..."
cd "$FLUTTER_DIR"
flutter run -d linux &
FLUTTER_PID=$!

if wait -n "$BROWSER_PID" "$SPRING_PID" "$FLUTTER_PID"; then
    echo "A service exited. Shutting down ficScraper..."
else
    echo "A service exited with an error. Shutting down ficScraper..."
fi

exit 0
