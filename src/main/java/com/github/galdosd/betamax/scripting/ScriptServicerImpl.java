package com.github.galdosd.betamax.scripting;

import com.github.galdosd.betamax.sprite.Sprite;
import com.github.galdosd.betamax.sprite.SpriteName;
import com.github.galdosd.betamax.sprite.SpriteRegistry;
import lombok.Value;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ScriptServicerImpl implements ScriptServicer {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private final SpriteRegistry spriteRegistry;
    public ScriptServicerImpl(SpriteRegistry spriteRegistry) {
        this.spriteRegistry = spriteRegistry;
    }

    @Value private static final class Situation {
        EventType eventType;
        SpriteName spriteName;
        /** ignored and must be 0 except for eventType==SPRITE_MOMENT */
        int moment;
    }

    Map<Situation,SpriteCallback> callbacks = new HashMap<>();

    @Override public void log(String msg) {
        LOG.debug("[jython] {}", msg);
    }

    @Override public void fatal(String msg) {
        throw new RuntimeException("Fatal jython script error: " + msg);
    }

    @Override public void exit() {
        LOG.info("Jython script requested normal exit");
        System.exit(0);
    }

    @Override public Map<String, Object> getState() {
        throw new UnsupportedOperationException("FIXME unimplemented");
    }

    @Override public Sprite getSpriteByName(SpriteName spriteName) {
        return spriteRegistry.getSpriteByName(spriteName);
    }

    @Override public void createSprite(String templateName, SpriteName spriteName) {
        spriteRegistry.createSprite(templateName, spriteName);
    }

    @Override public void destroySprite(SpriteName spriteName) {
        destroySprite(spriteName);
    }

    @Override public void registerCallback(EventType eventType, SpriteName spriteName, int moment, SpriteCallback spriteCallback) {
        throw new UnsupportedOperationException("FIXME unimplemented");
    }

    @Override public SpriteName spriteName(String name) {
        return new SpriteName(name);
    }
}
