package com.github.galdosd.betamax.sprite;

import com.github.galdosd.betamax.graphics.TextureCoordinate;

/**
 * FIXME: Document this class
 */
public interface Sprite {
    void render();
    void resetRenderedFrame();
    int getRenderedFrame();
    void advanceRenderedFrame(int frames);
    SpriteName getName();

    boolean isClickableAtCoordinate(TextureCoordinate coordinate);
    void setClickableEverywhere(boolean clickableEverywhere);

    int getTotalFrames();
}
