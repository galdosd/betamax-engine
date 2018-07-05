package com.github.galdosd.betamax.scripting;

import com.github.galdosd.betamax.sprite.Sprite;
import com.github.galdosd.betamax.sprite.SpriteName;

import java.util.Map;
import java.util.function.Consumer;

/** Provide all needed services to the python scripts. Used by betamax.py
 */
public interface ScriptServicer {
    void log(String msg);
    void fatal(String msg);
    void exit();

    /**
     * the values claim to be Objects but we're being lazy about type system here
     * They must be Strings, Integers, or Booleans
     * Support may be added for nested lists and maybe even maps if needed
     */
    Map<String, Object> getState(); // TODO is this the right interface for this?

    // FIXME spritenames should be a typesafe String enum dammit
    Sprite getSpriteByName(SpriteName spriteName);
    void createSprite(String templateName, SpriteName spriteName);
    void destroySprite(SpriteName spriteName);

    /** moment should be 0 unless eventType is SPRITE_MOMENT */
    void registerCallback(EventType eventType, SpriteName spriteName, int moment, SpriteCallback spriteCallback);

    SpriteName spriteName(String name);

    interface SpriteCallback {
        void invoke();
    }
}
