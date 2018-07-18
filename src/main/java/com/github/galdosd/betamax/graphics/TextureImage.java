package com.github.galdosd.betamax.graphics;

import com.github.galdosd.betamax.OurTool;
import com.github.galdosd.betamax.opengl.TextureCoordinate;
import lombok.Getter;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * FIXME: Document this class
 */
public final class TextureImage {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());


    private final ByteBuffer bytePixelData;
    @Getter private final int width, height;
    private final String filename;

    private boolean unloaded = false;

    TextureImage(int width, int height, ByteBuffer bytePixelData, String filename) {
        this.width = width;
        this.height = height;
        this.bytePixelData = bytePixelData;
        bytePixelData.position(0);
        this.filename = filename;
    }

    ByteBuffer getBytePixelData() {
        checkState(0==bytePixelData.position(), "bytePixelData.position()!=0");
        return bytePixelData;
    }

    void saveToCache() throws IOException {
        checkArgument(!unloaded);
        // FIXME we should only save 1 byte per sample not the damn float -- at least test this with LZ4, disadvantage
        // is still have to convert to floats and that may be expensive? LZ4 is not dumb, it may be just fine
        try(FileChannel fileChannel = OurTool.writeCached(TextureImages.CACHE_KEY, filename)) {
            LOG.trace("Saving to cache: {}", filename);
            // FIXME write original filename as a safety check
            ByteBuffer byteHeader = ByteBuffer.allocate(Integer.BYTES * 2);
            IntBuffer intHeader = byteHeader.asIntBuffer();
            intHeader.put(width);
            intHeader.put(height);
            fileChannel.write(byteHeader);
            ByteBuffer localBytePixelData = getBytePixelData();
            fileChannel.write(localBytePixelData);
            // FileChannel#write fucks the position up despite failing to specify that in its contract
            // what a fucking junk heap
            localBytePixelData.position(0);
            LOG.info("Saved to cache: {}", filename);
        }
    }

    /** for textures that don't live the whole life of the program, you must call this
     * or you'll get a memory leak. we have to do this to have performant use of off heap memory
     * for opengl. no, finalizers are not a reasonable solution, they're crazy.
     */
    public void unload() {
        checkArgument(!unloaded);
        MemoryUtil.memFree(bytePixelData);
        unloaded = true;
    }

    public ColorSample getPixel(TextureCoordinate coordinate) {
        checkArgument(!unloaded);
        // XXX: should this be rounding or truncating? i have no idea
        int x = (int) (coordinate.getX() * (width - 1));
        int y = (int) (coordinate.getY() * (height - 1));
        checkArgument(x >= 0 && x < width);
        checkArgument(y >= 0 && y < height);
        int offset = TextureImages.BANDS * (width * y + x);
        ColorSample colorSample = new ColorSample(
                // these bytes have unsigned data stored on them
                getBytePixelData().get(offset) & 0xFF,
                getBytePixelData().get(offset + 1) & 0xFF,
                getBytePixelData().get(offset + 2) & 0xFF,
                getBytePixelData().get(offset + 3) & 0xFF
        );
        LOG.debug("Pixel at {}x{} is {} (from {})", x, y, colorSample, filename);
        return colorSample;
    }

    public void uploadGl(int boundTarget) {
        checkArgument(!unloaded);
        GL11.glTexImage2D( // TODO add a metrics timer here
                boundTarget, 0, GL11.GL_RGBA8, getWidth(), getHeight(), 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, bytePixelData
        );
    }
}
