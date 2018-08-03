package com.github.galdosd.betamax.sprite;

import com.github.galdosd.betamax.engine.GameplaySnapshot;
import com.github.galdosd.betamax.graphics.TextureName;
import com.github.galdosd.betamax.opengl.ShaderProgram;
import com.github.galdosd.betamax.opengl.TextureCoordinate;

/**
 * FIXME separate into two interfaces, one for scripts and one for internal engine rendering etc
 */
public interface Sprite extends AutoCloseable {
    /** Sprite clicking collision detection code iterates through sprites in reverse render order, ie
     *  front to back, and returns the first sprite that says it is clickable at the given coordinate
     *  Whether a sprite is clickable at a given coordinate is based on one of the strategies in this enum
     */
    enum Clickability {
        /** the sprite is clickable everywhere regardless of appearance. */
        EVERYWHERE,
        /** the sprite is clickable nowhere. it is skipped. useful for eg mouse cursor sprite */
        NOWHERE,
        /** the sprite is clickable iff the location it is clicked on isn't "transparent enough"
         *  ... definition of "enough" is in ColorSample#isTransparentEnough
         */
        TRANSPARENCY_BASED
    }
    void render(ShaderProgram shaderProgram);
    boolean isClickableAtCoordinate(TextureCoordinate coordinate);

    TextureName getTextureName(int framesAhead);

    /** used by scripts */
    void setClickability(Clickability clickability);
    /** compatibility shim. FIXME remove */
    default void setClickableEverywhere(boolean _true) {
        if(_true) {
            setClickability(Clickability.EVERYWHERE);
        }
    }

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
