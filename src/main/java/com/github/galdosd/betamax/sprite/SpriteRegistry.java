package com.github.galdosd.betamax.sprite;

import com.github.galdosd.betamax.FrameClock;
import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.scripting.EventType;
import com.github.galdosd.betamax.scripting.LogicHandler;
import com.github.galdosd.betamax.scripting.ScriptWorld;
import com.github.galdosd.betamax.scripting.SpriteEvent;
import com.google.common.base.Preconditions;
import lombok.Getter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * FIXME: Document this class
 */
public class SpriteRegistry {
    private final FrameClock frameClock;
    private final Map<String,SpriteTemplate> registeredTemplates = new HashMap<>();

    // TODO sprite destruction will be O(number of sprites on screen)
    // this will probably never significantly impact performance tho
    // could just store the index anyway
    private final Map<SpriteName,Sprite> registeredSprites = new HashMap<>();
    private final List<Sprite> orderedSprites = new LinkedList<>();

    public SpriteRegistry(FrameClock frameClock) {
        this.frameClock = frameClock;
    }

    // TODO it might be nice to have some mechanism by which templates not used for a while are unloaded
    // that said it probably makes sense to have that partially manually controlled (to group templates into
    // dayparts for example) rather than entirely automagical, especially since the performance implications are
    // quite serious if the magic gets it wrong
    // conversely, automatic background loading of things that will be needed in the future might be worthwhile
    public SpriteTemplate getTemplate(String name) {
        SpriteTemplate template = registeredTemplates.get(name);
        if(null==template) {
            template = new SpriteTemplate(Global.spriteBase+name, frameClock);
            registeredTemplates.put(name,template);
        }
        return template;
    }

    public void createSprite(String templateName, SpriteName spriteName) {
        addSprite(getTemplate(templateName).create(spriteName));
    }

    private void addSprite(Sprite sprite) {
        SpriteName name = sprite.getName();
        checkArgument(!registeredSprites.containsKey(name), "duplicate sprite name: " + name);
        registeredSprites.put(name, sprite);
        orderedSprites.add(sprite);
    }

    public Sprite getSpriteByName(SpriteName spriteName) {
        Sprite sprite = registeredSprites.get(spriteName);
        checkArgument(null!=sprite, "No such sprite: " + spriteName);
        return sprite;
    }

    public void removeSprite(SpriteName spriteName) {
        Sprite sprite = getSpriteByName(spriteName);
        registeredSprites.remove(spriteName);
        boolean wasRemovedFromList = orderedSprites.remove(sprite);
        checkState(wasRemovedFromList);

    }

    public void renderAll() {
        for(Sprite sprite: orderedSprites) {
            sprite.render();
        }
    }

    public void dispatchEvents(LogicHandler logicHandler) {
        dispatchBeginEvent(logicHandler);
        // TODO handle SPRITE_CREATE, SPRITE_DESTROY
        dispatchSpriteMomentEvents(logicHandler);
    }

    @Getter boolean alreadyBegun = false;
    private void dispatchBeginEvent(LogicHandler logicHandler) {
        if(!alreadyBegun) {
            alreadyBegun = true;
            logicHandler.onBegin();
        }
    }

    private void dispatchSpriteMomentEvents(LogicHandler logicHandler) {
        for(Sprite sprite: orderedSprites) {
            // TODO: sprites created by handler callbacks will not have their first moment #0 event processed
            // the same issue will apply to SPRITE_CREATE/DESTROY. we can surround this in a BFS like loop to ensure it
            SpriteEvent momentEvent = new SpriteEvent(
                    EventType.SPRITE_MOMENT,
                    sprite.getName(),
                    sprite.getRenderedFrame()
            );
            logicHandler.onSpriteEvent(momentEvent);
        }
    }
}
