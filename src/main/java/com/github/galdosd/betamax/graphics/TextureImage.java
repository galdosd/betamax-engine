package com.github.galdosd.betamax.graphics;

import com.github.galdosd.betamax.OurTool;
import lombok.Getter;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * FIXME: Document this class
 */
// TODO: split out most statics dealing with loading/unloading from file into ImageFiles statics container maybe
public final class TextureImage {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private static final int BANDS = 4; // RGBA so 4 samples per pixel
    private static final String CACHE_KEY = "TextureImage#loadAlphaTiff";

    private final ByteBuffer bytePixelData;
    @Getter private final int width, height;
    private final String filename;

    private boolean unloaded = false;

    final ByteBuffer getBytePixelData() {
        checkState(0==bytePixelData.position(), "bytePixelData.position()!=0");
        return bytePixelData;
    }
    private TextureImage(int width, int height, ByteBuffer bytePixelData, String filename) {
        this.width = width;
        this.height = height;
        this.bytePixelData = bytePixelData;
        bytePixelData.position(0);
        this.filename = filename;
    }

    private static Optional<TextureImage> loadCached(String filename) throws IOException {
        Optional<FileChannel> optionalFileChannel = OurTool.readCached(CACHE_KEY, filename);
        if(optionalFileChannel.isPresent()) {
            try(FileChannel readChannel = optionalFileChannel.get()) {
                LOG.trace("Loading from cache: {}", filename);

                final int headerSizeInBytes = 2 * Integer.BYTES;
                ByteBuffer byteHeader = ByteBuffer.allocate(headerSizeInBytes);
                IntBuffer intHeader = byteHeader.asIntBuffer();
                int readHeaderBytes = readChannel.read(byteHeader);
                checkState(headerSizeInBytes == readHeaderBytes, "read failure: cache file header");
                int width = intHeader.get();
                int height = intHeader.get();

                final int colorSamples = width * height * BANDS;
                // these limits are arbitrary, maybe someday a 250 megapixel image will be reasonable
                checkArgument(width > 0 && height > 0 && width < 16384 && height < 16384,
                        "bad image size %sx%s", width, height);

                ByteBuffer bytePixelData = MemoryUtil.memAlloc(colorSamples * Float.BYTES);
                int readPixelBytes = readChannel.read(bytePixelData); // TODO add metrics timer
                bytePixelData.flip();
                checkState(colorSamples * Float.BYTES == readPixelBytes, "read failure: cache file pixel data");

                checkState(bytePixelData.position()==0);
                LOG.info("Loaded from cache: {}", filename);
                return Optional.of(new TextureImage(width, height, bytePixelData, filename));
            }
        } else {
            return Optional.empty();
        }
    }

    private void saveToCache() throws IOException {
        checkArgument(!unloaded);
        // FIXME we should only save 1 byte per sample not the damn float -- at least test this with LZ4, disadvantage
        // is still have to convert to floats and that may be expensive? LZ4 is not dumb, it may be just fine
        try(FileChannel fileChannel = OurTool.writeCached(CACHE_KEY, filename)) {
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

    /** Load image from a file, possibly using a temporary directory as a cache.
     * The cache will contain a likely larger but faster to read version of the image, compressed losslessly with LZ4
     * This allows image loading during runtime to be much faster hopefully
     *
     * @param filename Must be a file readable by ImageIO with whatever plugins you have loaded.
     *                 I've only tested with some alpha tiffs we had. Must be in RGBA layout, you
     *                 will probably get bizarre results or worse otherwise. We do not explicitly verify
     *                 this, would require wasting a bunch of time figuring out SampleModel. Just watch out.
     * @param readCache If true, check the cache first
     * @param writeCache If true, write to the cache if it was empty
     */
    public static TextureImage fromRgbaFile(String filename, boolean readCache, boolean writeCache) {
        if (readCache) {
            try {
                Optional<TextureImage> cached = loadCached(filename);
                if(cached.isPresent()) {
                    return cached.get();
                }
            } catch (IOException e) {
                throw new RuntimeException("Fast-load cached texture exists but loading failed or file was corrupt", e);
            }
        }
        TextureImage textureImage = fromRgbaFile(filename);
        if(writeCache) {
            try {
                textureImage.saveToCache();
            } catch (IOException e) {
                throw new RuntimeException("Failed to write fast-load cached texture", e);
            }
        }
        return textureImage;
    }

    private static TextureImage fromRgbaFile(String filename) {
        BufferedImage image;
        try (InputStream inputStream = OurTool.streamResource(filename)){
            image = ImageIO.read(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // TODO could optimize by not allocating a new float[] everytime
        Raster raster = image.getData();
        int width = raster.getWidth();
        int height = raster.getHeight();
        LOG.trace("Loaded image from {} (size {}x{}) {}-band, DataBuffer dataType {}",
                filename, width, height, raster.getNumBands(),
                raster.getSampleModel().getDataType());

        int[] pixels = raster.getPixels(0, 0, width, height, (int[]) null);
        ByteBuffer pixelData = convertForGl(width, height, pixels);

        return new TextureImage(width, height, pixelData, filename);
    }

    /** convert image data from file to be suitable for opengl
     *
     * 1. we have to turn it upside down because opengl texture coordinates have the origin
     *    at the southwest corner but most image formats start from the northwest corner
     * 2. Raster#getPixels gives us unsigned ints but opengl wants signed bytes
     * 3. we need an off-heap LWJGL native ByteBuffer FloatBuffer, not a java on-heap FloatBuffer, so we can send
     *    the data to opengl later
     */
    private static ByteBuffer convertForGl(int width, int height, int[] samples) {
        final int sampleCount = BANDS * width * height;
        checkState(samples.length == sampleCount);
        byte[] fixedSamples = new byte[sampleCount];
        for (int row = height - 1; row >= 0; row--) {
            for (int col = (width - 1) * BANDS; col >= 0; col--) {
                fixedSamples[BANDS * width * row + col] =
                        (byte)(samples[BANDS * width * (height - row - 1) + col] - 128);
            }
        }
        // TODO maybe don't allocate a second array, make fixedSamples off-heap to begin with
        ByteBuffer bytePixelData = MemoryUtil.memAlloc(sampleCount);
        bytePixelData.put(fixedSamples, 0, sampleCount);
        bytePixelData.flip();
        checkState(bytePixelData.position()==0);
        return bytePixelData;
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
        int x = (int) (coordinate.getX() * width);
        int y = (int) (coordinate.getY() * height);
        int offset = BANDS * (width * y + x);
        return new ColorSample(
                getBytePixelData().get(offset),
                getBytePixelData().get(offset+1),
                getBytePixelData().get(offset+2),
                getBytePixelData().get(offset+3)
        );
    }

    public void uploadGl(int boundTarget) {
        checkArgument(!unloaded);
        GL11.glTexImage2D( // TODO add a metrics timer here
                boundTarget, 0, GL11.GL_RGBA, getWidth(), getHeight(), 0,
                GL11.GL_RGBA, GL11.GL_BYTE, bytePixelData
        );
    }
}
