package com.github.galdosd.betamax.opengl;

import lombok.Value;

/** Opengl coordinates. Cf. TextureCoordinate. Origin is in the center of screen.
 */
@Value public class FramebufferCoordinate {
    public boolean isValid() {
        return x >= -1.0f && x <= 1.0f && y >= -1.0f && y <= 1.0f;
    }
    float x, y;
}
