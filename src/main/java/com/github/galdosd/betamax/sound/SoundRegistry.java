package com.github.galdosd.betamax.sound;

import com.github.galdosd.betamax.graphics.SpriteTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * FIXME: Document this class
 */
public class SoundRegistry implements  AutoCloseable {
    private final SoundWorld soundWorld;
    private final Map<SoundName,SoundBuffer> registeredBuffers = new HashMap<>();

    public SoundRegistry() {
        soundWorld = new SoundWorld();
    }

    @Override public void close() {
        registeredBuffers.values().forEach(SoundBuffer::close);
        soundWorld.close();
    }

    public SoundBuffer getSoundBuffer(SoundName soundName) {
        SoundBuffer cached = registeredBuffers.get(soundName);
        if(null==cached) {
            registeredBuffers.put(soundName,soundWorld.loadSound(soundName));
        }
        return registeredBuffers.get(soundName);
    }
}
