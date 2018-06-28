package com.github.galdosd.betamax.scripting;

import com.github.galdosd.betamax.sprite.Sprite;
import com.github.galdosd.betamax.sprite.SpriteRegistry;

import java.util.Map;

/**
 * FIXME: Document this class
 */
public class ScriptWorld implements LogicHandler {
    private final SpriteRegistry spriteRegistry;
    public ScriptWorld(SpriteRegistry spriteRegistry) {
        this.spriteRegistry = spriteRegistry;
    }


    // FIXME implement these
    @Override public void onSpriteCreate(Sprite sprite) { }
    @Override public void onSpriteDestroy(Sprite sprite) { }
    @Override public void onSpriteFrame(Sprite sprite) { }
    @Override public void onSpriteClick(Sprite sprite) { }

    @Override public void onBegin() {
        spriteRegistry.createSprite("room", "sprite_room");
        spriteRegistry.createSprite("demowalk", "sprite_demowalk");
    }

    public void loadScript(Object mainScript) {
        // FIXME implement
    }
}
