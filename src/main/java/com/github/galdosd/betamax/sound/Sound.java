package com.github.galdosd.betamax.sound;

import com.github.galdosd.betamax.OurTool;
import lombok.Value;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libc.LibCStdlib;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static com.google.common.base.Preconditions.checkState;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_decode_filename;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_decode_memory;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_get_error;

/**
 * FIXME: Document this class
 */
public final class Sound implements AutoCloseable {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private final int channel, sampleRate;
    private final ShortBuffer data;

    private static final IntBuffer channelsBuffer = MemoryUtil.memAllocInt(1);
    private static final IntBuffer sampleRateBuffer = MemoryUtil.memAllocInt(1);
    private static final Object $LOCK = new Object();

    private Sound(int channel, int sampleRate, ShortBuffer data) {
        this.channel = channel;
        this.sampleRate = sampleRate;
        this.data = data;
    }

    @Override public void close() {
        LibCStdlib.free(data);
    }

    public static Sound loadSound(String fileName) {
        synchronized ($LOCK ) {
            //ShortBuffer rawAudioBuffer = stb_vorbis_decode_filename(fileName, channelsBuffer, sampleRateBuffer);
            ByteBuffer soundFileBuffer = null;
            ShortBuffer rawAudioBuffer = null;
            try {
                soundFileBuffer = OurTool.readOffHeapBuffer(fileName);
                rawAudioBuffer = stb_vorbis_decode_memory(soundFileBuffer, channelsBuffer, sampleRateBuffer);
                // TODO seems like it would be some legwork to construct a stb decoder just to get the damn error code
                checkState(null!=rawAudioBuffer, "could not load " + fileName);
                int channels = channelsBuffer.get();
                int sampleRate = sampleRateBuffer.get();
                LOG.debug("Loaded {}-channel {}hz sound ({} bytes) from {}",
                        channels, sampleRate, rawAudioBuffer.limit() * Short.BYTES, fileName);
                return new Sound(channels, sampleRate, rawAudioBuffer);
            }
            catch(Exception e) {
                if(null!=rawAudioBuffer) {
                    LibCStdlib.free(rawAudioBuffer);
                }
                throw new RuntimeException(e);
            }
            finally {
                if(null!=soundFileBuffer) {
                    MemoryUtil.memFree(soundFileBuffer);
                }
            }
        }
    }
}
