package com.github.galdosd.betamax;

import com.github.galdosd.betamax.sprite.Sprite;
import com.github.galdosd.betamax.sprite.SpriteTemplate;

import java.util.Map;

/**
 * FIXME: Document this class
 */
public class ScriptWorld {
    void onSpriteCreate(Sprite sprite) {}
    void onSriteDestroy(Sprite sprite) {}
    void onSpriteFrame(Sprite sprite) {}
    void onSpriteClick(Sprite sprite) {}
    void onBegin() {}


    public static interface ScriptServicer {

        void provideScriptWorld(ScriptWorld scriptWorld);

        void log(String msg);
        void fatal(String msg);
        void exit();

        /** the values claim to be Objects but we're being lazy about type system here
         *  They must be Strings, Integers, or Booleans
         *  Support may be added for nested lists and maybe even maps if needed
         */
        Map<String,Object> getState(); // TODO is this the right interface for this?

        void createSprite(String templateName, String spriteName);
        void destroySprite(String spriteName);
        void resetSpriteFramecount(String spriteName);
        void resetSpriteFramecount(String spriteName, int framecount);
        int getSpriteFramecount(String spriteName);
        void advanceSpriteFramecount(String spriteName, int framecount);
    }
}
