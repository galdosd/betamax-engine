package com.github.galdosd.betamax.sound;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.OurTool;
import lombok.ToString;
import org.lwjgl.openal.AL10;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libc.LibCStdlib;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static com.google.common.base.Preconditions.checkState;
import static org.lwjgl.openal.AL10.alBufferData;
import static org.lwjgl.openal.AL10.alDeleteBuffers;
import static org.lwjgl.openal.AL10.alGenBuffers;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_decode_memory;

/**
 * FIXME: Document this class
 */
@ToString public final class SoundBuffer implements AutoCloseable {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private static final Counter ramSoundBytes = Global.metrics.counter("ramSoundBytes");
    private static final Timer soundDecodeTimer = Global.metrics.timer("soundDecodeTimer");
    private final int channels, sampleRate;
    private final int bytes;
    private final String filename;
    private final int handle;

    private static final IntBuffer channelsBuffer = MemoryUtil.memAllocInt(1);
    private static final IntBuffer sampleRateBuffer = MemoryUtil.memAllocInt(1);
    private static final Object $LOCK = new Object();

    private SoundBuffer(int channels, int sampleRate, int handle, int bytes, String filename) {
        this.channels = channels;
        this.sampleRate = sampleRate;
        this.handle = handle;
        this.bytes = bytes;
        this.filename = filename;
        ramSoundBytes.inc(bytes);
        LOG.debug("Loaded {}-channel {}hz sound ({} bytes) from {} (handle {})",
                channels, sampleRate, bytes, filename, handle);
    }

    @Override public void close() {
        SoundWorld.checkAlError();
        alDeleteBuffers(handle);
        SoundWorld.checkAlError();
        ramSoundBytes.dec(bytes);
    }

    int getHandle() {
        return handle;
    }

    private static int channelsToFormat(int channels) {
        if(channels == 1) return AL10.AL_FORMAT_MONO16;
        if(channels == 2) return AL10.AL_FORMAT_STEREO16;
        return -1;
    }

    static SoundBuffer loadSoundFromFile(String filename) {
        ByteBuffer soundFileBuffer = null;
        ShortBuffer rawAudioBuffer = null;
        Integer handle = null;
        int channels, sampleRate;
        try {
            soundFileBuffer = OurTool.readOffHeapBuffer(filename);
            synchronized ($LOCK) {
                channelsBuffer.position(0);
                sampleRateBuffer.position(0);
                try(Timer.Context ignored = soundDecodeTimer.time()) {
                    rawAudioBuffer = stb_vorbis_decode_memory(soundFileBuffer, channelsBuffer, sampleRateBuffer);
                }
                // TODO seems like it would be some legwork to construct a stb decoder just to get the damn error code
                checkState(null != rawAudioBuffer, "could not load " + filename);
                channels = channelsBuffer.get();
                sampleRate = sampleRateBuffer.get();
            }
            handle = alGenBuffers();
            alBufferData(handle, channelsToFormat(channels), rawAudioBuffer, sampleRate);
            SoundWorld.checkAlError();
            return new SoundBuffer(channels, sampleRate, handle, rawAudioBuffer.limit() * Short.BYTES, filename);
        } catch (Exception e) {
            if (null != handle) {
                alDeleteBuffers(handle);
            }
            throw new RuntimeException(e);
        } finally {
            if (null != soundFileBuffer) {
                MemoryUtil.memFree(soundFileBuffer);
            }
            if (null != rawAudioBuffer) {
                LibCStdlib.free(rawAudioBuffer);
            }
        }
    }

    public SoundSource beginPlaying() {
        SoundSource soundSource = new SoundSource();
        try {
            soundSource.playSound(this);
        } catch(Exception e) {
            soundSource.close();
            throw new RuntimeException(e);
        }
        return soundSource;
    }

    public int secondsToSamples(float seconds) {
        // XXX should this be multipled by channels too? what does openal think of as samples?
        return (int)(seconds * sampleRate);
    }

    public int totalSamples () {
        return bytes / channels / Short.BYTES;
    }

    public float totalSeconds() {
        return (float)totalSamples() / (float)sampleRate;
    }
}
