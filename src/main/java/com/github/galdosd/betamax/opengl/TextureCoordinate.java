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
    double x, y;
}
