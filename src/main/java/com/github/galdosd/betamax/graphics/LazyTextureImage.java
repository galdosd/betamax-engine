package com.github.galdosd.betamax.graphics;

import com.github.galdosd.betamax.imageio.ColorSample;
import com.github.galdosd.betamax.imageio.TextureImage;
import com.github.galdosd.betamax.imageio.TextureImagesIO;
import com.github.galdosd.betamax.opengl.TextureCoordinate;

/** A proxy for a TextureImage that may or may not actually be loaded into RAM at any given time
 */
public class LazyTextureImage implements  AutoCloseable {
    private final TextureImage image;
    public LazyTextureImage(String filename) {
        image = TextureImagesIO.fromRgbaFile(filename, true, true);
    }

    public void uploadGl(int boundTarget) {
       image.uploadGl(boundTarget);
    }

    public ColorSample getPixel(TextureCoordinate coordinate) {
        return image.getPixel(coordinate);
    }

    @Override public void close()  {
        image.close();
    }

    public long getByteCount() {
        return image.getByteCount();
    }
}
