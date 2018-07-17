package com.github.galdosd.betamax;

/**
 * FIXME: Document this class
 */
public interface FrameClock {
    int getCurrentFrame();
    boolean getPaused();
    void setPaused(boolean newPaused);


}
