package com.github.galdosd.betamax.sprite;

import com.github.galdosd.betamax.graphics.TextureName;
import com.github.galdosd.betamax.opengl.TextureCoordinate;

/**
 * FIXME: Document this class
 */
public interface Sprite extends AutoCloseable {
    void render();
    boolean isClickableAtCoordinate(TextureCoordinate coordinate);

    TextureName getTextureName(int framesAhead);

    /** used by scripts */
    void setClickableEverywhere(boolean clickableEverywhere);
    int getCurrentFrame();
    SpriteName getName();
    /** used by scripts */
    void setLayer(int layer);
    int getLayer();
    int getCreationSerial();
    int getTotalFrames();

    void setRepetitions(int repetitions);
    int getRepetitions();
    String getTemplateName();
    int getAge();

    boolean getPaused();
    void setPaused(boolean paused);

    boolean getHidden();
    void setHidden(boolean hidden);

    void close();

    void uploadCurrentFrame();

    int getSoundPauseLevel();

    TextureCoordinate getLocation();
    void setLocation(TextureCoordinate location);
}
