package com.github.galdosd.betamax.sprite;

import com.github.galdosd.betamax.FrameClock;
import com.github.galdosd.betamax.Global;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FIXME: Document this class
 */
public class SpriteRegistry {
    private final FrameClock frameClock;
    private final Map<String,SpriteTemplate> registeredTemplates = new HashMap<>();

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
}
