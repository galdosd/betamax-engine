package com.github.galdosd.betamax.imageio;

import lombok.Value;

@Value public final class ColorSample {
    int r, g, b, a;

    /** is this transparent enough to be considered transparent to the eye?
     */
    public boolean isTransparentEnough() {
        return a < 200;
    }
}

