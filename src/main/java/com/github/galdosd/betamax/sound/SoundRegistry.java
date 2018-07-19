package com.github.galdosd.betamax.sound;

import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.LoggerFactory;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static com.google.common.base.Preconditions.checkState;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_decode_filename;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_decode_memory;

/**
 * FIXME: Document this class
 */
public class SoundRegistry implements AutoCloseable {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private static final Object $LOCK = new Object();
    private static boolean initialized = false;

    public static void main(String[] args) {
        try(SoundRegistry soundRegistry = new SoundRegistry()) {
            Sound sound = soundRegistry.loadSound("test1.ogg");
            soundRegistry.playSound(sound);
            LOG.info("it's over!");
        }
    }

    private final long context, device;
    public SoundRegistry() {
        synchronized ($LOCK) {
            checkState(!initialized);
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


    private final IntBuffer channelsBuffer = MemoryUtil.memAllocInt(1);
    private final IntBuffer sampleRateBuffer = MemoryUtil.memAllocInt(1);

    public Sound loadSound(String fileName) {
        ShortBuffer rawAudioBuffer = stb_vorbis_decode_filename(fileName, channelsBuffer, sampleRateBuffer);
        // ShortBuffer rawAudioBuffer = stb_vorbis_decode_memory(soundFileBuffer, channelsBuffer, sampleRateBuffer);
        int channels = channelsBuffer.get();
        int sampleRate = sampleRateBuffer.get();
        return new Sound(channels,sampleRate,rawAudioBuffer);
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
