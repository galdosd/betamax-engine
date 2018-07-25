package com.github.galdosd.betamax.graphics;

import com.github.galdosd.betamax.sprite.Sprite;

import java.util.List;

/**
 * FIXME: Document this class
 */
public class TextureRegistry {

    public Texture getTexture(String imageFilename) {
        return Texture.simpleTexture(imageFilename);
    }

    public boolean checkAllSpritesReadyToRender(List<Sprite> sprites, int waitTimeMs) {
        return true;
    }
}
