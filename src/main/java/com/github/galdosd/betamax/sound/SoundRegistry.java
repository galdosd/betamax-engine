package com.github.galdosd.betamax.sound;

import com.github.galdosd.betamax.OurTool;
import org.lwjgl.openal.*;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkState;
import static org.lwjgl.openal.AL10.*;
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
    private final ALCapabilities alCapabilities;
    private final ALCCapabilities alcCapabilities;

    public static void main(String[] args) {
        try(SoundRegistry soundRegistry = new SoundRegistry()) {
            
            try(Sound sound = soundRegistry.loadSound("test1.ogg")) {
                soundRegistry.playSound(sound);
                OurTool.sleepUntilPrecisely(System.currentTimeMillis()+9000);
            }
        }
    }

    public Sound loadSound(String filename) {
        // we wrap the package private Sound#loadSoundFromFile because it should not be called of openal is not initialized
        Sound sound = Sound.loadSoundFromFile(filename);
        checkAlError();
        return sound;
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

        alcCapabilities = ALC.createCapabilities(device);
        alCapabilities = AL.createCapabilities(alcCapabilities);
        checkAlError();
    }

    void checkAlError(){
        int alError = alGetError();
        checkState(alError==AL_NO_ERROR, "OpenAL error " + alError);
        int alcError = alcGetError(device);
        checkState(alcError==ALC_NO_ERROR, "OpenALC error " + alcError);
    }

    public void playSound(Sound sound){
        checkState(initialized);
        LOG.debug("Playing sound {}", sound);
        int sourceHandle = alGenSources();
        try {
            alSourcei(sourceHandle, AL_BUFFER, sound.getHandle());
            alSourcePlay(sourceHandle);
            checkAlError();
        } finally {
            //alDeleteSources(sourceHandle);
        }

    }

    @Override public void close() {
        alcDestroyContext(context);
        alcCloseDevice(device);
        synchronized ($LOCK) {
            initialized = false;
        }
    }
}
