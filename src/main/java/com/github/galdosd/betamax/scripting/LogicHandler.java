package com.github.galdosd.betamax.scripting;

import com.github.galdosd.betamax.sprite.Sprite;

/**
 * FIXME: Document this class
 */
public interface LogicHandler {
    void onSpriteEvent(Sprite sprite, EventType eventType);
    void onBegin();
}
