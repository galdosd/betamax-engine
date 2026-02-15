# Betamax Engine Reference

A comprehensive reference for the betamax-engine, a 2D sprite animation engine built with Java/LWJGL/Jython.

## Architecture Overview

Betamax is a 2D sprite-based game engine where:
- **Rendering** is handled by Java + LWJGL 3.1.6 (OpenGL 3.3 core)
- **Game logic** is written in Jython (Python 2.7 on JVM)
- **Assets** are RGBA TIFF images organized into sprite template directories
- The window is fixed at **960x540 pixels**

## Sprite System

### Fullscreen Quads

**Every sprite is a fullscreen quad** covering the entire 960x540 window. The engine does not support variable-sized sprites. Each sprite is a textured quad with vertices at the four corners of clip space (-1 to 1). Small game objects are achieved by drawing pixel art on a transparent 960x540 canvas — only the non-transparent pixels are visible.

Relevant code: `Texture.java` lines 136-147 define the vertex data:
```
-1.0, 1.0 (top-left)    to    1.0, 1.0 (top-right)
-1.0,-1.0 (bottom-left)  to    1.0,-1.0 (bottom-right)
```

### Layering and Render Order

Sprites render in this order:
1. **Layer number** (lower layers render first, appearing behind higher layers)
2. **Creation order** (tiebreaker within same layer)

Alpha blending is enabled, so transparent pixels in higher-layer sprites correctly show lower layers behind them.

Set layer via: `get_sprite("name").setLayer(n)` where lower n = further back.

### Sprite Templates

A sprite template is a directory of RGBA TIFF frames. The engine discovers templates at startup by scanning the classpath. Each template directory is at:
```
src/main/resources/com/github/galdosd/betamax/sprites/<template_name>/
```

Frame files must be named `frame_NNNN.tif` (4-digit zero-padded):
```
sprites/mysprite/frame_0000.tif
sprites/mysprite/frame_0001.tif
sprites/mysprite/frame_0002.tif
```

**Format**: RGBA TIFF, must be exactly 960x540 pixels. Alpha channel controls transparency.

### Sprite Animation

Sprites animate by cycling through their frames. Each render frame, the engine advances the sprite's animation by one frame. A sprite with N frames will complete one full cycle every N render frames.

Sprite animation can be controlled via:
- `.setPaused(bool)` — freeze/resume animation
- `.setRepetitions(n)` — set number of times to loop (0 = infinite)
- `.setHidden(bool)` — hide sprite without destroying it
- `.getCurrentFrame()` — get current frame index
- `.getTotalFrames()` — get total frame count

## Coordinate System

The engine uses three coordinate systems:

### TextureCoordinate [0,1] x [0,1]
- **Origin**: bottom-left (southwest)
- **(0.5, 0.5)** = screen center
- **(0, 0)** = bottom-left corner
- **(1, 1)** = top-right corner
- **This is what Python scripts use** via `set_sprite_location(name, x, y)`
- **IMPORTANT**: Coordinates are validated — values outside [0, 1] will throw an error

### FramebufferCoordinate [-1,1] x [-1,1]
- **Origin**: center
- Conversion: `fb = tex * 2.0 - 1.0`
- Used internally as the `translatePosition` uniform in shaders

### Screen Coordinates
- **Origin**: top-left (northwest)
- Only used in mouse click callbacks, immediately converted to TextureCoordinate

### Positioning Sprites

`set_sprite_location(name, x, y)` sets the TextureCoordinate where the sprite's **center** appears on screen:
- `set_sprite_location("player", 0.5, 0.5)` — player at screen center
- `set_sprite_location("player", 0.25, 0.22)` — player at 25% from left, 22% from bottom

Since sprites are fullscreen quads, "positioning" actually shifts the entire quad. For a small object drawn at the canvas center, the object appears at the specified screen position while the transparent parts extend beyond the visible area.

**Edge behavior**: At x=0.0 or x=1.0, the sprite center is at the screen edge, so the art (drawn at canvas center) will be half-visible. This can be used for spawn/despawn effects.

## Python Scripting API

Game scripts are written in Jython (Python 2.7) and loaded from:
```
src/main/resources/com/github/galdosd/betamax/scripts/<script_name>.py
```

The active script is set via JVM property: `-Dbetamax.mainScript=myscript.py`

### Imports

```python
from betamax import *
```

This provides: `create_sprite`, `destroy_sprite`, `get_sprite`, `set_sprite_location`, `sprite_exists`, `after_sprite`, `log`, `state`, `sprites`, `whenever`, etc.

### Event System

The engine has an event-driven callback system. **All callbacks must be registered during script initialization** (top-level code), before the `BEGIN` event fires.

#### Available Events

| Event | Decorator | When it fires |
|-------|-----------|---------------|
| `BEGIN` | `@whenever.begin` | Once, after all initialization is complete |
| `SPRITE_MOMENT` | `@whenever.sprite_moment("name", frame_num)` | When a specific sprite reaches a specific animation frame |
| `SPRITE_CREATE` | `@whenever.sprite_create("name")` | When a sprite with that name is created |
| `SPRITE_DESTROY` | `@whenever.sprite_destroy("name")` | When a sprite with that name is destroyed |
| `SPRITE_CLICK` | `@whenever.sprite_click("name")` | When user clicks on non-transparent pixels of that sprite |

#### Callback Registration Examples

```python
from betamax import *

# Global begin event (no arguments)
@whenever.begin
def on_start():
    create_sprite("background", "bg")
    set_sprite_location("bg", 0.5, 0.5)

# Sprite moment — fires when sprite "player" reaches frame 0
@whenever.sprite_moment("player", 0)
def on_player_frame_0():
    log("Player hit frame 0")

# Sprite click
@whenever.sprite_click("player")
def on_player_click():
    log("Player was clicked!")

# Sprite creation
@whenever.sprite_create("enemy")
def on_enemy_created():
    log("Enemy appeared!")
```

**Important notes:**
- All callbacks take **zero arguments** (the callback function signature must be `def func():`)
- Callbacks are registered at script load time, before `BEGIN` fires
- You cannot register new callbacks after initialization
- `sprite_moment` requires both the sprite name AND the frame number
- `sprite_moment` with frame -1 fires on the last frame

### Game Loop via sprite_moment

There is **no built-in game loop tick**. To create one, register `sprite_moment` callbacks for every frame of a looping sprite:

```python
def game_tick():
    # ... your game logic ...
    pass

# Register for each frame of a 6-frame sprite
for i in range(6):
    def make_callback(frame):
        def cb():
            game_tick()
        return cb
    whenever.sprite_moment("player", frame)(make_callback(i))
```

This fires `game_tick()` once per render frame (approximately 60 times per second).

### Sprite API

```python
sprite = get_sprite("name")
sprite.setLocation(textureCoord)    # Use set_sprite_location() wrapper instead
sprite.setLayer(n)                   # Render order (lower = behind)
sprite.setRepetitions(n)             # Animation loop count (0 = infinite)
sprite.setPaused(bool)               # Freeze/resume animation
sprite.setHidden(bool)               # Show/hide without destroying
sprite.setPinnedToCursor(bool)       # Sprite follows mouse cursor
sprite.setClickability(enum)         # Control click behavior
sprite.getCurrentFrame()             # Current animation frame index
sprite.getTotalFrames()              # Total frames in template
sprite.getAge()                      # Frames since creation
```

### State Management

```python
from betamax import state

# Set state variables (JSON-serialized, persists across frames)
state.score = 100
state.player_pos = [0.5, 0.5]

# Get state variables
current_score = state.score  # Returns 100
```

State variables are stored as JSON strings internally. Any JSON-serializable value works: numbers, strings, lists, dicts. For performance-critical per-frame data, prefer module-level Python variables.

### Utility Functions

```python
create_sprite("template_name", "instance_name")  # Create sprite from template
destroy_sprite("name")                            # Remove sprite from scene
destroy_any_sprite("name")                        # Destroy if exists (no error if not)
sprite_exists("name")                             # Check if sprite exists
set_sprite_location("name", x, y)                 # Position sprite (TextureCoordinate)
after_sprite("first", "second")                   # When first ends, replace with second
log("message")                                    # Log to engine console
reboot()                                          # Restart everything
set_global_shader("shader_name")                  # Change active shader
```

## Input System

### Available Input

- **Mouse clicks**: Detected on non-transparent sprite pixels. Fires `SPRITE_CLICK` event for the topmost clickable sprite at the click position.

### NOT Available

- **No keyboard input** is exposed to Python scripts
- **No mouse position/hover** events (except `setPinnedToCursor`)
- **No gamepad/controller** support

This means games must be designed around mouse/click interaction only.

## Shaders

The engine uses GLSL 330 core shaders.

### Default Vertex Shader (`default.vert`)
```glsl
#version 330 core
layout (location = 0) in vec2 position;
layout (location = 1) in vec2 vTexPos;
uniform vec2 translatePosition;
out vec2 fTexPos;

void main() {
    gl_Position = vec4(position.x + translatePosition.x,
                       position.y + translatePosition.y, 0.0, 1.0);
    fTexPos = vec2(vTexPos.x, vTexPos.y);
}
```

### Default Fragment Shader (`default.frag`)
```glsl
#version 330 core
in vec2 fTexPos;
out vec4 outColor;
uniform sampler2D ourTexture;

void main() {
    outColor = texture(ourTexture, fTexPos);
}
```

Custom shaders can be set via `set_global_shader("name")`. Shader files live in:
```
src/main/resources/com/github/galdosd/betamax/shaders/
```

## Build System

### Prerequisites
- Java 11 (JDK)
- Maven 3.x
- OpenJFX 11 (`libopenjfx-java libopenjfx-jni` packages on Ubuntu/Debian)
- Python 3 + Pillow (for generating test assets)
- Xvfb + ImageMagick (for headless running and screenshots)

### Build Command
```bash
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 mvn clean package -DskipTests
```

This produces a fat JAR at:
```
target/betamax-engine-1.0-SNAPSHOT-betamax-assembly.jar
```

### Key Dependencies
- LWJGL 3.1.6 (OpenGL, GLFW, OpenAL, STB, LZ4)
- Jython 2.7.1 (Python scripting)
- Lombok 1.18.6 (Java boilerplate reduction)
- OpenJFX 11.0.2 (DevConsole, not needed at runtime with debugMode=false)
- Reflections 0.9.11 (classpath scanning for sprite templates)
- Jackson 2.9.5 (JSON serialization)

## Running the Engine

### JVM Properties (Required)

| Property | Description | Example |
|----------|-------------|---------|
| `betamax.mainScript` | Python script to run | `smoketest.py` |
| `betamax.textureCacheDir` | Directory for texture cache | `/tmp/betamax-cache/` |
| `betamax.snapshotDir` | Directory for snapshots | `/tmp/betamax-snapshots/` |
| `betamax.usePrecompiledManifests` | Use pre-built manifests? | `false` |
| `betamax.manifestsPackageFilename` | Manifest output file | `/tmp/manifests.json` |
| `betamax.debugMode` | Enable debug console (needs JavaFX) | `false` |
| `betamax.enableSound` | Enable OpenAL sound | `false` |

### Run Command
```bash
Xvfb :99 -screen 0 1280x720x24 +extension GLX +render -noreset &
export DISPLAY=:99

java \
  -Dbetamax.mainScript=platformer.py \
  -Dbetamax.textureCacheDir=/tmp/betamax-cache/ \
  -Dbetamax.snapshotDir=/tmp/betamax-snapshots/ \
  -Dbetamax.usePrecompiledManifests=false \
  -Dbetamax.manifestsPackageFilename=/tmp/manifests.json \
  -Dbetamax.debugMode=false \
  -Dbetamax.enableSound=false \
  -jar target/betamax-engine-1.0-SNAPSHOT-betamax-assembly.jar
```

### Capturing Screenshots
```bash
import -window root /tmp/screenshot.png
```

## Known Limitations and Workarounds

1. **All sprites are fullscreen** — cannot have variable-sized sprites. Work around with transparent canvases.

2. **No keyboard input in scripts** — only mouse clicks. Design games as point-and-click or auto-runners with click-to-jump.

3. **Coordinates clamped to [0, 1]** — cannot position sprites off-screen. Spawn at edges (0.0 or 1.0) and destroy at opposite edge.

4. **Callbacks registered at init only** — cannot dynamically add callbacks during gameplay. Register all needed callbacks upfront for all possible sprite names.

5. **Platform.exit() with debugMode=false** — JavaFX toolkit not initialized, so Platform.exit() throws. Wrapped in try-catch in GlProgramBase.java.

6. **Sound requires OpenAL** — crashes if hardware unavailable. Use `betamax.enableSound=false` to disable.

7. **Proprietary assets removed** — original game sprites, sounds, and some shaders were proprietary and are gitignored. Placeholder shaders and test sprites are provided.

8. **Java 11 required** — Java 8 OpenJDK lacks JavaFX. Java 11 has `--illegal-access=permit` by default, keeping LWJGL 3.1.6 and Jython 2.7.1 compatible without `--add-opens` flags.

## Source Code Structure

```
src/main/java/com/github/galdosd/betamax/
  engine/
    BetamaxGlProgram.java   — Main entry point, game loop, window (960x540)
    GlProgramBase.java      — Base OpenGL program with GLFW setup
    OurShaders.java         — Shader references
  graphics/
    Texture.java            — Sprite rendering (fullscreen quad, VAO, VBO)
  opengl/
    TextureCoordinate.java  — [0,1] coordinate system
    FramebufferCoordinate.java — [-1,1] coordinate system
  scripting/
    ScriptServicer.java     — Java-Python bridge
    ScriptWorld.java        — Jython interpreter management
  sprite/
    SpriteRegistry.java     — Sprite creation, destruction, render ordering
    SpriteImpl.java         — Individual sprite state
    SpriteTemplate.java     — Template definition (frames, moments)
  sound/
    SoundWorld.java         — OpenAL sound system (disable with enableSound=false)

src/main/resources/
  betamax.py                — Python scripting module (imported by game scripts)
  com/github/galdosd/betamax/
    scripts/                — Game scripts (*.py)
    sprites/                — Sprite template directories
    shaders/                — GLSL shader files
```
