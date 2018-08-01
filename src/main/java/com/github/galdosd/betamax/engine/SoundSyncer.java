package com.github.galdosd.betamax.engine;

import com.codahale.metrics.Timer;
import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.OurTool;
import com.github.galdosd.betamax.sprite.Sprite;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * FIXME: Document this class
 */
public class SoundSyncer {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());
    private static final Timer resyncSoundTimer = Global.metrics.timer("resyncSoundTimer");

    boolean resyncNeeded = false;

    public void reset() {
        resyncNeeded = false;
    }

    public void needResync() {
        resyncNeeded = true;
    }

    public void resyncIfNeeded(List<Sprite> sprites) {
        computeIfResyncNeeded(sprites);
        if(resyncNeeded) {
            doResync(sprites);
            resyncNeeded = false;
        }
    }

    private void computeIfResyncNeeded(List<Sprite> sprites) {

    }

    private void doResync(List<Sprite> sprites) {
        try(Timer.Context ignored = resyncSoundTimer.time()) {
            LOG.debug("Resyncing sound");
            for (Sprite sprite : sprites) {
                sprite.resyncSound();
            }
        }
    }
}
