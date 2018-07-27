package com.github.galdosd.betamax.engine;

import com.github.galdosd.betamax.graphics.SpriteTemplateRegistry;
import com.github.galdosd.betamax.graphics.TextureLoadAdvisor;
import com.github.galdosd.betamax.graphics.TextureName;
import com.github.galdosd.betamax.sprite.Sprite;
import com.github.galdosd.betamax.sprite.SpriteRegistry;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
        List<TextureName> needed = new LinkedList<>();
        Set<TextureName>  neededSet = new HashSet<>();
        for(int framesAhead = 0; framesAhead < frameLookahead; framesAhead++) {
            for(Sprite sprite: spriteRegistry.getSpritesInRenderOrder()) {
                TextureName textureName = sprite.getTextureName(framesAhead);
                if(!neededSet.contains(textureName)) needed.add(textureName);
                neededSet.add(textureName);
            }
        }
        return needed;
    }

//    @Override public List<TextureName> getLeastNeededTextures(
//            int frameLookahead, int maxVictims, List<TextureName> candidates) {
//        throw new UnsupportedOperationException();
//    }
}
