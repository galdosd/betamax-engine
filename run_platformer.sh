#!/bin/bash
# Gritty's Philly Adventure - Build & Run
#
# Prerequisites (Ubuntu/Debian):
#   apt-get install -y openjdk-11-jdk libopenjfx-java libopenjfx-jni xvfb imagemagick
#   pip3 install Pillow
#
# This script:
#   1. Generates platformer sprite assets (RGBA TIFFs)
#   2. Builds the fat JAR with Maven
#   3. Runs the engine under Xvfb (headless)
#   4. Captures screenshots at multiple time intervals
#
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-11-openjdk-amd64}"
DISPLAY_NUM="${DISPLAY_NUM:-99}"
SCREENSHOT_DIR="${SCREENSHOT_DIR:-/tmp/betamax-screenshots}"

echo "=== Gritty's Philly Adventure - Build & Run ==="
echo "JAVA_HOME: $JAVA_HOME"
echo ""

# Step 1: Generate platformer assets
echo "--- Step 1: Generating platformer sprite assets ---"
python3 "$SCRIPT_DIR/tools/generate_platformer_assets.py"

# Step 2: Build
echo ""
echo "--- Step 2: Building fat JAR ---"
JAVA_HOME="$JAVA_HOME" mvn -f "$SCRIPT_DIR/pom.xml" clean package -DskipTests -q
JAR="$SCRIPT_DIR/target/betamax-engine-1.0-SNAPSHOT-betamax-assembly.jar"
echo "Built: $JAR ($(du -h "$JAR" | cut -f1))"

# Step 3: Start Xvfb and run engine
echo ""
echo "--- Step 3: Starting Xvfb and engine ---"
mkdir -p /tmp/betamax-cache /tmp/betamax-snapshots "$SCREENSHOT_DIR"

pkill Xvfb 2>/dev/null || true
sleep 1
Xvfb ":$DISPLAY_NUM" -screen 0 1280x720x24 +extension GLX +render -noreset &
XVFB_PID=$!
sleep 2
export DISPLAY=":$DISPLAY_NUM"

"$JAVA_HOME/bin/java" \
  -Dbetamax.mainScript=platformer.py \
  -Dbetamax.textureCacheDir=/tmp/betamax-cache/ \
  -Dbetamax.snapshotDir=/tmp/betamax-snapshots/ \
  -Dbetamax.usePrecompiledManifests=false \
  -Dbetamax.manifestsPackageFilename=/tmp/manifests.json \
  -Dbetamax.debugMode=false \
  -Dbetamax.enableSound=false \
  -jar "$JAR" > /tmp/betamax-engine.log 2>&1 &
ENGINE_PID=$!
echo "Engine PID: $ENGINE_PID"

# Step 4: Capture screenshots at different intervals for varied shots
echo "Waiting for engine to initialize..."
sleep 12

if kill -0 $ENGINE_PID 2>/dev/null; then
    echo ""
    echo "--- Step 4: Engine running! Capturing screenshots ---"

    # Screenshot 1: Early game (street level, initial scene)
    import -window root "$SCREENSHOT_DIR/screenshot_street1.png"
    echo "  Captured: screenshot_street1.png"

    sleep 5

    # Screenshot 2: A few seconds in (more objects scrolling)
    if kill -0 $ENGINE_PID 2>/dev/null; then
        import -window root "$SCREENSHOT_DIR/screenshot_street2.png"
        echo "  Captured: screenshot_street2.png"
    fi

    sleep 8

    # Screenshot 3: Further in (different object mix)
    if kill -0 $ENGINE_PID 2>/dev/null; then
        import -window root "$SCREENSHOT_DIR/screenshot_mid.png"
        echo "  Captured: screenshot_mid.png"
    fi

    sleep 10

    # Screenshot 4: Even further (possibly impound lot by now)
    if kill -0 $ENGINE_PID 2>/dev/null; then
        import -window root "$SCREENSHOT_DIR/screenshot_later.png"
        echo "  Captured: screenshot_later.png"
    fi

    sleep 8

    # Screenshot 5: Late game
    if kill -0 $ENGINE_PID 2>/dev/null; then
        import -window root "$SCREENSHOT_DIR/screenshot_late.png"
        echo "  Captured: screenshot_late.png"
    fi

    echo ""
    echo "Screenshots saved to: $SCREENSHOT_DIR/"
    ls -la "$SCREENSHOT_DIR/"*.png

    echo ""
    echo "--- Engine log (last 30 lines) ---"
    tail -30 /tmp/betamax-engine.log

    echo ""
    echo "=== GRITTY'S PHILLY ADVENTURE IS RUNNING ==="
    echo "Press Ctrl+C to stop."

    # Cleanup on exit
    trap "kill $ENGINE_PID 2>/dev/null; kill $XVFB_PID 2>/dev/null" EXIT
    wait $ENGINE_PID
else
    echo ""
    echo "=== ENGINE FAILED TO START ==="
    echo "--- Engine log ---"
    cat /tmp/betamax-engine.log
    kill $XVFB_PID 2>/dev/null
    exit 1
fi
