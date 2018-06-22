package com.github.galdosd.betamax.sprite;

/**
 * FIXME: Document this class
 */
public interface Sprite {
    void render();
    void resetFramecount();
    int getFramecount();
    void advanceFramecount(int frames);
}
