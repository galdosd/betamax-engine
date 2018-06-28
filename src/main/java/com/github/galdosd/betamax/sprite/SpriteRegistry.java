package com.github.galdosd.betamax.sprite;

import com.github.galdosd.betamax.FrameClock;
import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.scripting.LogicHandler;
import com.github.galdosd.betamax.scripting.ScriptWorld;
import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * FIXME: Document this class
 */
public class SpriteRegistry {
    private final FrameClock frameClock;
    private final Map<String,SpriteTemplate> registeredTemplates = new HashMap<>();

    // TODO sprite destruction will be O(number of sprites on screen)
    // this will probably never significantly impact performance tho
    private final Map<String,Sprite> registeredSprites = new HashMap<>();
    private final List<Sprite> orderedSprites = new LinkedList<>();

    public SpriteRegistry(FrameClock frameClock) {
        this.frameClock = frameClock;
    }

    public SpriteTemplate getTemplate(String name) {
        SpriteTemplate template = registeredTemplates.get(name);
        if(null==template) {
            template = new SpriteTemplate(Global.spriteBase+name, frameClock);
            registeredTemplates.put(name,template);
        }
        return template;
    }

    public void createSprite(String templateName, String spriteName) {
        addSprite(spriteName, getTemplate(templateName).create());
    }

    private void addSprite(String name, Sprite sprite) {
        checkArgument(!registeredSprites.containsKey(name), "duplicate sprite name: " + name);
        registeredSprites.put(name, sprite);
        orderedSprites.add(sprite);
    }

    public void renderAll() {
        for(Sprite sprite: orderedSprites) {
            sprite.render();
        }
    }

    boolean alreadyBegun = false;
    public void dispatchEvents(LogicHandler logicHandler) {
        if(!alreadyBegun) {
            alreadyBegun = true;
            logicHandler.onBegin();
        }

    }
}
