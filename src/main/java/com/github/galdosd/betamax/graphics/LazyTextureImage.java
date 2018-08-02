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
        synchronized ($LOCK) {
            checkLoaded();
            image.uploadGl(boundTarget);
        }
    }

    public ColorSample getPixel(TextureCoordinate coordinate) {
        synchronized ($LOCK) {
            checkLoaded();
            return image.getPixel(coordinate);
        }
    }

    public long getByteCount() {
        synchronized ($LOCK) {
            checkLoaded();
            return image.getByteCount();
        }
    }

    @Override public void close()  {
        unload();
    }

    void setLoaded(boolean loaded) {
        if (loaded == getLoaded()) return;
        if (loaded) load();
        else unload();
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
        TextureImage newImage = TextureImagesIO.fromRgbaFile(name, true, true);
        synchronized ($LOCK) {
            // prevent leaks due to double load
            // this should never happen or at least be rare enough that doing the extra pointless load work
            // is not a major deal, certainly not worth holding the lock the whole time and slowing checkLoaded
            if(null!=image) newImage.close();
            else image = newImage;
        }
    }

    private void unload() {
        synchronized ($LOCK) {
            if(getLoaded()) {
                image.close();
                image = null;
            }
        }
    }

    private void checkLoaded() {
        //if(Global.debugMode) {
         //   checkState(getLoaded(), "Runtime RAM load: %s", getName());
       // } else {
            if(!getLoaded()) LOG.error("Runtime RAM load: {}", getName());
            setLoaded(true);
       // }
    }

    public TextureName getName() {
        return name;
    }
}
