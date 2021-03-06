package com.github.galdosd.betamax.engine;

import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.OurTool;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * FIXME: Document this class
 */
class GameLoopFrameClock implements FrameClock {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    // passage of time management
    private int frameCount = 0;
    private int targetFps = Global.targetFps;
    private long nextLogicFrameTime;
    private boolean paused = false;

    @Override public int getCurrentFrame() {
        return frameCount;
    }

    @Override public boolean getPaused() {
        return paused;
    }

    @Override public void setPaused(boolean newPaused) {
        if (newPaused == paused) {
            return;
        } else if (newPaused) {
            // pause
            LOG.info("Paused");
        } else {
            // unpause
            // we need to ignore the time spent in pause, otherwise we'll get a flood of catch up logic frames
            // and rendering will appear to skip
            resetLogicFrames();
            LOG.info("Unpaused");
        }
        paused = newPaused;
    }

    public int getTargetFps() {
        return targetFps;
    }

    public void setTargetFps(int targetFps) {
        checkArgument(targetFps>0);
        this.targetFps = targetFps;
        LOG.info("New target FPS: {}", targetFps);
    }

    public void beginLogicFrame() {
        if (!paused) {
            frameCount++;
        }
        nextLogicFrameTime += 1000 / targetFps;
    }

    public void stepFrame() {
        if(paused) {
            frameCount++;
        }
    }

    public boolean sleepTillNextLogicFrame() {
        return OurTool.sleepUntilPrecisely(nextLogicFrameTime - 1);
    }

    public boolean moreLogicFramesNeeded() {
        return !getPaused() && System.currentTimeMillis() > nextLogicFrameTime;
    }

    @Override public void resetLogicFrames() {
        LOG.debug("Reset logic frame counter");
        nextLogicFrameTime = System.currentTimeMillis();
    }

    public void setCurrentFrame(int currentFrame) {
        frameCount = currentFrame;
    }
}
