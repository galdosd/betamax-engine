package com.github.galdosd.betamax.sound;

import com.codahale.metrics.Timer;
import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.OurTool;
import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkState;
import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;

/**
 * FIXME: Document this class
 */
public final class SoundWorld implements AutoCloseable {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());
    private final Timer soundLoadTimer = Global.metrics.timer("soundLoadTimer");

    private static final Object $LOCK = new Object();
    private static boolean initialized = false;

    private final long context, device;
    private final ALCapabilities alCapabilities;
    private final ALCCapabilities alcCapabilities;
    private final boolean disabled;

    public SoundSource newSource() {
        return new SoundSource();
    }

    public void globalPause() {
        if (!disabled) SoundSource.globalPause();
    }

    public void globalUnpause() {
        if (!disabled) SoundSource.globalUnpause();
    }

    public void globalPitch(float newPitch) {
        if (!disabled) SoundSource.globalPitch(newPitch);
    }
    public SoundBuffer loadSound(SoundName filename) {
        if (disabled) return null;
        try(Timer.Context ignored = soundLoadTimer.time()) {
            SoundBuffer soundBuffer = SoundBuffer.loadSoundFromFile(filename.getName());
            checkAlcError();
            return soundBuffer;
        }
    }

    public SoundWorld() {
        if (!Global.enableSound) {
            LOG.info("Sound disabled, skipping OpenAL initialization");
            disabled = true;
            device = 0;
            context = 0;
            alcCapabilities = null;
            alCapabilities = null;
            return;
        }
        disabled = false;
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
        LOG.info("Initialized OpenAL device {} context {} ({})", device, context, defaultDeviceName);
        checkAlcError();
    }


    private void checkAlcError() {
        if (disabled) return;
        int alcError = alcGetError(device);
        checkState(alcError==ALC_NO_ERROR, "OpenALC error " + alcError);
        checkAlError();
    }

    static void checkAlError(){
        int alError = alGetError();
        checkState(alError==AL_NO_ERROR, "OpenAL error " + alError);
    }

    @Override public void close() {
        if (disabled) return;
        checkAlcError();
        alcMakeContextCurrent(MemoryUtil.NULL);
        alcDestroyContext(context);
        alcCloseDevice(device);
        int alcError = alcGetError(device);
        checkState(alcError==ALC_NO_ERROR, "OpenALC error " + alcError);
        synchronized ($LOCK) {
            initialized = false;
        }
    }
}
