package com.github.galdosd.betamax.sound;

import lombok.ToString;
import org.lwjgl.openal.AL10;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.openal.AL10.*;

/**
 * FIXME: Document this class
 */
@ToString public final class SoundSource implements AutoCloseable {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());
    private final int handle;
    private boolean playing = false;
    /** we use this like a semaphore so it can be reentrantly paused so global and per-sprite pause both work easily */
    private int pauseLevel = 0;

    private static final Set<SoundSource> allSoundSources = new HashSet<>();
    private static final Object LOCK$allSoundSources = new Object();

    static void globalUnpause() {
        allSoundSources.stream().forEach(SoundSource::resume);
    }

    static void globalPause() {
        allSoundSources.stream().forEach(SoundSource::pause);
    }

    SoundSource() {
        handle = alGenSources();
        LOG.debug("New {}", this);
        SoundWorld.checkAlError();
        synchronized (LOCK$allSoundSources) {
            allSoundSources.add(this);
        }
    }

    public void playSound(SoundBuffer soundBuffer){
        LOG.debug("Playing {} on {}", soundBuffer, this);
        alSourcei(handle, AL10.AL_BUFFER, soundBuffer.getHandle());
        SoundWorld.checkAlError();
        alSourcePlay(handle);
        SoundWorld.checkAlError();
    }

    @Override public void close() {
        LOG.debug("Closing {}", this);
        SoundWorld.checkAlError();
        alDeleteSources(handle);
        SoundWorld.checkAlError();
        synchronized (LOCK$allSoundSources) {
            allSoundSources.remove(this);
        }
    }

    public void resume() {
        pauseLevel--;
        if(pauseLevel == 0) {
            LOG.debug("Resume {}", this);
            alSourcePlay(handle);
            SoundWorld.checkAlError();
        } else {
            LOG.debug("Pause {} level {}", this, pauseLevel);
        }
    }

    public void pause() {
        if(pauseLevel == 0) {
            LOG.debug("Pause {}", this);
            alSourcePause(handle);
        } else {
            LOG.debug("Pause {} level {}", this, pauseLevel);
        }
        pauseLevel++;
    }

    public void mute() {
        LOG.debug("Mute {}", this);
        alSourcef(handle, AL_GAIN, 0f);
    }

    public void unmute() {
        LOG.debug("Unmute {}", this);
        alSourcef(handle, AL_GAIN, 1f);
    }

    public static void globalPitch(float newPitch) {
        allSoundSources.stream().forEach(s -> s.setPitch(newPitch));

    }

    private void setPitch(float newPitch) {
        alSourcef(handle, AL_PITCH, newPitch);
    }
}
