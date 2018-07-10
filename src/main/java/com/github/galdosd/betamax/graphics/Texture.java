package com.github.galdosd.betamax.graphics;

import com.github.galdosd.betamax.OurTool;
import lombok.Value;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.nio.FloatBuffer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.lwjgl.opengl.GL11.*;

/**
 * FIXME: Document this class
 * For methods beginning with name of "bt" bind() must be called first
 */
public final class Texture {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    final int BANDS = 4; // RGBA so 4 samples per pixel
    final int handle;
    int boundTarget = 0;
    FloatBuffer pixelData;
    int width, height;

    public Texture() {
        handle = GL11.glGenTextures();
    }

    public void bind(int target) {
        glBindTexture(target, handle);
        boundTarget = target;
    }

    public void btSetParameters() {
        rebind();
        // glTexParameteri(target, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        // glTexParameteri(target, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        // FIXME will this segfault?
        // glTexParameterfv(boundTarget, GL_TEXTURE_BORDER_COLOR, new float[]{1.0f, 0.5f, 0.8f, 0.5f});

        glTexParameteri(boundTarget, GL11.GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(boundTarget, GL11.GL_TEXTURE_WRAP_T, GL_REPEAT);

        glTexParameteri(boundTarget, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        glTexParameteri(boundTarget, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    }

    private void rebind() {
        // TODO use glGet to ensure this really is the bound object. thanks opengl.
        checkArgument(0!=boundTarget, "call bind() to set GL target first");
        bind(boundTarget);
    }

    /** for textures that don't live the whole life of the program, you must call this
     * or you'll get a memory leak. we have to do this to have performant use of off heap memory
     * for opengl. no, finalizers are not a reasonable solution, they're crazy.
     */
    public void unload() {
        if(null!=pixelData) {
            width = height = 0;
            MemoryUtil.memFree(pixelData);
        }
    }

    public void loadAlphaTiff(String filename) {
        checkArgument(null==pixelData, "already loaded something else");
        BufferedImage image;
        try {
            image = ImageIO.read(OurTool.streamResource(filename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // TODO could optimize by not allocationg a new float[] everytime
        Raster raster = image.getData();
        width = raster.getWidth();
        height = raster.getHeight();
        LOG.trace("Loaded image from {} (size {}x{}) {}-band, DataBuffer dataType {}",
                filename, width, height, raster.getNumBands(),
                raster.getSampleModel().getDataType());

        float[] pixels = raster.getPixels(0, 0, width, height, (float[]) null);
        checkState(pixels.length == (BANDS * width * height));
        // opengl texture coordinates start from southwest corner, but
        // image formats usually start from northwest, so we turn it upside down
        float[] upsideDownPixels = new float[pixels.length];
        for (int row = height - 1; row >= 0; row--) {
            for (int col = (width - 1) * BANDS; col >= 0; col--) {
                // we also divide because Raster#getPixels gives us samples in 0.0f to 255.0f
                // but opengl wants 0.0f to 1.0f
                upsideDownPixels[BANDS * width * row + col] =
                        pixels[BANDS * width * (height - row - 1) + col] / 255.0f;
            }
        }
        // TODO ineffecient but we expect this to be at init time
        pixelData = MemoryUtil.memAllocFloat(upsideDownPixels.length);
        pixelData.put(upsideDownPixels, 0, upsideDownPixels.length);
        pixelData.flip();
    }

    @Value private static final class ColorSample {
        float r, g, b, a;

        /** is this transparent enough to be considered transparent to the eye?
         */
        public boolean isTransparentEnough() {
            return a < 0.8;
        }
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

    private ColorSample getPixel(TextureCoordinate coordinate) {
        // XXX: should this be rounding or truncating? i have no idea
        int x = (int) (coordinate.getX() * width);
        int y = (int) (coordinate.getY() * height);
        return getPixel(x,y);
    }

    public void btUploadTextureUnit() {
        checkArgument(null!=pixelData);
        checkState(0!=width && 0!=height);
        rebind();
        glTexImage2D(
                // TODO GL_INT or something would be more precise maybe but i couldn't get it to work
                boundTarget, 0, GL_RGBA, width, height, 0,
                GL_RGBA, GL_FLOAT, pixelData
        );
    }

    public boolean isTransparentAtCoordinate(TextureCoordinate coordinate) {
        ColorSample color = getPixel(coordinate);
        boolean transparentEnough = color.isTransparentEnough();
        LOG.trace("{}.isTransparentEnough() == {}", color, transparentEnough);
        return transparentEnough;
    }

}
