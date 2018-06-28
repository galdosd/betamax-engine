package com.github.galdosd.betamax.scripting;

import com.github.galdosd.betamax.sprite.Sprite;
import com.github.galdosd.betamax.sprite.SpriteRegistry;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * FIXME: Document this class
 */
public class ScriptWorld implements LogicHandler {
    private final ScriptEngine scriptEngine;
    private final SpriteRegistry spriteRegistry;
    public ScriptWorld(SpriteRegistry spriteRegistry) {
        this.spriteRegistry = spriteRegistry;
        scriptEngine = new ScriptEngineManager().getEngineByName("python");
    }


    @Override public void onSpriteEvent(Sprite sprite, EventType eventType) {

    }

    @Override public void onBegin() {
        spriteRegistry.createSprite("room", "sprite_room");
        spriteRegistry.createSprite("demowalk", "sprite_demowalk");
    }

    public void loadScript(String scriptName) {
        // FIXME implement
        //scriptEngine.eval()
    }
}
