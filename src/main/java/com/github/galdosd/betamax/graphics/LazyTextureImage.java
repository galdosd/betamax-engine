package com.github.galdosd.betamax.graphics;

import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.imageio.ColorSample;
import com.github.galdosd.betamax.imageio.TextureImage;
import com.github.galdosd.betamax.imageio.TextureImagesIO;
import com.github.galdosd.betamax.opengl.TextureCoordinate;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkState;

/** A proxy for a TextureImage that may or may not actually be loaded into RAM at any given time
 *  Must be unloaded in the main thread! Can be loaded from any thread.
 *
 */
public class LazyTextureImage implements  AutoCloseable {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private TextureImage image;
    private final TextureName name;
    // TODO get rid of this lock
    private final Object $LOCK = new Object();

    public LazyTextureImage(TextureName name) {
        this.name = name;
    }

    public String toString() {
        return "LazyTextureImage=" + name.getFilename();
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
        if(Global.debugMode) {
            checkState(getLoaded(), "Runtime RAM load: %s", getName());
        } else {
            if(!getLoaded()) LOG.error("Runtime RAM load: {}", getName());
            setLoaded(true);
        }
    }

    public TextureName getName() {
        return name;
    }
}
