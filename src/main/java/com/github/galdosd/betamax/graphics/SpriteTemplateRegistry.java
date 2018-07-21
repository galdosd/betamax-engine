package com.github.galdosd.betamax.graphics;

import com.github.galdosd.betamax.sound.SoundRegistry;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * FIXME: Document this class
 */
public final class SpriteTemplateRegistry implements AutoCloseable {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private final SoundRegistry soundRegistry;
    private final Map<String,SpriteTemplate> registeredTemplates = new HashMap<>();
    private final Map<String,SpriteTemplateManifest> registeredManifests = new HashMap<>();

    public SpriteTemplateRegistry(SoundRegistry soundRegistry) {
        this.soundRegistry = soundRegistry;
    }

    // TODO it might be nice to have some mechanism by which templates not used for a while are unloaded
    // that said it probably makes sense to have that partially manually controlled (to group templates into
    // dayparts for example) rather than entirely automagical, especially since the performance implications are
    // quite serious if the magic gets it wrong
    // conversely, automatic background loading of things that will be needed in the future might be worthwhile
    public SpriteTemplate getTemplate(String name) {
        SpriteTemplate template = registeredTemplates.get(name);
        if(null==template) {
            template = new SpriteTemplate(getManifest(name));
            // TODO this does not allow for lazy sound loading because if it is not loaded into the SpriteTemplate by
            // the time the Sprite is created the Sprite/SpriteTemplate have no soundRegistry access to load the
            // soundbuffer
            template.loadSoundBuffer(soundRegistry);
            registeredTemplates.put(name,template);
        }
        return template;
    }

    private SpriteTemplateManifest getManifest(String name) {
        SpriteTemplateManifest manifest = registeredManifests.get(name);
        if(null==manifest) {
            manifest = SpriteTemplateManifest.load(name);
            registeredManifests.put(name, manifest);
        }
        return manifest;
    }

    @Override public void close() {
        registeredTemplates.values().forEach(SpriteTemplate::close);
        soundRegistry.close();
    }

    public int getNamedMoment(String templateName, String momentName) {
        return getManifest(templateName).getMomentByName(momentName);
    }
}
