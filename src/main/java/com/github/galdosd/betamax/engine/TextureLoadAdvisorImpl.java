package com.github.galdosd.betamax.engine;

import com.github.galdosd.betamax.graphics.SpriteTemplateRegistry;
import com.github.galdosd.betamax.graphics.TextureLoadAdvisor;
import com.github.galdosd.betamax.graphics.TextureName;
import com.github.galdosd.betamax.sprite.SpriteRegistry;

import java.util.List;

/**
 * FIXME: Document this class
 */
public final class TextureLoadAdvisorImpl implements TextureLoadAdvisor {
    private final SpriteRegistry spriteRegistry;
    private final SpriteTemplateRegistry spriteTemplateRegistry;

    public TextureLoadAdvisorImpl(SpriteRegistry spriteRegistry, SpriteTemplateRegistry spriteTemplateRegistry) {
        this.spriteRegistry = spriteRegistry;
        this.spriteTemplateRegistry = spriteTemplateRegistry;
    }

    @Override public List<TextureName> getMostNeededTextures(int frameLookahead) {
        throw new UnsupportedOperationException();
    }

    @Override public List<TextureName> getLeastNeededTextures(int frameLookahead, int maxVictims, List<TextureName> candidates) {
        throw new UnsupportedOperationException();
    }
}
