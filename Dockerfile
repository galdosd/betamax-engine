FROM ubuntu:24.04 AS builder

ENV DEBIAN_FRONTEND=noninteractive

# Install Java 11, Maven, and build tools
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-11-jdk \
    maven \
    python3 \
    python3-pip \
    && rm -rf /var/lib/apt/lists/*

# Install Pillow for TIFF sprite generation
RUN pip3 install --break-system-packages Pillow

WORKDIR /build
COPY . /build/

# Generate test sprite assets
RUN python3 -c "\
from PIL import Image; \
import os; \
d = 'src/main/resources/com/github/galdosd/betamax/sprites/testsprite'; \
os.makedirs(d, exist_ok=True); \
Image.new('RGBA', (64,64), (220,60,60,200)).save(f'{d}/frame_0000.tif'); \
Image.new('RGBA', (64,64), (60,180,60,200)).save(f'{d}/frame_0001.tif'); \
Image.new('RGBA', (64,64), (60,60,220,200)).save(f'{d}/frame_0002.tif'); \
print('Test sprites generated')"

# Build the fat JAR
RUN JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 mvn clean package -DskipTests

# --- Runtime stage ---
FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive

# Install Java 11 runtime, Xvfb, and screenshot tools
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-11-jre \
    xvfb \
    imagemagick \
    libgl1-mesa-dri \
    libglx-mesa0 \
    mesa-utils \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the fat JAR from builder
COPY --from=builder /build/target/betamax-engine-1.0-SNAPSHOT-betamax-assembly.jar /app/betamax-engine.jar

# Create directories for cache and snapshots
RUN mkdir -p /tmp/betamax-cache /tmp/betamax-snapshots /tmp/betamax-screenshots

# Entrypoint script that starts Xvfb and runs the engine
COPY <<'ENTRYPOINT_SCRIPT' /app/run.sh
#!/bin/bash
set -e

# Start Xvfb
Xvfb :99 -screen 0 1280x720x24 +extension GLX +render -noreset &
sleep 2
export DISPLAY=:99

echo "Starting Betamax engine..."
java \
  -Dbetamax.mainScript=smoketest.py \
  -Dbetamax.textureCacheDir=/tmp/betamax-cache/ \
  -Dbetamax.snapshotDir=/tmp/betamax-snapshots/ \
  -Dbetamax.usePrecompiledManifests=false \
  -Dbetamax.manifestsPackageFilename=/tmp/manifests.json \
  -Dbetamax.debugMode=false \
  -Dbetamax.enableSound=false \
  -jar /app/betamax-engine.jar &

ENGINE_PID=$!
echo "Engine PID: $ENGINE_PID"

# Wait for engine to initialize and render
sleep 12

if kill -0 $ENGINE_PID 2>/dev/null; then
    echo "Engine is running. Taking screenshots..."
    import -window root /tmp/betamax-screenshots/screenshot1.png
    sleep 2
    import -window root /tmp/betamax-screenshots/screenshot2.png
    sleep 2
    import -window root /tmp/betamax-screenshots/screenshot3.png
    echo "Screenshots saved to /tmp/betamax-screenshots/"
    echo "Engine running successfully! Press Ctrl+C to stop."
    wait $ENGINE_PID
else
    echo "Engine failed to start. Logs:"
    exit 1
fi
ENTRYPOINT_SCRIPT

RUN chmod +x /app/run.sh

CMD ["/app/run.sh"]
