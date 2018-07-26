package com.github.galdosd.betamax.graphics;

import com.github.galdosd.betamax.sprite.Sprite;

import java.util.List;

/**
 * FIXME: Document this class
 */
public class TextureRegistry {
    private final Object LOCK$advisor = new Object();
    private TextureLoadAdvisor advisor;

    public Texture getTexture(TextureName imageFilename) {
        return Texture.simpleTexture(imageFilename, true);
    }

    public boolean checkAllSpritesReadyToRender(List<Sprite> sprites, int waitTimeMs) {
        return true;
    }

    public void setAdvisor(TextureLoadAdvisor advisor) {
        synchronized(LOCK$advisor) {
            this.advisor = advisor;
        }
    }
}
