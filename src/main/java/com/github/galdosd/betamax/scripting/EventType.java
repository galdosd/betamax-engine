package com.github.galdosd.betamax.scripting;

/**
 * FIXME: Document this class
 */
public enum EventType {
    // sprite events, specific to exactly one sprite
    SPRITE_CREATE, SPRITE_DESTROY, SPRITE_CLICK,
    SPRITE_MOMENT, // SpriteEvent#moment has meaning for this and only this EventType instance
    // global events, not specific to any single sprite
    BEGIN;
}
