package com.github.galdosd.betamax.scripting;

import com.github.galdosd.betamax.sprite.Sprite;
import com.github.galdosd.betamax.sprite.SpriteName;
import com.github.galdosd.betamax.sprite.SpriteRegistry;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

public class ScriptServicerImpl implements ScriptServicer {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private final SpriteRegistry spriteRegistry;
    public ScriptServicerImpl(SpriteRegistry spriteRegistry) {
        this.spriteRegistry = spriteRegistry;
    }

    @ToString @EqualsAndHashCode private static final class Situation {
        public final EventType eventType;
        public final SpriteName spriteName;
        /** ignored and must be 0 except for eventType==SPRITE_MOMENT */
        public final int moment;

        private Situation(EventType eventType, SpriteName spriteName, int moment) {
            checkArgument(eventType==EventType.SPRITE_MOMENT || moment == 0, "moment may only be set for EventType.SPRITE_MOMENT");
            checkArgument(spriteName!=null || eventType == EventType.BEGIN, "sprite must be set for sprite events");
            checkArgument(spriteName==null || eventType != EventType.BEGIN, "sprite may not be set for BEGIN event");

            this.eventType = eventType;
            this.spriteName = spriteName;
            this.moment = moment;
        }
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
        Situation situation = new Situation(eventType, spriteName, moment);
        checkArgument(!callbacks.containsKey(situation), "Callback already registered for %s", situation);
        callbacks.put(situation, spriteCallback);
    }

    @Override public SpriteName spriteName(String name) {
        return new SpriteName(name);
    }
}
