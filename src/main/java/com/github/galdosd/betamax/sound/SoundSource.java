package com.github.galdosd.betamax.sound;

import lombok.ToString;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;
import static org.lwjgl.openal.AL10.*;

/**
 * FIXME: Document this class
 */
@ToString public final class SoundSource implements AutoCloseable {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());
    private final int handle;
    /** we use this like a semaphore so it can be reentrantly paused so global and per-sprite pause both work easily */
    private int pauseLevel = 0;
    private SoundBuffer buffer;

    private static final Set<SoundSource> allSoundSources = new HashSet<>();
    private static final Object LOCK$allSoundSources = new Object();
    private static float currentGlobalPitch = 1.0f;

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
        buffer = soundBuffer;
        LOG.debug("Playing {}", this);
        alSourcei(handle, AL10.AL_BUFFER, soundBuffer.getHandle());
        SoundWorld.checkAlError();
        alSourcePlay(handle);
        SoundWorld.checkAlError();
        setPitch(currentGlobalPitch);
        SoundWorld.checkAlError();
    }

    public String getRemarks() {
        float secOffset = getSecOffset();
        return String.format("%s %.1fs of %.1fs", getSourceStateString(), secOffset, buffer.totalSeconds());
    }

    private float getSecOffset() {
        if(getSourceState() == AL_STOPPED) {
            return buffer.totalSeconds();
        }
        float offset = alGetSourcef(handle, AL11.AL_SEC_OFFSET);
        SoundWorld.checkAlError();
        return offset;
    }

    public void seek(float seconds) {
        int samples = buffer.secondsToSamples(seconds);
        boolean requestingEnd = samples >= buffer.totalSamples() - 1;
        if(requestingEnd) {
            LOG.debug("Seeked to end and stopped {}", this);
            alSourceStop(handle);
            SoundWorld.checkAlError();
        } else {
            if(AL_STOPPED==getSourceState()) {
                alSourcePlay(handle);
                if(pauseLevel > 0) {
                    alSourcePause(handle);
                    LOG.debug("Seeked to {} and paused after having finished {}", seconds, this);
                } else {
                    LOG.debug("Seeked to {} after having finished {}", seconds, this);
                }
            } else {
                LOG.debug("Seeked playing source to {}: {}", seconds, this);
            }
            alSourcef(handle, AL11.AL_SAMPLE_OFFSET, samples);
            SoundWorld.checkAlError();
        }
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
        checkState(pauseLevel>=0);
        if(pauseLevel == 0) {
            if(AL_PAUSED == getSourceState()) {
                LOG.debug("Resume {}", this);
                alSourcePlay(handle);
                SoundWorld.checkAlError();
            } else {
                LOG.debug("Resume {} (suppressed, already complete)", this);

            }
        } else {
            LOG.debug("Pause {} level {}", this, pauseLevel);
        }
    }

    private int getSourceState() {
        int sourceState = alGetSourcei(handle, AL_SOURCE_STATE);
        SoundWorld.checkAlError();
        return sourceState;
    }

    private String getSourceStateString() {
        int sourceState = getSourceState();
        switch(sourceState) {
            case AL_INITIAL:    return "INITIAL";
            case AL_PLAYING:    return "PLAYING";
            case AL_PAUSED:     return "PAUSED";
            case AL_STOPPED:    return "STOPPED";
            default:            return "UNKNOWN=" + sourceState;
        }
    }

    public void pause() {
        checkState(pauseLevel>=0);
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
        SoundWorld.checkAlError();
    }

    public void unmute() {
        LOG.debug("Unmute {}", this);
        alSourcef(handle, AL_GAIN, 1f);
        SoundWorld.checkAlError();
    }

    public static void globalPitch(float newPitch) {
        allSoundSources.stream().forEach(s -> s.setPitch(newPitch));
        currentGlobalPitch = newPitch;
    }

    private void setPitch(float newPitch) {
        alSourcef(handle, AL_PITCH, newPitch);
        SoundWorld.checkAlError();
    }

    public int getPauseLevel() {
        checkState(pauseLevel>=0);
        return pauseLevel;
    }

    /** Given expected seeked position, return how far off we actually are (positive means actual playing sound
     *  is ahead of expectation, negative means it's behind.
     *
     *  We notionally think of a sound playing once and then continuing on with dead silence infinitely long,
     *  so naturally if the sound is already successfully over (AL_STOPPED) and the expected position is past
     *  the end of the soundtrack, then our drift is of course 0.0. Of course, if the sound is stopped (over) and
     *  the expected position is earlier than the end, then we do have a little drift.
     */
    public float getDrift(float expectedPositionInSeconds) {
        if(getSourceState()==AL_STOPPED && expectedPositionInSeconds > buffer.totalSeconds()) {
            return 0.0f;
        } else {
            return getSecOffset() - expectedPositionInSeconds;
        }
    }
}
