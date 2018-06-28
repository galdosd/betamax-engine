package com.github.galdosd.betamax.scripting;

import com.github.galdosd.betamax.sprite.Sprite;

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
    Sprite getSpriteByName(String spriteName);
    void createSprite(String templateName, String spriteName);
    void destroySprite(String spriteName);

    interface SpriteCallback {
        void invoke();
    }
    void registerCallback(EventType eventType, String spriteName, SpriteCallback spriteCallback);

}
