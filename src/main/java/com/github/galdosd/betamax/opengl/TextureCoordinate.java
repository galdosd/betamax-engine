package com.github.galdosd.betamax.opengl;

import lombok.Value;

/** Origin is in the southwest corner. 1,1 is the northeast corner
 * We made a class just to make it clearer which coordinate system we're using.
 *
 * Don't get confused with the two other coordinate systems we have the misfortune of dealing with
 * - opengl coordinates (origin is the center, 1,1 is northeast corner, -1,-1 is southwest corner)
 * - screen coordinates (origin is northwest corner, $WIDTH,$HEIGHT is southeast corner)
 *              ^ screen coordinates are barely dealt with at all though, we only see them in
 *              GlProgramBase's mouse callback which immediately converts to a TextureCoordinate
 */
@Value public final class TextureCoordinate {
    public static final TextureCoordinate CENTER = new TextureCoordinate(0.5,0.5);
    double x, y;

    public boolean isValid() {
        return x >= 0.0 && x <= 1.0 && y >= 0.0 && y <= 1.0;
    }

    public FramebufferCoordinate toFramebufferCoordinate() {
        return new FramebufferCoordinate(
                (float)(getX() * 2.0f - 1.0f),
                (float)(getY() * 2.0f - 1.0f)
        );
    }

    public String toShortString() {
        return String.format("%.3f t %.3f", getX(), getY());
    }
}
