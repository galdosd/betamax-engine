package com.github.galdosd.betamax.graphics;

import com.github.galdosd.betamax.imageio.ColorSample;
import com.github.galdosd.betamax.imageio.TextureImage;
import com.github.galdosd.betamax.imageio.TextureImagesIO;
import com.github.galdosd.betamax.opengl.TextureCoordinate;

import static com.google.common.base.Preconditions.checkState;

/** A proxy for a TextureImage that may or may not actually be loaded into RAM at any given time
 *  Must be unloaded in the main thread! Can be loaded from any thread.
 *
 */
public class LazyTextureImage implements  AutoCloseable {
    private TextureImage image;
    private final TextureName name;
    // TODO get rid of this lock
    private final Object $LOCK = new Object();

    public LazyTextureImage(TextureName name) {
        this.name = name;
    }

    public void uploadGl(int boundTarget) {
        checkLoaded();
        image.uploadGl(boundTarget);
    }

    public ColorSample getPixel(TextureCoordinate coordinate) {
        checkLoaded();
        return image.getPixel(coordinate);
    }

    public long getByteCount() {
        checkLoaded();
        return image.getByteCount();
    }

    @Override public void close()  {
        if(getLoaded()) unload();
    }

    void setLoaded(boolean loaded) {
        synchronized ($LOCK) {
            if (loaded == getLoaded()) return;
            if (loaded) load();
            else unload();
        }
    }

    boolean getLoaded() {
        // FIXME this will cause a block if another thread is partly done with loading.
        // also unloading but that's not a big deal because unloading is very fast
        // this also means our timers to catch late loads won't be accurate because the
        // late loading is hidden inside the getLoaded call
        synchronized ($LOCK) {
            return null != image;
        }
    }

    private void load() {
        image = TextureImagesIO.fromRgbaFile(name, true, true);
    }

    private void unload() {
        checkLoaded();
        image.close();
        image = null;
    }

    private void checkLoaded() {
        checkState(getLoaded(), "Image not loaded %s", name);
    }

    public TextureName getName() {
        return name;
    }
}
