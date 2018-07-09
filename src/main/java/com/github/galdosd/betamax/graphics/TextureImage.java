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
/*
    public static TextureImage loadCached(String filename) {
        Optional<InputStream> inputStream = OurTool.streamCached(CACHE_KEY, filename);
        inputStream.get().read()
        if(inputStream.isPresent()) {
            DataInputStream dataInputStream = new DataInputStream(inputStream.get());
            int width = dataInputStream.readInt();
            int height = dataInputStream.readInt();
            final int imageBytes = width * height * BANDS * Float.BYTES;
            FloatBuffer pixelData = MemoryUtil.memAllocFloat();
            ByteArrayInputStream
            pixelData.

        }
    }

    private void saveToCache() {
        //OutputStream outputStream = OurTool.writeCachedStream(CACHE_KEY, loadedFilename);
    }*/

    public static TextureImage fromAlphaTiffFile(String filename) {
        BufferedImage image;
        try {
            image = ImageIO.read(OurTool.streamResource(filename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // TODO could optimize by not allocationg a new float[] everytime
        Raster raster = image.getData();
        int width = raster.getWidth();
        int height = raster.getHeight();
        LOG.debug("Loaded image from {} (size {}x{}) {}-band, DataBuffer dataType {}",
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

    private ColorSample getPixel(int x, int y) {
        int offset = BANDS * (width * y + x);
        return new ColorSample(
                pixelData.get(offset),
                pixelData.get(offset+1),
                pixelData.get(offset+2),
                pixelData.get(offset+3)
        );
    }

    public ColorSample getPixel(TextureCoordinate coordinate) {
        checkArgument(unloaded==false);
        // XXX: should this be rounding or truncating? i have no idea
        int x = (int) (coordinate.getX() * width);
        int y = (int) (coordinate.getY() * height);
        return getPixel(x,y);
    }

}
