from betamax import log, destroy_sprite, create_sprite, whenever, load_template, state, set_sprite_location, set_global_shader

# A simple example that demonstrates some (but nowhere near all) of betamax's scriptable capabilities

state(
    walkcount = 0,
)

@whenever.sprite_click("sprite_demowalk")
def click_walker():
    destroy_sprite("sprite_demowalk")
    create_sprite("demogameover", "sprite_demogameover")
    set_sprite_location("sprite_room", 0.75, 1.0)
    set_global_shader("HIGHLIGHT")

@whenever.sprite_moment("sprite_demowalk", "namesample", "demowalk")
def log_demo():
    log("log_demo " + str(state.walkcount))
    if state.walkcount == 30:
        destroy_sprite("sprite_room")
    state.walkcount += 1

@whenever.sprite_destroy("sprite_room")
def log_destroy():
    log("You are tearing me apart")

@whenever.sprite_create("sprite_room")
def log_create():
    log("I think the room mighta gotten created lol")

@whenever.begin
def begin_little_demo():
    # you don't have to call them sprite_blah, i'm just trying to make it more clear which one is the
    # template name and which is the sprite name
    room_sprite = create_sprite("room", "sprite_room")
    # room_sprite.setPinnedToCursor(True)
    sprite = create_sprite("demowalk", "sprite_demowalk")
    sprite.setRepetitions(2)

