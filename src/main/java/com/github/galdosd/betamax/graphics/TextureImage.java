package com.github.galdosd.betamax.graphics;

import com.github.galdosd.betamax.OurTool;
import lombok.Getter;
import lombok.Value;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.*;
import java.nio.FloatBuffer;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * FIXME: Document this class
 */
public final class TextureImage {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private static final int BANDS = 4; // RGBA so 4 samples per pixel
    private static final String CACHE_KEY = "TextureImage#loadAlphaTiff";

    @Getter private final FloatBuffer pixelData;
    @Getter private final int width, height;
    private final String filename;

    private boolean unloaded = false;

    private TextureImage(int width, int height, FloatBuffer pixelData, String filename) {
        this.width = width;
        this.height = height;
        this.pixelData = pixelData;
        this.filename = filename;
    }

    private static Optional<TextureImage> loadCached(String filename) throws IOException {
        Optional<InputStream> inputStreamOptional = OurTool.streamCached(CACHE_KEY, filename);
        if(inputStreamOptional.isPresent()) {
            try(DataInputStream dataInputStream = new DataInputStream(inputStreamOptional.get())) {
                int width = dataInputStream.readInt();
                int height = dataInputStream.readInt();
                final int colorSamples = width * height * BANDS;
                // these limits are arbitrary, maybe someday a 250 megapixel image will be reasonable
                checkArgument(width > 0 && height > 0 && width < 16384 && height < 16384,
                        "bad image size %sx%s", width, height);
                FloatBuffer pixelData = MemoryUtil.memAllocFloat(colorSamples);
                for (int jj = 0; jj < colorSamples; jj++) {
                    pixelData.put(dataInputStream.readFloat());
                }
                pixelData.flip();
                return Optional.of(new TextureImage(width, height, pixelData, filename));
            }
        } else {
            return Optional.empty();
        }
    }

    private void saveToCache() throws IOException {
        try(DataOutputStream outputStream = new DataOutputStream(OurTool.writeCachedStream(CACHE_KEY, filename))) {
            outputStream.writeInt(width);
            outputStream.writeInt(height);
            for (float datum : pixelData.array()) {
                outputStream.writeFloat(datum);
            }
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

        float[] pixels = raster.getPixels(0, 0, width, height, (float[]) null);
        checkState(pixels.length == (BANDS * width * height));
        FloatBuffer pixelData = convertForGl(width, height, pixels);

        return new TextureImage(width, height, pixelData, filename);
    }

    /** convert image data from file to be suitable for opengl
     *
     * 1. we have to turn it upside down because opengl texture coordinates have the origin
     *    at the southwest corner but most image formats start from the northwest corner
     * 2. Raster#getPixels gives us samples in 0.0f to 255.0f range, but opengl wants 0.0f to 1.0f
     * 3. we need an off-heap LWJGL native FloatBuffer, not a java on-heap FloatBuffer, so we can send
     *    the data to opengl later
     */
    private static FloatBuffer convertForGl(int width, int height, float[] pixels) {
        float[] upsideDownPixels = new float[pixels.length];
        for (int row = height - 1; row >= 0; row--) {
            for (int col = (width - 1) * BANDS; col >= 0; col--) {
                upsideDownPixels[BANDS * width * row + col] =
                        pixels[BANDS * width * (height - row - 1) + col] / 255.0f;
            }
        }
        // TODO maybe don't allocate a second array, make upsideDownPixels off-heap to begin with
        FloatBuffer pixelData = MemoryUtil.memAllocFloat(upsideDownPixels.length);
        pixelData.put(upsideDownPixels, 0, upsideDownPixels.length);
        pixelData.flip();
        return pixelData;
    }

    /** for textures that don't live the whole life of the program, you must call this
     * or you'll get a memory leak. we have to do this to have performant use of off heap memory
     * for opengl. no, finalizers are not a reasonable solution, they're crazy.
     */
    public void unload() {
        checkArgument(unloaded==false);
        MemoryUtil.memFree(pixelData);
        unloaded = true;
    }

    public ColorSample getPixel(TextureCoordinate coordinate) {
        checkArgument(unloaded==false);
        // XXX: should this be rounding or truncating? i have no idea
        int x = (int) (coordinate.getX() * width);
        int y = (int) (coordinate.getY() * height);
        int offset = BANDS * (width * y + x);
        return new ColorSample(
                pixelData.get(offset),
                pixelData.get(offset+1),
                pixelData.get(offset+2),
                pixelData.get(offset+3)
        );
    }

}
