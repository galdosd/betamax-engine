from betamax import create_sprite, whenever, set_sprite_location, log

@whenever.begin
def begin():
    log("Smoke test starting!")
    sprite = create_sprite("testsprite", "sprite_test")
    set_sprite_location("sprite_test", 0.5, 0.5)

@whenever.sprite_create("sprite_test")
def on_create():
    log("Test sprite created successfully!")
