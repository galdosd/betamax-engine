package com.github.galdosd.betamax.imageio;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.opengl.TextureCoordinate;
import lombok.Getter;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * FIXME: Document this class
 */
public final class TextureImage implements  AutoCloseable {
    private static final Counter ramImageBytesCounter = Global.metrics.counter("ramImageBytes");
    private static final Counter ramTexturesCounter = Global.metrics.counter("ramTextures");
    private static final Timer textureUploadTimer = Global.metrics.timer("textureUploadTimer");

    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private final ByteBuffer bytePixelData;
    @Getter private final int width, height;
    @Getter private final String filename;

    private boolean unloaded = false;

    public String toString() {
        return String.format("TextureImage[%dx%d](%s)", width, height, filename);
    }

    TextureImage(int width, int height, ByteBuffer bytePixelData, String filename) {
        this.width = width;
        this.height = height;
        this.bytePixelData = bytePixelData;
        bytePixelData.position(0);
        checkState(bytePixelData.capacity() == bytePixelData.limit());
        checkState(getByteCount()==width*height* TextureImagesIO.BANDS);
        ramImageBytesCounter.inc(bytePixelData.capacity());
        ramTexturesCounter.inc();
        this.filename = filename;
    }

    ByteBuffer getBytePixelData() {
        checkState(!unloaded);
        checkState(0==bytePixelData.position(), "bytePixelData.position()!=0");
        checkState(width*height* TextureImagesIO.BANDS==bytePixelData.remaining(),
                "bytePixelData.remaining()==%d", bytePixelData.remaining());
        return bytePixelData;
    }

    /** for textures that don't live the whole life of the program, you must call this
     * or you'll get a memory leak. we have to do this to have performant use of off heap memory
     * for opengl. no, finalizers are not a reasonable solution, they're crazy.
     */
    public void close() {
        checkArgument(!unloaded);
        MemoryUtil.memFree(bytePixelData);
        ramImageBytesCounter.dec(bytePixelData.capacity());
        ramTexturesCounter.dec();
        unloaded = true;
    }

    public ColorSample getPixel(TextureCoordinate coordinate) {
        checkArgument(!unloaded);
        // XXX: should this be rounding or truncating? i have no idea
        int x = (int) (coordinate.getX() * (width - 1));
        int y = (int) (coordinate.getY() * (height - 1));
        checkArgument(x >= 0 && x < width);
        checkArgument(y >= 0 && y < height);
        int offset = TextureImagesIO.BANDS * (width * y + x);
        ColorSample colorSample = new ColorSample(
                // these bytes have unsigned data stored on them
                getBytePixelData().get(offset) & 0xFF,
                getBytePixelData().get(offset + 1) & 0xFF,
                getBytePixelData().get(offset + 2) & 0xFF,
                getBytePixelData().get(offset + 3) & 0xFF
        );
        LOG.trace("Pixel at {}x{} is {} (from {})", x, y, colorSample, filename);
        return colorSample;
    }

    public void uploadGl(int boundTarget) {
        checkArgument(!unloaded);
        try (Timer.Context _unused_context = textureUploadTimer.time()) {
            GL11.glTexImage2D( // TODO add a metrics timer here
                    boundTarget, 0, GL11.GL_RGBA8, getWidth(), getHeight(), 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, bytePixelData
            );
        }
    }

    public long getByteCount() {
        return getBytePixelData().capacity();
    }

    boolean getUnloaded() {
        return unloaded;
    }
}
