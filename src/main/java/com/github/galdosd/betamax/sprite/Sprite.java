package com.github.galdosd.betamax.sprite;

import com.github.galdosd.betamax.opengl.TextureCoordinate;

/**
 * FIXME: Document this class
 */
public interface Sprite {
    void render();
    boolean isClickableAtCoordinate(TextureCoordinate coordinate);
    void setClickableEverywhere(boolean clickableEverywhere);
    void resetRenderedFrame();
    int getRenderedFrame();
    void advanceRenderedFrame(int frames);
    SpriteName getName();
    void setLayer(int layer);
    int getLayer();
    int getCreationSerial();
    int getTotalFrames();

    String getTemplateName();
    int getAge();
}
