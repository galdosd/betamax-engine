package com.github.galdosd.betamax.sound;

import com.github.galdosd.betamax.OurTool;
import lombok.Getter;
import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryUtil;

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
public class SoundExperiment {
    private static final IntBuffer channelsBuffer = MemoryUtil.memAllocInt(1);
    private static final IntBuffer sampleRateBuffer = MemoryUtil.memAllocInt(1);
    private static int channelsToFormat(int channels) {
        if(channels == 1) return AL10.AL_FORMAT_MONO16;
        if(channels == 2) return AL10.AL_FORMAT_STEREO16;
        return -1;
    }
    long   device;
    long  context;
    public static void main(String[] args) {
        new SoundExperiment().doStuff();
    }

    private void doStuff() {
        initOpenAl();

        String filename = "test1.ogg";
        SoundBuffer soundBufferHandle = loadSoundBuffer(filename);
        int sourcePointer = alGenSources();

        alSourcei(sourcePointer, AL_BUFFER, soundBufferHandle.handle);

        alSourcePlay(sourcePointer);
        try {
            //Wait for a second
            Thread.sleep(9000);
        } catch (InterruptedException ignored) {
        }

        alDeleteSources(sourcePointer);

        alcDestroyContext(context);
        alcCloseDevice(device);
    }

    void checkAlError(){
        int alError = alGetError();
        checkState(alError==AL_NO_ERROR, "OpenAL error " + alError);
        int alcError = alcGetError(device);
        checkState(alcError==ALC_NO_ERROR, "OpenALC error " + alcError);
    }

    private void initOpenAl() {
        String defaultDeviceName = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
        device = alcOpenDevice(defaultDeviceName);
        int[] attributes = {0};
        context = alcCreateContext(device, attributes);
        alcMakeContextCurrent(context);

        ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
        ALCapabilities alCapabilities  = AL.createCapabilities(alcCapabilities);
        checkAlError();
    }

    public static class SoundBuffer implements AutoCloseable{
        @Getter int handle;
        SoundBuffer(int handle) {
            this.handle = handle;
        }

        @Override
        public void close() throws Exception {
            alDeleteBuffers(handle);
        }
    }

    private static SoundBuffer loadSoundBuffer(String filename) {
        ShortBuffer rawAudioBuffer;


        ByteBuffer soundFileBuffer = OurTool.readOffHeapBuffer(filename);
        rawAudioBuffer = stb_vorbis_decode_memory(soundFileBuffer, channelsBuffer, sampleRateBuffer);
        //rawAudioBuffer = stb_vorbis_decode_filename("/tmp/test1.ogg", channelsBuffer, sampleRateBuffer);
        System.out.println("Size " + rawAudioBuffer.limit() * Short.BYTES);

        int channels = channelsBuffer.get(0);
        int sampleRate = sampleRateBuffer.get(0);

        int bufferPointer = alGenBuffers();
        alBufferData(bufferPointer, channelsToFormat(channels), rawAudioBuffer, sampleRate);
        return new SoundBuffer(bufferPointer);
    }

}
