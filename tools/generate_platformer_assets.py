#!/usr/bin/env python3
"""
Generate pixel art assets for "Gritty's Philly Adventure" platformer demo.

All sprites are 960x540 RGBA TIFFs with pixel art drawn at specific positions
on transparent backgrounds. The engine stretches each sprite to fill the window
as a fullscreen quad.

Usage:
    python3 tools/generate_platformer_assets.py [output_base_dir]

Default output: src/main/resources/com/github/galdosd/betamax/sprites/
"""
import os
import sys
from PIL import Image, ImageDraw

W, H = 960, 540
CX, CY = W // 2, H // 2  # canvas center

SPRITE_BASE = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "src", "main", "resources", "com", "github", "galdosd", "betamax", "sprites"
)

if len(sys.argv) > 1:
    SPRITE_BASE = sys.argv[1]


def new_frame():
    """Create a new transparent 960x540 RGBA image."""
    return Image.new('RGBA', (W, H), (0, 0, 0, 0))


def save_frame(img, template_name, frame_num):
    """Save a frame as a TIFF in the proper sprite directory."""
    d = os.path.join(SPRITE_BASE, template_name)
    os.makedirs(d, exist_ok=True)
    path = os.path.join(d, "frame_%04d.tif" % frame_num)
    img.save(path)
    return path


def px(draw, x, y, color, size=1):
    """Draw a pixel (or block of pixels) at canvas position."""
    if size == 1:
        draw.point((x, y), fill=color)
    else:
        draw.rectangle([x, y, x + size - 1, y + size - 1], fill=color)


def rect(draw, x, y, w, h, color):
    """Draw a filled rectangle."""
    draw.rectangle([x, y, x + w - 1, y + h - 1], fill=color)


# =============================================================================
# Color Palette (NES-inspired)
# =============================================================================
SKY_BLUE = (92, 148, 252, 255)
SKY_LIGHT = (146, 184, 254, 255)
GROUND_GRAY = (160, 160, 160, 255)
SIDEWALK = (190, 190, 185, 255)
CURB_GRAY = (120, 120, 120, 255)
DIRT_BROWN = (130, 90, 50, 255)
BRICK_RED = (180, 60, 50, 255)
BRICK_DARK = (140, 40, 35, 255)

# Gritty colors
GRITTY_ORANGE = (255, 130, 0, 255)
GRITTY_DARK_ORANGE = (200, 100, 0, 255)
GRITTY_BELLY = (255, 160, 50, 255)
GRITTY_EYE_WHITE = (255, 255, 255, 255)
GRITTY_EYE_BLACK = (0, 0, 0, 255)
GRITTY_MOUTH = (40, 40, 40, 255)

# Philly colors
SKYLINE_DARK = (40, 45, 60, 255)
SKYLINE_MED = (60, 65, 80, 255)
PIPE_GREEN = (0, 160, 0, 255)  # reused for greased pole base
POLE_SILVER = (200, 210, 220, 255)
POLE_SHINE = (240, 245, 255, 255)
POLE_DARK = (150, 155, 165, 255)

PIGEON_GRAY = (140, 145, 155, 255)
PIGEON_DARK = (100, 105, 115, 255)
PIGEON_HEAD = (80, 120, 80, 255)
PIGEON_BEAK = (220, 180, 50, 255)
PIGEON_EYE = (200, 50, 0, 255)

CHEESESTEAK_BREAD = (220, 180, 100, 255)
CHEESESTEAK_BREAD_DARK = (190, 150, 80, 255)
CHEESESTEAK_CHEESE = (255, 220, 80, 255)
CHEESESTEAK_MEAT = (160, 80, 50, 255)
CHEESESTEAK_SPARKLE = (255, 255, 200, 255)

CLOUD_WHITE = (255, 255, 255, 255)
CLOUD_LIGHT = (235, 240, 250, 255)

TRASHCAN_GREEN = (60, 100, 60, 255)
TRASHCAN_DARK = (40, 70, 40, 255)
TRASHCAN_LID = (80, 120, 80, 255)

BOOT_YELLOW = (255, 200, 0, 255)
BOOT_DARK = (200, 150, 0, 255)
BOOT_CLAMP = (100, 100, 100, 255)

IMPOUND_SKY = (30, 20, 40, 255)
IMPOUND_FENCE = (160, 170, 175, 255)
IMPOUND_GROUND = (100, 100, 95, 255)
IMPOUND_CONCRETE = (140, 140, 135, 255)

BELL_BRONZE = (180, 140, 60, 255)
BELL_DARK = (140, 100, 40, 255)
BELL_SHINE = (220, 180, 80, 255)

PRETZEL_BROWN = (180, 120, 50, 255)
PRETZEL_DARK = (140, 90, 30, 255)
PRETZEL_SALT = (240, 240, 230, 255)

BOSS_SHIRT_BLUE = (30, 50, 120, 255)
BOSS_PANTS = (40, 40, 60, 255)
BOSS_SKIN = (210, 170, 130, 255)
BOSS_HAT = (20, 30, 60, 255)
BOSS_BADGE = (220, 200, 50, 255)

CAR_ORANGE = (230, 120, 0, 255)
CAR_DARK = (180, 90, 0, 255)
CAR_WINDOW = (100, 150, 200, 255)
CAR_TIRE = (40, 40, 40, 255)
CAR_RUST = (160, 80, 30, 255)

TOWHOOK_METAL = (170, 170, 175, 255)
TOWHOOK_DARK = (120, 120, 130, 255)
TOWHOOK_CHAIN = (150, 150, 160, 255)

# Scale factor for pixel art (each "pixel" becomes this many real pixels)
S = 4


# =============================================================================
# Background: Philly Street
# =============================================================================
def generate_bg_street():
    img = new_frame()
    draw = ImageDraw.Draw(img)

    # Sky gradient
    for y in range(H):
        if y < H * 0.15:
            c = SKY_LIGHT
        elif y < H * 0.5:
            t = (y - H * 0.15) / (H * 0.35)
            r = int(SKY_LIGHT[0] + (SKY_BLUE[0] - SKY_LIGHT[0]) * t)
            g = int(SKY_LIGHT[1] + (SKY_BLUE[1] - SKY_LIGHT[1]) * t)
            b = int(SKY_LIGHT[2] + (SKY_BLUE[2] - SKY_LIGHT[2]) * t)
            c = (r, g, b, 255)
        else:
            c = SKY_BLUE
        draw.line([(0, y), (W - 1, y)], fill=c)

    # Skyline silhouette (buildings of varying heights)
    buildings = [
        (50, 180, 60),    # narrow tall
        (120, 220, 80),   # wide medium
        (210, 160, 40),   # narrow short
        (260, 250, 70),   # Comcast tower (tallest)
        (340, 200, 55),
        (410, 170, 45),
        (465, 230, 65),   # City Hall-ish
        (540, 190, 50),
        (600, 210, 60),
        (670, 180, 40),
        (720, 240, 70),
        (800, 200, 55),
        (865, 170, 45),
    ]
    ground_top = int(H * 0.78)  # where the ground starts

    for bx, bh, bw in buildings:
        by = ground_top - bh
        # Building body
        rect(draw, bx, by, bw, bh, SKYLINE_DARK)
        # Windows (little yellow dots)
        for wy in range(by + 8, by + bh - 8, 12):
            for wx in range(bx + 6, bx + bw - 6, 10):
                if (wx + wy) % 3 != 0:  # some windows lit, some dark
                    rect(draw, wx, wy, 4, 6, (255, 230, 120, 255))
                else:
                    rect(draw, wx, wy, 4, 6, SKYLINE_MED)

    # Ground: sidewalk
    rect(draw, 0, ground_top, W, H - ground_top, SIDEWALK)

    # Curb line
    rect(draw, 0, ground_top, W, 4, CURB_GRAY)

    # Sidewalk cracks
    for x in range(0, W, 80):
        draw.line([(x, ground_top + 4), (x, H - 1)], fill=CURB_GRAY, width=1)

    # Street at very bottom (darker)
    street_top = int(H * 0.92)
    rect(draw, 0, street_top, W, H - street_top, (80, 80, 80, 255))
    # Lane marking
    for x in range(0, W, 60):
        rect(draw, x, street_top + 8, 30, 3, (220, 220, 50, 255))

    save_frame(img, "bg_street", 0)
    print("  bg_street: 1 frame")


# =============================================================================
# Background: PPA Impound Lot
# =============================================================================
def generate_bg_impound():
    img = new_frame()
    draw = ImageDraw.Draw(img)

    # Dark sky
    for y in range(H):
        t = y / H
        r = int(IMPOUND_SKY[0] * (1 - t * 0.3))
        g = int(IMPOUND_SKY[1] * (1 - t * 0.3))
        b = int(IMPOUND_SKY[2] * (1 - t * 0.3))
        draw.line([(0, y), (W - 1, y)], fill=(r, g, b, 255))

    ground_top = int(H * 0.78)

    # Chain-link fence posts
    for x in range(0, W, 100):
        rect(draw, x, ground_top - 120, 6, 120, IMPOUND_FENCE)
        # Post cap
        rect(draw, x - 2, ground_top - 124, 10, 4, IMPOUND_FENCE)

    # Chain-link pattern (diagonal hatching)
    for y in range(ground_top - 120, ground_top, 8):
        for x in range(0, W, 8):
            if (x + y) % 16 < 8:
                draw.line([(x, y), (x + 8, y + 8)], fill=(140, 150, 155, 100), width=1)
            else:
                draw.line([(x + 8, y), (x, y + 8)], fill=(140, 150, 155, 100), width=1)

    # "PPA IMPOUND LOT" sign
    sign_x, sign_y = 350, ground_top - 100
    rect(draw, sign_x, sign_y, 260, 40, (200, 30, 30, 255))
    rect(draw, sign_x + 2, sign_y + 2, 256, 36, (220, 50, 50, 255))
    # P P A text (pixel letters)
    _draw_text_ppa(draw, sign_x + 20, sign_y + 8)

    # Concrete ground
    rect(draw, 0, ground_top, W, H - ground_top, IMPOUND_CONCRETE)
    rect(draw, 0, ground_top, W, 4, (100, 100, 95, 255))

    # Parking lines
    for x in range(60, W, 120):
        rect(draw, x, ground_top + 10, 3, H - ground_top - 10, (220, 220, 50, 180))

    # Oil stains
    for ox in [150, 400, 700]:
        for dx in range(-8, 9):
            for dy in range(-4, 5):
                if dx * dx + dy * dy * 4 < 60:
                    px(draw, ox + dx, ground_top + 30 + dy, (60, 50, 40, 120), 2)

    save_frame(img, "bg_impound", 0)
    print("  bg_impound: 1 frame")


def _draw_text_ppa(draw, x, y):
    """Draw 'PPA IMPOUND' in blocky pixel font."""
    # Simple block letters, each ~8px wide
    white = (255, 255, 255, 255)
    # P
    rect(draw, x, y, 3, 20, white)
    rect(draw, x, y, 10, 3, white)
    rect(draw, x + 8, y, 3, 12, white)
    rect(draw, x, y + 10, 10, 3, white)
    # P
    x2 = x + 16
    rect(draw, x2, y, 3, 20, white)
    rect(draw, x2, y, 10, 3, white)
    rect(draw, x2 + 8, y, 3, 12, white)
    rect(draw, x2, y + 10, 10, 3, white)
    # A
    x3 = x + 32
    rect(draw, x3, y, 3, 20, white)
    rect(draw, x3 + 8, y, 3, 20, white)
    rect(draw, x3, y, 10, 3, white)
    rect(draw, x3, y + 10, 10, 3, white)
    # Space then IMPOUND in smaller text
    xs = x + 60
    for i, ch in enumerate("IMPOUND"):
        rect(draw, xs + i * 8, y + 4, 3, 14, white)
        if ch in ('M', 'W', 'D', 'O', 'U', 'N'):
            rect(draw, xs + i * 8 + 4, y + 4, 3, 14, white)
        rect(draw, xs + i * 8, y + 4, 6, 3, white)


# =============================================================================
# Gritty (the player character)
# =============================================================================
def draw_gritty(draw, ox, oy, leg_offset=0, arms_up=False):
    """Draw Gritty at canvas position (ox, oy) = bottom-center of character.
    Character is approximately 24x32 pixels (at scale S).
    """
    s = S
    # Body (orange fuzzy oval)
    for dy in range(-7, 1):
        w = 5 if dy > -3 else (4 if dy > -6 else 3)
        for dx in range(-w, w + 1):
            c = GRITTY_ORANGE if (dx + dy) % 3 != 0 else GRITTY_DARK_ORANGE
            px(draw, ox + dx * s, oy + dy * s, c, s)

    # Belly
    for dy in range(-4, -1):
        for dx in range(-3, 4):
            if abs(dx) + abs(dy + 3) < 4:
                px(draw, ox + dx * s, oy + dy * s, GRITTY_BELLY, s)

    # Head (big round orange head)
    for dy in range(-14, -7):
        w = 5 if -12 < dy < -8 else (4 if dy > -13 else 3)
        for dx in range(-w, w + 1):
            c = GRITTY_ORANGE if (dx + dy) % 2 != 0 else GRITTY_DARK_ORANGE
            px(draw, ox + dx * s, oy + dy * s, c, s)

    # Googly eyes (Gritty's signature feature!)
    # Left eye - white circle with black pupil
    for dx in range(-4, -1):
        for dy in range(-13, -10):
            px(draw, ox + dx * s, oy + dy * s, GRITTY_EYE_WHITE, s)
    px(draw, ox + (-3) * s, oy + (-12) * s, GRITTY_EYE_BLACK, s)
    px(draw, ox + (-2) * s, oy + (-12) * s, GRITTY_EYE_BLACK, s)

    # Right eye
    for dx in range(1, 4):
        for dy in range(-13, -10):
            px(draw, ox + dx * s, oy + dy * s, GRITTY_EYE_WHITE, s)
    px(draw, ox + (2) * s, oy + (-12) * s, GRITTY_EYE_BLACK, s)
    px(draw, ox + (1) * s, oy + (-12) * s, GRITTY_EYE_BLACK, s)

    # Mouth (wide grin)
    for dx in range(-3, 4):
        px(draw, ox + dx * s, oy + (-9) * s, GRITTY_MOUTH, s)
    # Teeth
    for dx in range(-2, 3):
        if dx % 2 == 0:
            px(draw, ox + dx * s, oy + (-9) * s, GRITTY_EYE_WHITE, s)

    # Arms
    if arms_up:
        # Jumping pose - arms up
        for dy in range(-10, -7):
            px(draw, ox + (-6) * s, oy + dy * s, GRITTY_ORANGE, s)
            px(draw, ox + (6) * s, oy + dy * s, GRITTY_ORANGE, s)
    else:
        # Running arms
        px(draw, ox + (-6) * s, oy + (-5 + leg_offset) * s, GRITTY_ORANGE, s)
        px(draw, ox + (-6) * s, oy + (-4 + leg_offset) * s, GRITTY_ORANGE, s)
        px(draw, ox + (6) * s, oy + (-5 - leg_offset) * s, GRITTY_ORANGE, s)
        px(draw, ox + (6) * s, oy + (-4 - leg_offset) * s, GRITTY_ORANGE, s)

    # Legs
    # Left leg
    for dy in range(1, 4):
        px(draw, ox + (-2 - leg_offset) * s, oy + dy * s, GRITTY_DARK_ORANGE, s)
    # Right leg
    for dy in range(1, 4):
        px(draw, ox + (2 + leg_offset) * s, oy + dy * s, GRITTY_DARK_ORANGE, s)

    # Shoes (black)
    px(draw, ox + (-2 - leg_offset) * s, oy + 4 * s, GRITTY_EYE_BLACK, s)
    px(draw, ox + (-3 - leg_offset) * s, oy + 4 * s, GRITTY_EYE_BLACK, s)
    px(draw, ox + (2 + leg_offset) * s, oy + 4 * s, GRITTY_EYE_BLACK, s)
    px(draw, ox + (3 + leg_offset) * s, oy + 4 * s, GRITTY_EYE_BLACK, s)


def generate_gritty():
    frames = []
    # Frame 0: run pose 1 (left leg forward)
    img = new_frame()
    draw_gritty(ImageDraw.Draw(img), CX, CY, leg_offset=1, arms_up=False)
    frames.append(img)

    # Frame 1: run pose 2 (right leg forward)
    img = new_frame()
    draw_gritty(ImageDraw.Draw(img), CX, CY, leg_offset=-1, arms_up=False)
    frames.append(img)

    # Frame 2: standing
    img = new_frame()
    draw_gritty(ImageDraw.Draw(img), CX, CY, leg_offset=0, arms_up=False)
    frames.append(img)

    # Frame 3: jumping (arms up)
    img = new_frame()
    draw_gritty(ImageDraw.Draw(img), CX, CY, leg_offset=0, arms_up=True)
    frames.append(img)

    # Frame 4-5: repeat run cycle for smooth looping
    img = new_frame()
    draw_gritty(ImageDraw.Draw(img), CX, CY, leg_offset=1, arms_up=False)
    frames.append(img)

    img = new_frame()
    draw_gritty(ImageDraw.Draw(img), CX, CY, leg_offset=-1, arms_up=False)
    frames.append(img)

    for i, f in enumerate(frames):
        save_frame(f, "gritty", i)
    print("  gritty: %d frames" % len(frames))


# =============================================================================
# Pigeon (enemy)
# =============================================================================
def draw_pigeon(draw, ox, oy, frame=0):
    """Draw a Philly pigeon. ~20x16 pixels at scale."""
    s = S
    leg_off = 1 if frame == 0 else -1

    # Body (oval)
    for dy in range(-3, 2):
        w = 4 if -2 <= dy <= 0 else 3
        for dx in range(-w, w + 1):
            c = PIGEON_GRAY if (dx + dy) % 3 != 0 else PIGEON_DARK
            px(draw, ox + dx * s, oy + dy * s, c, s)

    # Head
    for dy in range(-6, -3):
        w = 2 if dy == -5 else (2 if dy == -4 else 1)
        for dx in range(2, 2 + w + 1):
            px(draw, ox + dx * s, oy + dy * s, PIGEON_HEAD, s)

    # Eye
    px(draw, ox + 3 * s, oy + (-5) * s, PIGEON_EYE, s)

    # Beak
    px(draw, ox + 5 * s, oy + (-4) * s, PIGEON_BEAK, s)

    # Tail feathers
    for dx in range(-5, -3):
        px(draw, ox + dx * s, oy + (-1) * s, PIGEON_DARK, s)

    # Legs
    px(draw, ox + (0 + leg_off) * s, oy + 2 * s, (180, 120, 60, 255), s)
    px(draw, ox + (0 + leg_off) * s, oy + 3 * s, (180, 120, 60, 255), s)
    px(draw, ox + (2 - leg_off) * s, oy + 2 * s, (180, 120, 60, 255), s)
    px(draw, ox + (2 - leg_off) * s, oy + 3 * s, (180, 120, 60, 255), s)


def generate_pigeon():
    for frame in range(2):
        img = new_frame()
        draw_pigeon(ImageDraw.Draw(img), CX, CY, frame)
        save_frame(img, "pigeon", frame)
    print("  pigeon: 2 frames")


# =============================================================================
# Trash Can (obstacle)
# =============================================================================
def generate_trashcan():
    img = new_frame()
    draw = ImageDraw.Draw(img)
    s = S
    ox, oy = CX, CY

    # Can body
    for dy in range(-6, 4):
        w = 4 if dy > -4 else 3
        for dx in range(-w, w + 1):
            c = TRASHCAN_GREEN if (dx + dy) % 4 != 0 else TRASHCAN_DARK
            px(draw, ox + dx * s, oy + dy * s, c, s)

    # Lid
    for dx in range(-5, 6):
        px(draw, ox + dx * s, oy + (-7) * s, TRASHCAN_LID, s)
        px(draw, ox + dx * s, oy + (-8) * s, TRASHCAN_LID, s)

    # Handle on lid
    for dx in range(-1, 2):
        px(draw, ox + dx * s, oy + (-9) * s, TRASHCAN_DARK, s)

    # Trash sticking out
    px(draw, ox + (-3) * s, oy + (-9) * s, (200, 200, 180, 255), s)
    px(draw, ox + (2) * s, oy + (-10) * s, (180, 170, 150, 255), s)

    save_frame(img, "trashcan", 0)
    print("  trashcan: 1 frame")


# =============================================================================
# Cheesesteak (collectible)
# =============================================================================
def draw_cheesesteak(draw, ox, oy, sparkle_pos=0):
    s = S
    # Bread (hoagie roll shape)
    for dx in range(-6, 7):
        h = 2 if abs(dx) < 5 else 1
        for dy in range(-h, h + 1):
            c = CHEESESTEAK_BREAD if dy < 0 else CHEESESTEAK_BREAD_DARK
            px(draw, ox + dx * s, oy + dy * s, c, s)

    # Cheese (whiz, dripping)
    for dx in range(-5, 6):
        px(draw, ox + dx * s, oy + (-1) * s, CHEESESTEAK_CHEESE, s)
    for dx in range(-3, 4, 2):
        px(draw, ox + dx * s, oy + 0 * s, CHEESESTEAK_CHEESE, s)

    # Meat visible
    for dx in range(-4, 5):
        px(draw, ox + dx * s, oy + 0 * s, CHEESESTEAK_MEAT, s)

    # Sparkle
    sparkle_positions = [(-8, -4), (7, -3), (-6, 3), (8, 2)]
    if 0 <= sparkle_pos < len(sparkle_positions):
        sx, sy = sparkle_positions[sparkle_pos]
        px(draw, ox + sx * s, oy + sy * s, CHEESESTEAK_SPARKLE, s)
        px(draw, ox + (sx - 1) * s, oy + sy * s, CHEESESTEAK_SPARKLE, s)
        px(draw, ox + (sx + 1) * s, oy + sy * s, CHEESESTEAK_SPARKLE, s)
        px(draw, ox + sx * s, oy + (sy - 1) * s, CHEESESTEAK_SPARKLE, s)
        px(draw, ox + sx * s, oy + (sy + 1) * s, CHEESESTEAK_SPARKLE, s)


def generate_cheesesteak():
    for frame in range(4):
        img = new_frame()
        draw_cheesesteak(ImageDraw.Draw(img), CX, CY, sparkle_pos=frame)
        save_frame(img, "cheesesteak", frame)
    print("  cheesesteak: 4 frames")


# =============================================================================
# Greased Pole (level end goal)
# =============================================================================
def generate_greasedpole():
    img = new_frame()
    draw = ImageDraw.Draw(img)
    s = S
    ox, oy = CX, CY

    # Pole shaft
    for dy in range(-16, 8):
        for dx in range(-1, 2):
            c = POLE_SILVER
            if dx == 0:
                c = POLE_SHINE  # center highlight (greased!)
            elif dx == -1:
                c = POLE_DARK
            px(draw, ox + dx * s, oy + dy * s, c, s)

    # Base
    for dx in range(-3, 4):
        for dy in range(6, 8):
            px(draw, ox + dx * s, oy + dy * s, POLE_DARK, s)

    # Grease drips (shiny spots)
    for dy in [-10, -5, 0, 4]:
        px(draw, ox + 2 * s, oy + dy * s, (255, 255, 255, 180), s)

    # Flag at top
    for dy in range(-16, -12):
        for dx in range(2, 8):
            c = (255, 100, 0, 255) if (dx + dy) % 2 == 0 else (0, 0, 0, 255)
            px(draw, ox + dx * s, oy + dy * s, c, s)

    save_frame(img, "greasedpole", 0)
    print("  greasedpole: 1 frame")


# =============================================================================
# Cloud (decoration)
# =============================================================================
def generate_cloud():
    img = new_frame()
    draw = ImageDraw.Draw(img)
    s = S
    ox, oy = CX, CY

    # Fluffy cloud shape
    cloud_pixels = []
    for dx in range(-10, 11):
        max_dy = 2
        if abs(dx) < 6:
            max_dy = 4
        if abs(dx) < 3:
            max_dy = 5
        for dy in range(-max_dy, max_dy + 1):
            r2 = (dx * dx / 100.0 + dy * dy / 25.0)
            if r2 < 1.0:
                c = CLOUD_WHITE if dy < 0 else CLOUD_LIGHT
                px(draw, ox + dx * s, oy + dy * s, c, s)

    save_frame(img, "cloud", 0)
    print("  cloud: 1 frame")


# =============================================================================
# Liberty Bell (background decoration)
# =============================================================================
def generate_libertybell():
    img = new_frame()
    draw = ImageDraw.Draw(img)
    s = S
    ox, oy = CX, CY

    # Bell shape (wider at bottom)
    for dy in range(-8, 6):
        if dy < -5:
            w = 2
        elif dy < -2:
            w = 3 + (dy + 5)
        elif dy < 2:
            w = 6
        else:
            w = 7
        for dx in range(-w, w + 1):
            c = BELL_BRONZE
            if dx == -1 or dx == 0:
                c = BELL_SHINE  # center highlight
            elif abs(dx) == w:
                c = BELL_DARK  # edge shadow
            px(draw, ox + dx * s, oy + dy * s, c, s)

    # Crack! (the famous crack)
    crack_points = [(0, -4), (1, -3), (0, -2), (-1, -1), (0, 0), (1, 1), (0, 2)]
    for cx_off, cy_off in crack_points:
        px(draw, ox + cx_off * s, oy + cy_off * s, (40, 30, 20, 255), s)

    # Yoke (top mount)
    for dx in range(-3, 4):
        px(draw, ox + dx * s, oy + (-9) * s, (100, 80, 50, 255), s)
    for dx in range(-1, 2):
        px(draw, ox + dx * s, oy + (-10) * s, (100, 80, 50, 255), s)

    # Clapper
    px(draw, ox, oy + 6 * s, BELL_DARK, s)
    px(draw, ox, oy + 7 * s, BELL_DARK, s)

    save_frame(img, "libertybell", 0)
    print("  libertybell: 1 frame")


# =============================================================================
# Pretzel (collectible)
# =============================================================================
def generate_pretzel():
    img = new_frame()
    draw = ImageDraw.Draw(img)
    s = S
    ox, oy = CX, CY

    # Pretzel shape (twisted loops)
    # Top crossing
    for dx in range(-4, 5):
        for dy in range(-5, 6):
            # Two overlapping circles
            d1 = ((dx + 2) ** 2 + dy ** 2)
            d2 = ((dx - 2) ** 2 + dy ** 2)
            on_ring1 = 6 <= d1 <= 16
            on_ring2 = 6 <= d2 <= 16
            if on_ring1 or on_ring2:
                c = PRETZEL_BROWN if (dx + dy) % 3 != 0 else PRETZEL_DARK
                px(draw, ox + dx * s, oy + dy * s, c, s)

    # Salt crystals
    salt_spots = [(-2, -4), (3, -3), (0, 2), (-3, 1), (2, 3)]
    for sx_off, sy_off in salt_spots:
        px(draw, ox + sx_off * s, oy + sy_off * s, PRETZEL_SALT, s)

    save_frame(img, "pretzel", 0)
    print("  pretzel: 1 frame")


# =============================================================================
# Parking Boot (impound lot trap)
# =============================================================================
def generate_parkingboot():
    for frame in range(2):
        img = new_frame()
        draw = ImageDraw.Draw(img)
        s = S
        ox, oy = CX, CY
        open_offset = 2 if frame == 1 else 0  # frame 1 = snapping open

        # Boot body (yellow clamp)
        for dy in range(-3, 4):
            for dx in range(-5, 6):
                if abs(dy) < 3 and abs(dx) < 5:
                    px(draw, ox + dx * s, oy + (dy - open_offset) * s, BOOT_YELLOW, s)

        # Clamp jaws
        for dx in range(-6, 7):
            px(draw, ox + dx * s, oy + (-3 - open_offset) * s, BOOT_DARK, s)
            px(draw, ox + dx * s, oy + (3 + open_offset) * s, BOOT_DARK, s)

        # Lock
        for dx in range(-1, 2):
            for dy in range(-2, 1):
                px(draw, ox + dx * s, oy + dy * s, BOOT_CLAMP, s)

        # Keyhole
        px(draw, ox, oy + (-1) * s, GRITTY_EYE_BLACK, s)

        save_frame(img, "parkingboot", frame)
    print("  parkingboot: 2 frames")


# =============================================================================
# Tow Hook (impound lot obstacle)
# =============================================================================
def generate_towhook():
    for frame in range(2):
        img = new_frame()
        draw = ImageDraw.Draw(img)
        s = S
        ox, oy = CX, CY
        swing = 2 if frame == 1 else -2  # swinging back and forth

        # Chain links going up
        for dy in range(-12, -4):
            px(draw, ox + swing * s // 2, oy + dy * s, TOWHOOK_CHAIN, s)
            if dy % 2 == 0:
                px(draw, ox + swing * s // 2 + s, oy + dy * s, TOWHOOK_CHAIN, s)

        # Hook
        for dy in range(-4, 2):
            px(draw, ox + swing * s, oy + dy * s, TOWHOOK_METAL, s)
        for dx in range(0, 4):
            px(draw, ox + (swing + dx) * s, oy + 2 * s, TOWHOOK_METAL, s)
        px(draw, ox + (swing + 3) * s, oy + 1 * s, TOWHOOK_METAL, s)
        px(draw, ox + (swing + 3) * s, oy + 0 * s, TOWHOOK_DARK, s)

        save_frame(img, "towhook", frame)
    print("  towhook: 2 frames")


# =============================================================================
# Boss: Tow Truck Driver
# =============================================================================
def draw_boss_figure(draw, ox, oy, shirt_color, has_hat=False, has_clipboard=False,
                     has_badge=False, frame=0):
    """Draw a boss character. ~30x40 pixels at scale."""
    s = S
    step = 1 if frame == 0 else -1

    # Legs
    for dy in range(2, 8):
        px(draw, ox + (-2 + step) * s, oy + dy * s, BOSS_PANTS, s)
        px(draw, ox + (-1 + step) * s, oy + dy * s, BOSS_PANTS, s)
        px(draw, ox + (2 - step) * s, oy + dy * s, BOSS_PANTS, s)
        px(draw, ox + (1 - step) * s, oy + dy * s, BOSS_PANTS, s)

    # Shoes
    px(draw, ox + (-3 + step) * s, oy + 8 * s, GRITTY_EYE_BLACK, s)
    px(draw, ox + (-2 + step) * s, oy + 8 * s, GRITTY_EYE_BLACK, s)
    px(draw, ox + (3 - step) * s, oy + 8 * s, GRITTY_EYE_BLACK, s)
    px(draw, ox + (2 - step) * s, oy + 8 * s, GRITTY_EYE_BLACK, s)

    # Body/shirt
    for dy in range(-6, 2):
        w = 4 if -4 < dy < 0 else 3
        for dx in range(-w, w + 1):
            px(draw, ox + dx * s, oy + dy * s, shirt_color, s)

    # Head
    for dy in range(-12, -6):
        w = 3 if -10 < dy < -7 else 2
        for dx in range(-w, w + 1):
            px(draw, ox + dx * s, oy + dy * s, BOSS_SKIN, s)

    # Eyes
    px(draw, ox + (-1) * s, oy + (-10) * s, GRITTY_EYE_BLACK, s)
    px(draw, ox + (1) * s, oy + (-10) * s, GRITTY_EYE_BLACK, s)

    # Angry eyebrows
    px(draw, ox + (-2) * s, oy + (-11) * s, GRITTY_EYE_BLACK, s)
    px(draw, ox + (2) * s, oy + (-11) * s, GRITTY_EYE_BLACK, s)

    # Mouth (angry)
    for dx in range(-1, 2):
        px(draw, ox + dx * s, oy + (-8) * s, (180, 60, 60, 255), s)

    if has_hat:
        for dx in range(-4, 5):
            px(draw, ox + dx * s, oy + (-13) * s, BOSS_HAT, s)
        for dx in range(-3, 4):
            px(draw, ox + dx * s, oy + (-14) * s, BOSS_HAT, s)

    if has_badge:
        px(draw, ox + (-2) * s, oy + (-3) * s, BOSS_BADGE, s)
        px(draw, ox + (-1) * s, oy + (-3) * s, BOSS_BADGE, s)

    if has_clipboard:
        for dy in range(-4, 0):
            px(draw, ox + 5 * s, oy + dy * s, (200, 180, 140, 255), s)
            px(draw, ox + 6 * s, oy + dy * s, (200, 180, 140, 255), s)
        # Clipboard edge
        for dy in range(-5, 0):
            px(draw, ox + 7 * s, oy + dy * s, (120, 80, 40, 255), s)

    # Arms
    arm_off = step
    px(draw, ox + (-5) * s, oy + (-4 + arm_off) * s, BOSS_SKIN, s)
    px(draw, ox + (-5) * s, oy + (-3 + arm_off) * s, BOSS_SKIN, s)
    px(draw, ox + (5) * s, oy + (-4 - arm_off) * s, BOSS_SKIN, s)
    px(draw, ox + (5) * s, oy + (-3 - arm_off) * s, BOSS_SKIN, s)


def generate_boss_driver():
    for frame in range(2):
        img = new_frame()
        draw_boss_figure(ImageDraw.Draw(img), CX, CY,
                         shirt_color=(80, 80, 100, 255),  # work shirt
                         has_hat=True, frame=frame)
        save_frame(img, "boss_driver", frame)
    print("  boss_driver: 2 frames")


def generate_boss_supervisor():
    for frame in range(2):
        img = new_frame()
        draw_boss_figure(ImageDraw.Draw(img), CX, CY,
                         shirt_color=BOSS_SHIRT_BLUE,
                         has_hat=True, has_clipboard=True, has_badge=True, frame=frame)
        save_frame(img, "boss_supervisor", frame)
    print("  boss_supervisor: 2 frames")


def generate_boss_commissioner():
    for frame in range(2):
        img = new_frame()
        draw = ImageDraw.Draw(img)
        draw_boss_figure(draw, CX, CY,
                         shirt_color=(20, 20, 40, 255),  # dark suit
                         has_hat=True, has_badge=True, frame=frame)
        # Extra details: gold epaulettes
        s = S
        for dx in range(-4, -2):
            px(draw, CX + dx * s, CY + (-6) * s, BOSS_BADGE, s)
        for dx in range(3, 5):
            px(draw, CX + dx * s, CY + (-6) * s, BOSS_BADGE, s)
        save_frame(img, "boss_commissioner", frame)
    print("  boss_commissioner: 2 frames")


# =============================================================================
# Gritty's Car (the prize!)
# =============================================================================
def generate_grittys_car():
    img = new_frame()
    draw = ImageDraw.Draw(img)
    s = S
    ox, oy = CX, CY

    # Car body (beat-up orange sedan)
    # Main body
    for dy in range(-4, 3):
        for dx in range(-10, 11):
            c = CAR_ORANGE
            if abs(dx) >= 9:
                c = CAR_DARK  # fenders
            # Rust spots
            if (dx == -5 and dy == 1) or (dx == 7 and dy == 0):
                c = CAR_RUST
            px(draw, ox + dx * s, oy + dy * s, c, s)

    # Roof
    for dy in range(-8, -4):
        w = 6 if dy > -7 else 4
        for dx in range(-w, w + 1):
            px(draw, ox + dx * s, oy + dy * s, CAR_DARK, s)

    # Windows
    for dy in range(-7, -4):
        for dx in range(-5, 6):
            if abs(dx) != 0:  # window divider
                px(draw, ox + dx * s, oy + dy * s, CAR_WINDOW, s)

    # Headlights
    px(draw, ox + 10 * s, oy + (-2) * s, (255, 255, 200, 255), s)
    px(draw, ox + 10 * s, oy + (-1) * s, (255, 255, 200, 255), s)

    # Taillights
    px(draw, ox + (-10) * s, oy + (-2) * s, (255, 40, 40, 255), s)
    px(draw, ox + (-10) * s, oy + (-1) * s, (255, 40, 40, 255), s)

    # Tires
    for t_dx in [-7, 5]:
        for dy in range(3, 6):
            for dx in range(t_dx, t_dx + 4):
                px(draw, ox + dx * s, oy + dy * s, CAR_TIRE, s)
        # Hubcap
        px(draw, ox + (t_dx + 1) * s, oy + 4 * s, (180, 180, 180, 255), s)
        px(draw, ox + (t_dx + 2) * s, oy + 4 * s, (180, 180, 180, 255), s)

    # Bumper sticker area (Flyers logo hint: orange circle)
    for dx in range(-2, 0):
        px(draw, ox + dx * s, oy + 1 * s, (255, 100, 0, 255), s)

    # Dent on the side
    px(draw, ox + 3 * s, oy + 0 * s, CAR_DARK, s)
    px(draw, ox + 4 * s, oy + 0 * s, CAR_DARK, s)

    save_frame(img, "grittys_car", 0)
    print("  grittys_car: 1 frame")


# =============================================================================
# Click Zone (invisible full-screen sprite for click detection)
# =============================================================================
def generate_clickzone():
    # Nearly invisible but has alpha=1 everywhere so clicks register
    img = Image.new('RGBA', (W, H), (0, 0, 0, 1))
    save_frame(img, "clickzone", 0)
    print("  clickzone: 1 frame (alpha=1 overlay)")


# =============================================================================
# Hill (background decoration for street levels)
# =============================================================================
def generate_hill():
    img = new_frame()
    draw = ImageDraw.Draw(img)
    s = S
    ox, oy = CX, CY

    # Green hill silhouette
    for dx in range(-18, 19):
        # Parabolic height
        h = max(0, int(10 * (1 - (dx / 18.0) ** 2)))
        for dy in range(-h, 1):
            shade = max(0, min(255, 80 + dy * 3))
            px(draw, ox + dx * s, oy + dy * s, (40, shade, 30, 255), s)

    save_frame(img, "hill", 0)
    print("  hill: 1 frame")


# =============================================================================
# Main
# =============================================================================
def main():
    print("Generating Gritty's Philly Adventure assets...")
    print("Output: %s" % SPRITE_BASE)
    print()

    generate_bg_street()
    generate_bg_impound()
    generate_gritty()
    generate_pigeon()
    generate_trashcan()
    generate_cheesesteak()
    generate_greasedpole()
    generate_cloud()
    generate_libertybell()
    generate_pretzel()
    generate_parkingboot()
    generate_towhook()
    generate_boss_driver()
    generate_boss_supervisor()
    generate_boss_commissioner()
    generate_grittys_car()
    generate_clickzone()
    generate_hill()

    print()
    print("Done! Generated all sprite assets.")


if __name__ == "__main__":
    main()
