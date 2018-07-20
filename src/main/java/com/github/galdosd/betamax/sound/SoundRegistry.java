package com.github.galdosd.betamax.sound;

import org.lwjgl.openal.*;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkState;
import static org.lwjgl.openal.ALC10.*;

/**
 * FIXME: Document this class
 */
public class SoundRegistry implements AutoCloseable {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private static final Object $LOCK = new Object();
    private static boolean initialized = false;

    private final long context, device;

    public static void main(String[] args) {
        try(SoundRegistry soundRegistry = new SoundRegistry()) {
            try(Sound sound = soundRegistry.loadSound("test1.ogg")) {
                soundRegistry.playSound(sound);
            }
        }
    }

    public Sound loadSound(String filename) {
        // we wrap the package private Sound#loadSoundFromFile because it should not be called of openal is not initialized
        return Sound.loadSoundFromFile(filename);
    }

    public SoundRegistry() {
        synchronized ($LOCK) {
            checkState(!initialized, "OpenAL was already initialized previously");
            initialized = true;
        }
        String defaultDeviceName = ALC10.alcGetString(0, ALC10.ALC_DEFAULT_DEVICE_SPECIFIER);
        device = alcOpenDevice(defaultDeviceName);
        int[] attributes = {0};
        context = alcCreateContext(device, attributes);
        alcMakeContextCurrent(context);

        ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
        ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);

    }

    public void playSound(Sound sound){
        checkState(initialized);

    }

    @Override public void close() {
        alcDestroyContext(context);
        alcCloseDevice(device);
        synchronized ($LOCK) {
            initialized = false;
        }
    }
}
