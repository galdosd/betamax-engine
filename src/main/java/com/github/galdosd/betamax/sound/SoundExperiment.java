package com.github.galdosd.betamax.sound;

import com.github.galdosd.betamax.OurTool;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libc.LibCStdlib;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static com.google.common.base.Preconditions.checkState;
import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_decode_filename;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_decode_memory;
import static org.lwjgl.system.libc.LibCStdlib.free;

/**
 * FIXME: Document this class
 */
public final class SoundExperiment implements AutoCloseable {
    private static final IntBuffer channelsBuffer = MemoryUtil.memAllocInt(1);
    private static final IntBuffer sampleRateBuffer = MemoryUtil.memAllocInt(1);
    private final long device;
    private final long context;

    public static void main(String[] args) {
        try(SoundExperiment soundExperiment = SoundExperiment.init()) {
            try(SoundBuffer soundBufferHandle = loadSoundBuffer("test1.ogg")) {
                soundExperiment.playSound(soundBufferHandle);
            }
        }
    }

    private SoundExperiment(long device, long context) {
        this.device=device;
        this.context=context;
        checkAlError();
    }

    @Override public void close() {
        alcDestroyContext(context);
        alcCloseDevice(device);
    }

    private static int channelsToFormat(int channels) {
        if(channels == 1) return AL10.AL_FORMAT_MONO16;
        if(channels == 2) return AL10.AL_FORMAT_STEREO16;
        return -1;
    }


    private void playSound(SoundBuffer soundBufferHandle) {
        int sourcePointer = alGenSources();

        alSourcei(sourcePointer, AL_BUFFER, soundBufferHandle.handle);

        alSourcePlay(sourcePointer);
        try {
            //Wait for a second
            Thread.sleep(9000);
        } catch (InterruptedException ignored) {
        }

        alDeleteSources(sourcePointer);
    }

    private void checkAlError(){
        int alError = alGetError();
        checkState(alError==AL_NO_ERROR, "OpenAL error " + alError);
        int alcError = alcGetError(device);
        checkState(alcError==ALC_NO_ERROR, "OpenALC error " + alcError);
    }

    public static SoundExperiment init() {
        String defaultDeviceName = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
        long device = alcOpenDevice(defaultDeviceName);
        int[] attributes = {0};
        long context = alcCreateContext(device, attributes);
        alcMakeContextCurrent(context);

        ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
        ALCapabilities alCapabilities  = AL.createCapabilities(alcCapabilities);
        return new SoundExperiment(device,context);
    }

    @ToString @FieldDefaults(level= AccessLevel.PRIVATE, makeFinal = true)
    public static class SoundBuffer implements AutoCloseable{
        @Getter int handle;
        @Getter int channels;
        @Getter int sampleRate;
        @Getter int bytes;
        @Getter String filename;

        private SoundBuffer(int handle, int channels, int sampleRate, int bytes, String filename) {
            this.handle = handle;
            this.channels = channels;
            this.sampleRate = sampleRate;
            this.bytes = bytes;
            this.filename = filename;
        }

        @Override public void close() {
            alDeleteBuffers(handle);
        }
    }

    public static SoundBuffer loadSoundBuffer(String filename) {
        ShortBuffer rawAudioBuffer = null;
        ByteBuffer soundFileBuffer = null;
        Integer bufferHandle = null;
        try {
            soundFileBuffer = OurTool.readOffHeapBuffer(filename);
            rawAudioBuffer = stb_vorbis_decode_memory(soundFileBuffer, channelsBuffer, sampleRateBuffer);
            // TODO seems like it would be some legwork to construct a stb decoder just to get the damn error code
            checkState(null != rawAudioBuffer, "could not load " + filename);
            int channels = channelsBuffer.get(0);
            int sampleRate = sampleRateBuffer.get(0);
            bufferHandle = alGenBuffers();
            alBufferData(bufferHandle, channelsToFormat(channels), rawAudioBuffer, sampleRate);
            return new SoundBuffer(bufferHandle, channels, sampleRate, rawAudioBuffer.limit() * Short.BYTES, filename);
        } catch(Exception e) {
            if(null!=bufferHandle) alDeleteBuffers(bufferHandle);
            throw new RuntimeException(e);
        } finally {
            if(null!=soundFileBuffer) {
                MemoryUtil.memFree(soundFileBuffer);
            }
            if(null!=rawAudioBuffer) {
                LibCStdlib.free(rawAudioBuffer);
            }
        }
    }

}
