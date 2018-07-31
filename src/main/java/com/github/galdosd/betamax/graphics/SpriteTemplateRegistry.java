package com.github.galdosd.betamax.graphics;

import com.codahale.metrics.Counter;
import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.imageio.SpriteTemplateManifest;
import com.github.galdosd.betamax.sound.SoundRegistry;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * FIXME: Document this class
 */
public final class SpriteTemplateRegistry implements AutoCloseable {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private final Counter loadedSpriteTemplatesCounter = Global.metrics.counter("loadedSpriteTemplates");
    private final SoundRegistry soundRegistry;
    private final Map<String,SpriteTemplate> registeredTemplates = new HashMap<>();
    private final Map<String,SpriteTemplateManifest> registeredManifests = new HashMap<>();
    private final TextureRegistry textureRegistry;

    public SpriteTemplateRegistry(SoundRegistry soundRegistry, TextureRegistry textureRegistry) {
        this.soundRegistry = soundRegistry;
        this.textureRegistry = textureRegistry;
    }

    /** This preloads manifests and sound, actual images of course are not actually loaded.
     * This should take a negligible amount of time if there are no sounds, a couple seconds maybe (wild guess)
     * (It's definitely worth it to avoid having to do any IO when sprites are created)
     * If there are sounds, add 10-15 seconds per hour of OGG sound roughly.
     * It'd be nice to load OGG sound dynamically so we can avoid adding ten seconds to boot time,
     * but it's not the end of the world if there is and there was no time in the shipping schedule for that
     */
    public void preloadEverything() {
        checkArgument(registeredManifests.size()==0);
        checkArgument(registeredTemplates.size()==0);
        LOG.info("Preloading all sprite template manifests and sounds");
        registeredManifests.putAll( SpriteTemplateManifest.preloadEverything() );
        for(SpriteTemplateManifest manifest: registeredManifests.values()) {
            SpriteTemplate template = new SpriteTemplate(manifest, textureRegistry);
            registeredTemplates.put(manifest.getTemplateName(), template);
            loadedSpriteTemplatesCounter.inc();
            template.loadSoundBuffer(soundRegistry);
        }
        LOG.info("Completed all sprite template preloading");
    }

    // TODO it might be nice to have some mechanism by which templates not used for a while are unloaded
    // that said it probably makes sense to have that partially manually controlled (to group templates into
    // dayparts for example) rather than entirely automagical, especially since the performance implications are
    // quite serious if the magic gets it wrong
    // conversely, automatic background loading of things that will be needed in the future might be worthwhile
    public SpriteTemplate getTemplate(String name) {
        SpriteTemplate template = registeredTemplates.get(name);
        if(null==template) {
            template = new SpriteTemplate(getManifest(name), textureRegistry);
            // TODO this does not allow for lazy sound loading because if it is not loaded into the SpriteTemplate by
            // the time the Sprite is created the Sprite/SpriteTemplate have no soundRegistry access to load the
            // soundbuffer
            template.loadSoundBuffer(soundRegistry);
            registeredTemplates.put(name,template);
            loadedSpriteTemplatesCounter.inc();
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
        loadedSpriteTemplatesCounter.dec(registeredTemplates.values().size());
        registeredTemplates.values().forEach(SpriteTemplate::close);
        soundRegistry.close();
    }

    public int getNamedMoment(String templateName, String momentName) {
        return getManifest(templateName).getMomentByName(momentName);
    }
}
