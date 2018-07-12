package com.github.galdosd.betamax.sprite;

import com.github.galdosd.betamax.Global;

import java.util.HashMap;
import java.util.Map;

/**
 * FIXME: Document this class
 */
public final class SpriteTemplateRegistry {
    private final Map<String,SpriteTemplate> registeredTemplates = new HashMap<>();
    // TODO it might be nice to have some mechanism by which templates not used for a while are unloaded
    // that said it probably makes sense to have that partially manually controlled (to group templates into
    // dayparts for example) rather than entirely automagical, especially since the performance implications are
    // quite serious if the magic gets it wrong
    // conversely, automatic background loading of things that will be needed in the future might be worthwhile
    public SpriteTemplate getTemplate(String name) {
        SpriteTemplate template = registeredTemplates.get(name);
        if(null==template) {
            template = new SpriteTemplate(Global.spriteBase+name);
            registeredTemplates.put(name,template);
        }
        return template;
    }
}
