package com.github.galdosd.betamax.graphics;

import lombok.Value;

@Value public final class ColorSample {
    float r, g, b, a;

    /** is this transparent enough to be considered transparent to the eye?
     */
    public boolean isTransparentEnough() {
        return a < 0.8;
    }
}

