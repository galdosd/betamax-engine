# Gritty's Philly Adventure
# A side-scrolling auto-runner platformer for betamax-engine
#
# Story: Gritty woke up hungover somewhere in Philly. He drunkenly lost his
# car last night and has to fight through PPA impound lots to find it.
# Click anywhere to jump!

from betamax import (create_sprite, destroy_sprite, get_sprite, set_sprite_location,
                     sprite_exists, whenever, log, state, destroy_any_sprite)
import java.lang.Math as Math

# ============================================================================
# Game Constants
# ============================================================================
GROUND_Y = 0.22        # sidewalk level (texture coords, 0=bottom)
GRITTY_X = 0.25        # Gritty's fixed horizontal position
GRAVITY = -0.002       # gravity per tick (negative = down)
JUMP_VEL = 0.028       # initial jump velocity
SCROLL_SPEED = 0.003   # obstacle scroll speed (street)
IMPOUND_SCROLL = 0.0025  # slightly slower in impound lots
CLOUD_SPEED = 0.001    # parallax cloud speed

# Spawn intervals (in ticks)
OBSTACLE_INTERVAL = 180
CHEESESTEAK_INTERVAL = 120
CLOUD_INTERVAL = 250
LEVEL_LENGTH = 800       # ticks per street/impound section
BOSS_SCROLL_SPEED = 0.001  # bosses scroll very slowly

# Collision box half-sizes (in texture coordinate space)
GRITTY_HW = 0.03   # gritty half-width
GRITTY_HH = 0.04   # gritty half-height
OBSTACLE_HW = 0.025
OBSTACLE_HH = 0.03
COLLECT_HW = 0.03
COLLECT_HH = 0.02

# Number of frames in gritty sprite (for moment registration)
GRITTY_FRAMES = 6

# ============================================================================
# Game State (module-level for performance, no JSON overhead)
# ============================================================================
gritty_y = GROUND_Y
gritty_vy = 0.0
jumping = False
tick_count = 0
score = 0
cheesesteaks_collected = 0
level = 1
phase = "street"          # "street" or "impound"
phase_tick = 0
next_id = 0
game_over = False
victory = False

# Active game objects: list of dicts
# Each: {"id": int, "type": str, "sprite_name": str, "x": float, "y": float,
#         "speed": float, "alive": bool}
objects = []

# Boss state
boss_active = False
boss_hits = 0
boss_name = None

# Level progression:
# street1 -> impound1 (boss: tow truck driver)
# street2 -> impound2 (boss: supervisor)
# street3 -> impound3 (boss: commissioner -> car -> victory)
level_sequence = [
    ("street", 1), ("impound", 1),
    ("street", 2), ("impound", 2),
    ("street", 3), ("impound", 3),
]
level_index = 0


# ============================================================================
# Helper Functions
# ============================================================================
def next_obj_id():
    global next_id
    next_id += 1
    return next_id


def clamp(v, lo, hi):
    if v < lo:
        return lo
    if v > hi:
        return hi
    return v


def spawn_object(obj_type, template, x, y, speed=None):
    """Spawn a new game object (obstacle, collectible, or decoration)."""
    global objects
    if speed is None:
        speed = SCROLL_SPEED if phase == "street" else IMPOUND_SCROLL

    oid = next_obj_id()
    name = "%s_%d" % (obj_type, oid)

    create_sprite(template, name)
    spr = get_sprite(name)

    # Set layers based on type
    if obj_type == "cloud" or obj_type == "hill" or obj_type == "libertybell":
        spr.setLayer(5)  # behind everything
    elif obj_type in ("cheesesteak", "pretzel"):
        spr.setLayer(40)  # collectibles
    elif obj_type in ("boss_driver", "boss_supervisor", "boss_commissioner", "grittys_car"):
        spr.setLayer(45)
    else:
        spr.setLayer(30)  # obstacles

    set_sprite_location(name, clamp(x, 0.0, 1.0), clamp(y, 0.0, 1.0))

    obj = {
        "id": oid,
        "type": obj_type,
        "template": template,
        "sprite_name": name,
        "x": x,
        "y": y,
        "speed": speed,
        "alive": True,
    }
    objects.append(obj)
    return obj


def destroy_object(obj):
    """Remove a game object from the scene."""
    obj["alive"] = False
    destroy_any_sprite(obj["sprite_name"])


def boxes_overlap(ax, ay, ahw, ahh, bx, by, bhw, bhh):
    """Check if two axis-aligned boxes overlap."""
    return (abs(ax - bx) < (ahw + bhw)) and (abs(ay - by) < (ahh + bhh))


# ============================================================================
# Level Management
# ============================================================================
def start_phase():
    """Initialize a new level phase (street or impound)."""
    global phase, level, phase_tick, boss_active, boss_hits, boss_name, objects

    phase_tick = 0
    boss_active = False
    boss_hits = 0
    boss_name = None

    # Clear all existing objects
    for obj in objects:
        if obj["alive"]:
            destroy_object(obj)
    objects = []

    # Switch background
    if sprite_exists("bg"):
        destroy_sprite("bg")

    if phase == "street":
        create_sprite("bg_street", "bg")
        log("=== STREET LEVEL %d ===" % level)
        log("Gritty searches the streets of Philly...")
    else:
        create_sprite("bg_impound", "bg")
        log("=== PPA IMPOUND LOT %d ===" % level)
        log("Gritty breaks into the impound lot!")

    get_sprite("bg").setLayer(0)
    set_sprite_location("bg", 0.5, 0.5)

    # Spawn some initial decorations
    if phase == "street":
        spawn_object("cloud", "cloud", 0.7, 0.80, CLOUD_SPEED)
        spawn_object("cloud", "cloud", 0.4, 0.85, CLOUD_SPEED)
        if level >= 2:
            spawn_object("libertybell", "libertybell", 0.8, 0.65, CLOUD_SPEED)


def advance_level():
    """Move to the next phase in the level sequence."""
    global level_index, phase, level, victory

    level_index += 1
    if level_index >= len(level_sequence):
        # Game complete!
        victory = True
        log("*** VICTORY! Gritty found his car! ***")
        log("Cheesesteaks collected: %d" % cheesesteaks_collected)
        return

    phase, level = level_sequence[level_index]
    start_phase()


# ============================================================================
# Spawning Logic
# ============================================================================
def spawn_street_objects():
    """Spawn obstacles and collectibles for street levels."""
    global phase_tick

    # Obstacles: pigeons and trash cans
    if phase_tick % OBSTACLE_INTERVAL == 0 and phase_tick > 0:
        if phase_tick % (OBSTACLE_INTERVAL * 2) == 0:
            spawn_object("trashcan", "trashcan", 1.0, GROUND_Y)
        else:
            spawn_object("pigeon", "pigeon", 1.0, GROUND_Y)

    # Cheesesteaks (floating above ground)
    if phase_tick % CHEESESTEAK_INTERVAL == 0 and phase_tick > 30:
        y = GROUND_Y + 0.12 + (phase_tick % 7) * 0.02
        spawn_object("cheesesteak", "cheesesteak", 1.0, clamp(y, 0.0, 1.0))

    # Pretzels (occasional)
    if phase_tick % 300 == 150:
        y = GROUND_Y + 0.08
        spawn_object("pretzel", "pretzel", 1.0, clamp(y, 0.0, 1.0))

    # Clouds
    if phase_tick % CLOUD_INTERVAL == 0 and phase_tick > 0:
        y = 0.72 + (phase_tick % 5) * 0.03
        spawn_object("cloud", "cloud", 1.0, clamp(y, 0.0, 1.0), CLOUD_SPEED)

    # Hills (background)
    if phase_tick % 400 == 200:
        spawn_object("hill", "hill", 1.0, GROUND_Y + 0.02, CLOUD_SPEED * 1.5)

    # Greased pole at end of level
    if phase_tick == LEVEL_LENGTH:
        spawn_object("greasedpole", "greasedpole", 1.0, GROUND_Y + 0.05, SCROLL_SPEED * 0.7)
        log("The greased pole appears! Almost there!")


def spawn_impound_objects():
    """Spawn obstacles for impound lot levels."""
    global phase_tick, boss_active, boss_name

    # Parking boots
    if phase_tick % (OBSTACLE_INTERVAL + 20) == 0 and phase_tick > 0 and not boss_active:
        spawn_object("parkingboot", "parkingboot", 1.0, GROUND_Y)

    # Tow hooks (floating)
    if phase_tick % (OBSTACLE_INTERVAL + 50) == 80 and not boss_active:
        y = GROUND_Y + 0.15
        spawn_object("towhook", "towhook", 1.0, clamp(y, 0.0, 1.0))

    # Cheesesteaks even in impound lots (Gritty needs sustenance)
    if phase_tick % (CHEESESTEAK_INTERVAL + 40) == 0 and phase_tick > 60 and not boss_active:
        y = GROUND_Y + 0.10
        spawn_object("cheesesteak", "cheesesteak", 1.0, clamp(y, 0.0, 1.0))

    # Boss appears at end
    if phase_tick == LEVEL_LENGTH and not boss_active:
        boss_active = True
        if level == 1:
            boss_template = "boss_driver"
            boss_type = "boss_driver"
            log("BOSS: Angry tow truck driver blocks the way!")
        elif level == 2:
            boss_template = "boss_supervisor"
            boss_type = "boss_supervisor"
            log("BOSS: PPA Supervisor with a clipboard of citations!")
        else:
            boss_template = "boss_commissioner"
            boss_type = "boss_commissioner"
            log("FINAL BOSS: The PPA Commissioner stands guard!")

        obj = spawn_object(boss_type, boss_template, 1.0, GROUND_Y, BOSS_SCROLL_SPEED)
        boss_name = obj["sprite_name"]


# ============================================================================
# Game Tick (Main Loop)
# ============================================================================
def game_tick():
    """Called every render frame via sprite_moment callbacks."""
    global gritty_y, gritty_vy, jumping, tick_count, phase_tick
    global score, cheesesteaks_collected, game_over, victory
    global boss_active, boss_hits, boss_name, objects

    if game_over or victory:
        return

    tick_count += 1
    phase_tick += 1

    # --- Physics ---
    if jumping:
        gritty_vy += GRAVITY
        gritty_y += gritty_vy
        if gritty_y <= GROUND_Y:
            gritty_y = GROUND_Y
            gritty_vy = 0.0
            jumping = False

    # Update Gritty position
    set_sprite_location("gritty", GRITTY_X, clamp(gritty_y, 0.0, 1.0))

    # --- Move all objects ---
    to_remove = []
    for obj in objects:
        if not obj["alive"]:
            to_remove.append(obj)
            continue

        obj["x"] -= obj["speed"]

        # Destroy if scrolled off left edge
        if obj["x"] < 0.02:
            # Greased pole reaching left means we passed it -> level complete
            if obj["type"] == "greasedpole":
                log("Gritty slides down the greased pole! Level complete!")
                destroy_object(obj)
                advance_level()
                return

            destroy_object(obj)
            to_remove.append(obj)
            continue

        # Update sprite position
        if sprite_exists(obj["sprite_name"]):
            set_sprite_location(obj["sprite_name"],
                                clamp(obj["x"], 0.0, 1.0),
                                clamp(obj["y"], 0.0, 1.0))

    # Clean up dead objects
    for obj in to_remove:
        if obj in objects:
            objects.remove(obj)

    # --- Collision Detection ---
    for obj in objects:
        if not obj["alive"]:
            continue

        obj_type = obj["type"]

        # Collectibles
        if obj_type in ("cheesesteak", "pretzel"):
            if boxes_overlap(GRITTY_X, gritty_y, GRITTY_HW, GRITTY_HH,
                             obj["x"], obj["y"], COLLECT_HW, COLLECT_HH):
                if obj_type == "cheesesteak":
                    cheesesteaks_collected += 1
                    score += 100
                    log("Cheesesteak! (%d collected)" % cheesesteaks_collected)
                else:
                    score += 50
                    log("Soft pretzel! +50")
                destroy_object(obj)

        # Obstacles (only hit if not jumping high enough)
        elif obj_type in ("pigeon", "trashcan", "parkingboot", "towhook"):
            if boxes_overlap(GRITTY_X, gritty_y, GRITTY_HW, GRITTY_HH,
                             obj["x"], obj["y"], OBSTACLE_HW, OBSTACLE_HH):
                # Hit! Gritty bounces (not game over, just a setback)
                if not jumping or gritty_y < obj["y"] + OBSTACLE_HH:
                    score = max(0, score - 25)
                    log("Ouch! Gritty hit a %s! (-25 points)" % obj_type)
                    # Bounce Gritty up
                    gritty_vy = JUMP_VEL * 0.6
                    jumping = True
                    destroy_object(obj)

        # Boss collision
        elif obj_type in ("boss_driver", "boss_supervisor", "boss_commissioner"):
            if boxes_overlap(GRITTY_X, gritty_y, GRITTY_HW, GRITTY_HH,
                             obj["x"], obj["y"], OBSTACLE_HW * 1.5, OBSTACLE_HH * 1.5):
                if jumping and gritty_y > obj["y"] + OBSTACLE_HH:
                    # Stomped the boss!
                    boss_hits += 1
                    log("Gritty stomps the boss! (%d/3)" % boss_hits)
                    gritty_vy = JUMP_VEL * 0.8
                    score += 200

                    if boss_hits >= 3:
                        log("Boss defeated!")
                        destroy_object(obj)
                        boss_active = False
                        score += 500

                        # Final boss: spawn Gritty's car!
                        if level == 3 and phase == "impound":
                            log("Wait... is that... GRITTY'S CAR!")
                            car = spawn_object("grittys_car", "grittys_car",
                                               1.0, GROUND_Y, SCROLL_SPEED * 0.5)
                            log("The stash is still in there! Gritty is OVERJOYED!")
                        else:
                            advance_level()
                            return
                else:
                    # Hit by boss
                    score = max(0, score - 50)
                    log("The boss pushes Gritty back! (-50)")
                    gritty_vy = JUMP_VEL * 0.5
                    jumping = True

        # Victory car
        elif obj_type == "grittys_car":
            if boxes_overlap(GRITTY_X, gritty_y, GRITTY_HW, GRITTY_HH,
                             obj["x"], obj["y"], 0.05, 0.03):
                victory = True
                score += 1000
                log("*** GRITTY FOUND HIS CAR! ***")
                log("The stash is still there! He does his drugs and goes for a cheesesteak.")
                log("FINAL SCORE: %d (Cheesesteaks: %d)" % (score, cheesesteaks_collected))
                log("=== GAME OVER: YOU WIN! ===")
                return

    # --- Spawning ---
    if phase == "street":
        spawn_street_objects()
    else:
        spawn_impound_objects()

    # --- Level timer (auto-advance if needed) ---
    if phase_tick > LEVEL_LENGTH + 500 and not boss_active:
        # Failsafe: if greased pole was missed somehow, advance anyway
        advance_level()


# ============================================================================
# Event Registration (must happen at module load time!)
# ============================================================================

# Register game_tick for every frame of gritty sprite
for _frame_i in range(GRITTY_FRAMES):
    def _make_tick(_f):
        def _tick():
            game_tick()
        return _tick
    whenever.sprite_moment("gritty", _frame_i)(_make_tick(_frame_i))

# Jump on click
@whenever.sprite_click("clickzone")
def on_click():
    global jumping, gritty_vy
    if not jumping and not game_over and not victory:
        jumping = True
        gritty_vy = JUMP_VEL
        log("Jump!")


# Initialize on begin
@whenever.begin
def begin():
    global phase, level, level_index

    log("=== GRITTY'S PHILLY ADVENTURE ===")
    log("Gritty woke up hungover. Where's his car?!")
    log("Click anywhere to jump!")
    log("")

    # Create clickzone (topmost layer for catching all clicks)
    create_sprite("clickzone", "clickzone")
    get_sprite("clickzone").setLayer(100)
    set_sprite_location("clickzone", 0.5, 0.5)

    # Create Gritty
    create_sprite("gritty", "gritty")
    get_sprite("gritty").setLayer(50)
    set_sprite_location("gritty", GRITTY_X, GROUND_Y)

    # Start first level
    level_index = 0
    phase, level = level_sequence[level_index]
    start_phase()
