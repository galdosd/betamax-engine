package com.github.galdosd.betamax.scripting;

import com.github.galdosd.betamax.sprite.Sprite;

/**
 * FIXME: Document this class
 */
public interface LogicHandler {
    void onSpriteCreate(Sprite sprite);
    void onSpriteDestroy(Sprite sprite);
    void onSpriteFrame(Sprite sprite);
    void onSpriteClick(Sprite sprite);
    void onBegin();
}
