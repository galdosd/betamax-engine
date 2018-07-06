package com.github.galdosd.betamax.scripting;

import com.github.galdosd.betamax.sprite.Sprite;
import com.github.galdosd.betamax.sprite.SpriteEvent;
import com.github.galdosd.betamax.sprite.SpriteName;
import com.github.galdosd.betamax.sprite.SpriteRegistry;
import lombok.NonNull;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/** Provide all needed services to the python scripts. Used by betamax.py This should be a thin wrapper.
 */
public final class ScriptServicer {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private final SpriteRegistry spriteRegistry;
    private final Map<SpriteEvent,ScriptCallback> callbacks = new HashMap<>();
    private final Map<EventType,ScriptCallback> globalCallbacks = new HashMap<>();

    public ScriptServicer(SpriteRegistry spriteRegistry) {
        this.spriteRegistry = spriteRegistry;
    }

    public void log(String msg) {
        LOG.debug("[jython] {}", msg);
    }

    public void fatal(String msg) {
        throw new RuntimeException("Fatal jython script error: " + msg);
    }

    public void exit() {
        LOG.info("Jython script requested normal exit");
        System.exit(0);
    }

    public Map<String, Object> getState() {
        throw new UnsupportedOperationException("FIXME unimplemented");
    }

    public Sprite getSpriteByName(SpriteName spriteName) {
        return spriteRegistry.getSpriteByName(spriteName);
    }

    public void createSprite(String templateName, SpriteName spriteName) {
        spriteRegistry.createSprite(templateName, spriteName);
    }

    public void destroySprite(SpriteName spriteName) {
        spriteRegistry.destroySprite(spriteName);
    }

    public void registerCallback(SpriteEvent spriteEvent, ScriptCallback scriptCallback) {
        // this should only be callable prior to onBegin, you should not be able to dynamically add callbacks
        // during the game, this would make saving/loading/rewinding/fast forwarding state intractable
        // callbacks should be set up during script initialization, initial sprites should be drawn during onBegin
        // (so if state is saved onBegin can just be skipped and the sprite stack can be restored)
        checkArgument(!spriteRegistry.isAlreadyBegun(), "cannot alter callbacks after already begun");
        checkArgument(!callbacks.containsKey(spriteEvent), "Callback already registered for %s", spriteEvent);
        callbacks.put(spriteEvent, scriptCallback);
    }

    public void registerSpriteCallback(EventType eventType, SpriteName spriteName, int moment, ScriptCallback scriptCallback) {
        SpriteEvent spriteEvent = new SpriteEvent(eventType, spriteName, moment);
        registerCallback(spriteEvent, scriptCallback);
    }

    void loadTemplate(String templateName) {
        spriteRegistry.getTemplate(templateName);
    }

    ScriptCallback getCallback(@NonNull SpriteEvent event) {
        return callbacks.get(event);
    }

    public SpriteName newSpriteName(String name) {
        return new SpriteName(name);
    }

    public void registerGlobalCallback(@NonNull EventType eventType, ScriptCallback callback){
        checkArgument(!globalCallbacks.containsKey(eventType), "Callback already registered for %s", eventType);
        globalCallbacks.put(eventType, callback);
    }

    public ScriptCallback getCallback(@NonNull EventType eventType) {
        return globalCallbacks.get(eventType);
    }
}
