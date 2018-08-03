package com.github.galdosd.betamax.scripting;

import com.github.galdosd.betamax.opengl.TextureCoordinate;
import com.github.galdosd.betamax.sprite.Sprite;
import com.github.galdosd.betamax.sprite.SpriteEvent;
import com.github.galdosd.betamax.sprite.SpriteName;
import com.github.galdosd.betamax.sprite.SpriteRegistry;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import org.slf4j.LoggerFactory;

import java.util.Collections;
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
    /** Only callback registering and logging and state vars is permitted until this is done */
    private boolean initializing = true;

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

    private void checkInit() {
        Preconditions.checkState(!initializing, "Only callback registration may be performed during initialization");
    }

    public Sprite getSpriteByName(SpriteName spriteName) {
        checkInit();
        return spriteRegistry.getSpriteByName(spriteName);
    }

    public boolean spriteExists(SpriteName spriteName) {
        checkInit();
        return spriteRegistry.spriteExists(spriteName);
    }
    public Sprite createSprite(String templateName, SpriteName spriteName) {
        checkInit();
        return spriteRegistry.createSprite(templateName, spriteName);
    }

    public void destroySprite(SpriteName spriteName) {
        checkInit();
        spriteRegistry.destroySprite(spriteName);
    }

    public void registerCallback(SpriteEvent spriteEvent, ScriptCallback scriptCallback) {
        // this should only be callable prior to onBegin, you should not be able to dynamically add callbacks
        // during the game, this would make saving/loading/rewinding/fast forwarding state intractable
        // callbacks should be set up during script initialization, initial sprites should be drawn during onBegin
        // (so if state is saved onBegin can just be skipped and the sprite stack can be restored)
        checkArgument(spriteRegistry.isAcceptingCallbacks(), "cannot alter callbacks after already begun");
        checkArgument(!callbacks.containsKey(spriteEvent), "Callback already registered for %s:\n     %s",
                spriteEvent, callbacks.get(spriteEvent));
        callbacks.put(spriteEvent, scriptCallback);
    }

    public void registerSpriteCallback(EventType eventType, SpriteName spriteName, int moment, ScriptCallback scriptCallback) {
        SpriteEvent spriteEvent = new SpriteEvent(eventType, spriteName, moment);
        registerCallback(spriteEvent, scriptCallback);
    }

    public void loadTemplate(String templateName) {
        checkInit();
        spriteRegistry.loadTemplate(templateName);
    }

    ScriptCallback getCallback(@NonNull SpriteEvent event) {
        return callbacks.get(event);
    }

    public SpriteName newSpriteName(String name) {
        return new SpriteName(name);
    }

    public TextureCoordinate newTextureCoord(double x, double y) {
        TextureCoordinate coord = new TextureCoordinate(x,y);
        checkArgument(coord.isValid(), "Out of bounds %s", coord);
        return coord;
    }

    public void registerGlobalCallback(@NonNull EventType eventType, ScriptCallback callback){
        checkArgument(!globalCallbacks.containsKey(eventType), "Callback already registered for %s", eventType);
        globalCallbacks.put(eventType, callback);
    }

    ScriptCallback getCallback(@NonNull EventType eventType) {
        return globalCallbacks.get(eventType);
    }

    public void finishInit() {
        initializing = false;
    }

    public Map<SpriteEvent, ScriptCallback> getAllCallbacks() {
        return callbacks;
    }

    public int getNamedMoment(String templateName, String momentName) {
        return spriteRegistry.getNamedMoment(templateName,momentName);
    }


    private final Map<String,String> stateVariables = new HashMap<>();

    public Map<String,String> getAllStateVariables() {
        return Collections.unmodifiableMap(stateVariables);
    }

    public void setStateVariable(String key, String val) {
        checkArgument(null!=key);
        checkArgument(null!=val);
        stateVariables.put(key,val);
    }

    public String getStateVariable(String key) {
        String val = stateVariables.get(key);
        checkArgument(null!=val, "No such state variable: %s", key);
        return val;
    }

    public String getGlobalShaderName() {
        return globalShaderName;
    }

    private String globalShaderName = null;
    public void setGlobalShader(String shaderName) {
        checkInit();
        globalShaderName = shaderName;
    }

    // FIXME 13am code
    public boolean rebootEverythingFlag = false;
    public void rebootEverything() {
        rebootEverythingFlag = true;
        LOG.info("Rebooting everything (scheduled)");
    }
}
