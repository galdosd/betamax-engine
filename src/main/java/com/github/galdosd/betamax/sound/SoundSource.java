package com.github.galdosd.betamax.sound;

import lombok.ToString;
import org.lwjgl.openal.AL10;
import org.slf4j.LoggerFactory;

import static org.lwjgl.openal.AL10.*;

/**
 * FIXME: Document this class
 */
@ToString public final class SoundSource implements AutoCloseable {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());
    private final int handle;

    SoundSource() {
        LOG.debug("New {}", this);
        handle = alGenSources();
        SoundWorld.checkAlError();
    }

    public void playSound(SoundBuffer soundBuffer){
        LOG.debug("Playing {} on {}", soundBuffer, this);
        alSourcei(handle, AL10.AL_BUFFER, soundBuffer.getHandle());
        alSourcePlay(handle);
        SoundWorld.checkAlError();
    }


    @Override public void close() {
        alDeleteSources(handle);
        SoundWorld.checkAlError();
    }

    public void resume() {
        throw new UnsupportedOperationException("DOM DINT IMPLENT DIS YAT");
    }

    public void pause() {
        throw new UnsupportedOperationException("DOM DINT IMPLENT DIS YAT");
    }

    public void mute() {
        throw new UnsupportedOperationException("DOM DINT IMPLENT DIS YAT");
    }

    public void unmute() {
        throw new UnsupportedOperationException("DOM DINT IMPLENT DIS YAT");
    }
}
