package com.github.galdosd.betamax.sprite;

import com.github.galdosd.betamax.engine.GameplaySnapshot;
import com.github.galdosd.betamax.graphics.TextureName;
import com.github.galdosd.betamax.opengl.ShaderProgram;
import com.github.galdosd.betamax.opengl.TextureCoordinate;

/**
 * FIXME separate into two interfaces, one for scripts and one for internal engine rendering etc
 */
public interface Sprite extends AutoCloseable {
    void render(ShaderProgram shaderProgram);
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

    boolean getPinnedToCursor();
    void setPinnedToCursor(boolean pinnedToCursor);

    TextureCoordinate getLocation();
    void setLocation(TextureCoordinate location);

    String getSoundRemarks();
    /** Number of seconds that the playing source is ahead of where it should be (or negative if behind) */
    float getSoundDrift();

    void resyncSound();

    GameplaySnapshot.SpriteSnapshot toSnapshot();
}
