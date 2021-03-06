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

//    public static void main(String[] args) {
//        try(SoundWorld soundRegistry = new SoundWorld()) {
//            try (SoundBuffer soundBuffer = soundRegistry.loadSound(new SoundName("com/github/galdosd/betamax/sprites/demowalk/test2.ogg"))) {
//                try(SoundSource source = soundRegistry.newSource()) {
//                    source.playSound(soundBuffer);
//                    OurTool.sleepUntilPrecisely(System.currentTimeMillis() + 9000);
//                }
//            }
//        }
//    }

    public SoundSource newSource() {
        return new SoundSource();
    }

    public void globalPause() {
        SoundSource.globalPause();
    }

    public void globalUnpause() {
        SoundSource.globalUnpause();
    }

    public void globalPitch(float newPitch) {
        SoundSource.globalPitch(newPitch);
    }
    public SoundBuffer loadSound(SoundName filename) {
        // we wrap the package private SoundBuffer#loadSoundFromFile because it should not be called of openal is not initialized
        try(Timer.Context ignored = soundLoadTimer.time()) {
            SoundBuffer soundBuffer = SoundBuffer.loadSoundFromFile(filename.getName());
            checkAlcError();
            return soundBuffer;
        }
    }

    public SoundWorld() {
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
        int alcError = alcGetError(device);
        checkState(alcError==ALC_NO_ERROR, "OpenALC error " + alcError);
        checkAlError();
    }

    static void checkAlError(){
        int alError = alGetError();
        checkState(alError==AL_NO_ERROR, "OpenAL error " + alError);
    }

    @Override public void close() {
        checkAlcError();
        // after this alGetError can no longer be called, or it will generate a "spurious" error"
        // https://github.com/LWJGL/lwjgl3/issues/219
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
