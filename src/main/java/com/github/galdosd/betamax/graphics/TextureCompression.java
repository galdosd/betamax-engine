package com.github.galdosd.betamax.graphics;

import com.codahale.metrics.Timer;
import com.github.galdosd.betamax.Global;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libc.LibCStdlib;
import org.lwjgl.util.lz4.LZ4;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * FIXME: Document this class
 */
public final class TextureCompression {
    private static final Timer memoryDecompressTimer = Global.metrics.timer("memoryDecompressTimer");

    private static final ByteBuffer LZ4_BUFFER = LibCStdlib.malloc(LZ4.LZ4_COMPRESSBOUND(
            1920*1080*Byte.BYTES*TextureImages.BANDS));

    private TextureCompression(){/*uninstantiable*/}

    static ByteBuffer compress(ByteBuffer localBytePixelData) {
        ByteBuffer lz4Buffer = getStaticBuffer();
        checkState(LZ4.LZ4_COMPRESSBOUND(localBytePixelData.remaining()) <= lz4Buffer.remaining());
        int compressedBytes = LZ4.LZ4_compress_default(localBytePixelData, lz4Buffer);
        checkState(0 != compressedBytes, "LZ4 compression failed");
        lz4Buffer.position(0);
        lz4Buffer.limit(compressedBytes);
        return lz4Buffer;
    }

    private static ByteBuffer getStaticBuffer() {
        LZ4_BUFFER.position(0);
        LZ4_BUFFER.limit(LZ4_BUFFER.capacity());
        return LZ4_BUFFER;
    }

    static ByteBuffer decompress(FileChannel readChannel, int expectedBytes) throws IOException {
        checkArgument(expectedBytes>0);
        ByteBuffer lz4Buffer = getStaticBuffer();

        int readCompressedBytes = readChannel.read(LZ4_BUFFER);
        checkState(readCompressedBytes>0, "Could not read enough compressed bytes");
        lz4Buffer.position(0);
        lz4Buffer.limit(readCompressedBytes);

        try(Timer.Context ignored = memoryDecompressTimer.time()) {
            ByteBuffer pixelBuffer = MemoryUtil.memAlloc(expectedBytes);
            int decompressedBytes = LZ4.LZ4_decompress_safe(LZ4_BUFFER, pixelBuffer);
            checkState(expectedBytes == decompressedBytes, "lz4 decompression failed: %d", decompressedBytes);
            return pixelBuffer;
        }

    }
}
