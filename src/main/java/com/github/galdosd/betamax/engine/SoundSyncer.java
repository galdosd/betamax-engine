package com.github.galdosd.betamax.engine;

import com.codahale.metrics.Timer;
import com.github.galdosd.betamax.Global;
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
    // we're computing a running average but only saving 2 values instead of having to save N
    float meanDriftTimesFramesTracked = 0.0f;
    int framesTracked = 0;

    public void reset() {
        resyncNeeded = false;
        framesTracked = 0;
        meanDriftTimesFramesTracked = 0.0f;
    }

    public void needResync() {
        resyncNeeded = true;
    }

    public void resyncIfNeeded(List<Sprite> sprites) {
        computeIfResyncNeeded(sprites);
        if(resyncNeeded) {
            doResync(sprites);
            reset();
        }
    }

    private void computeIfResyncNeeded(List<Sprite> sprites) {
        float maxDrift = 0.0f;
        for(Sprite sprite: sprites) {
            float soundDrift = Math.abs(sprite.getSoundDrift());
            if (soundDrift > maxDrift) {
                maxDrift = soundDrift;
            }
        }
        if(maxDrift < (Global.maxSoundDriftMillis / 1000.0f)) {
            if(framesTracked > 0) {
                meanDriftTimesFramesTracked += maxDrift;
                framesTracked++;
                float meanDrift = meanDriftTimesFramesTracked / (float) framesTracked;
                LOG.trace("............Ignoring temporary sound drift run after {} frames of {} ms",
                        framesTracked, (int) (meanDrift * 1000));
                framesTracked = 0;
                meanDriftTimesFramesTracked = 0.0f;
            }
        } else {
            framesTracked++;
            meanDriftTimesFramesTracked += maxDrift;
        }
        if(framesTracked > Global.driftTolerancePeriodFrames) {
            float meanDrift = meanDriftTimesFramesTracked / (float)framesTracked;
            LOG.debug("Detected and scheduled fix for desynced sound: %d ms", (int)meanDrift * 1000);
            resyncNeeded = true;
        }
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
