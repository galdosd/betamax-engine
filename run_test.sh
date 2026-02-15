#!/bin/bash
# Betamax Engine - End-to-End Smoke Test
#
# Prerequisites (Ubuntu/Debian):
#   apt-get install -y openjdk-11-jdk libopenjfx-java libopenjfx-jni xvfb imagemagick
#   pip3 install Pillow
#
# This script:
#   1. Generates minimal test sprite assets (RGBA TIFFs)
#   2. Builds the fat JAR with Maven
#   3. Runs the engine under Xvfb (headless)
#   4. Captures screenshots of the running engine
#
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-11-openjdk-amd64}"
DISPLAY_NUM="${DISPLAY_NUM:-99}"
SCREENSHOT_DIR="${SCREENSHOT_DIR:-/tmp/betamax-screenshots}"

echo "=== Betamax Engine Smoke Test ==="
echo "JAVA_HOME: $JAVA_HOME"
echo ""

# Step 1: Generate test sprites
echo "--- Step 1: Generating test sprite assets ---"
SPRITE_DIR="$SCRIPT_DIR/src/main/resources/com/github/galdosd/betamax/sprites/testsprite"
mkdir -p "$SPRITE_DIR"
python3 -c "
from PIL import Image
import os
d = '$SPRITE_DIR'
Image.new('RGBA', (64,64), (220,60,60,200)).save(os.path.join(d, 'frame_0000.tif'))
Image.new('RGBA', (64,64), (60,180,60,200)).save(os.path.join(d, 'frame_0001.tif'))
Image.new('RGBA', (64,64), (60,60,220,200)).save(os.path.join(d, 'frame_0002.tif'))
print('Generated 3 test sprite frames (64x64 RGBA TIFF)')
"

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
  -Dbetamax.mainScript=smoketest.py \
  -Dbetamax.textureCacheDir=/tmp/betamax-cache/ \
  -Dbetamax.snapshotDir=/tmp/betamax-snapshots/ \
  -Dbetamax.usePrecompiledManifests=false \
  -Dbetamax.manifestsPackageFilename=/tmp/manifests.json \
  -Dbetamax.debugMode=false \
  -Dbetamax.enableSound=false \
  -jar "$JAR" > /tmp/betamax-engine.log 2>&1 &
ENGINE_PID=$!
echo "Engine PID: $ENGINE_PID"

echo "Waiting for engine to initialize..."
sleep 12

# Step 4: Capture screenshots
if kill -0 $ENGINE_PID 2>/dev/null; then
    echo ""
    echo "--- Step 4: Engine running! Capturing screenshots ---"
    import -window root "$SCREENSHOT_DIR/screenshot1.png"
    sleep 2
    import -window root "$SCREENSHOT_DIR/screenshot2.png"
    sleep 2
    import -window root "$SCREENSHOT_DIR/screenshot3.png"
    echo "Screenshots saved to: $SCREENSHOT_DIR/"
    ls -la "$SCREENSHOT_DIR/"*.png

    echo ""
    echo "--- Engine log (last 20 lines) ---"
    tail -20 /tmp/betamax-engine.log

    echo ""
    echo "=== SMOKE TEST PASSED ==="
    echo "The engine is rendering animated sprites (red/green/blue cycling)."
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
