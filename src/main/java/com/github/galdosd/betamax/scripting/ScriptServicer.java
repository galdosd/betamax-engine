package com.github.galdosd.betamax.scripting;

import java.util.Map;

/** Provide all needed services to the python scripts. Used by betamax.py
 */
public interface ScriptServicer {

    void provideScriptWorld(ScriptWorld scriptWorld);

    void log(String msg);

    void fatal(String msg);

    void exit();

    /**
     * the values claim to be Objects but we're being lazy about type system here
     * They must be Strings, Integers, or Booleans
     * Support may be added for nested lists and maybe even maps if needed
     */
    Map<String, Object> getState(); // TODO is this the right interface for this?

    void createSprite(String templateName, String spriteName);

    void destroySprite(String spriteName);

    void resetSpriteFramecount(String spriteName);

    void resetSpriteFramecount(String spriteName, int framecount);

    int getSpriteFramecount(String spriteName);

    void advanceSpriteFramecount(String spriteName, int framecount);
}
