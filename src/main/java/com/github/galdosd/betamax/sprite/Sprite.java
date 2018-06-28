package com.github.galdosd.betamax.sprite;

/**
 * FIXME: Document this class
 */
public interface Sprite {
    void render();
    void resetRenderedFrame();
    int getRenderedFrame();
    void advanceRenderedFrame(int frames);
}
