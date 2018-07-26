package com.github.galdosd.betamax;

import com.codahale.metrics.MetricRegistry;
import com.github.galdosd.betamax.graphics.TextureName;

import static com.github.galdosd.betamax.OurTool.fromProperty;

/** It's not what it looks like honey! */
// TODO I know I know. Use DI or something. But only once it really matters. Extra magic ain't free
// in particular, (so far!) I've avoided sticking anything here that would impede easy mocks in unit tests
// that and i've put off making unit tests lol. jeez.
public final class Global {
    public static final MetricRegistry metrics = new MetricRegistry();
    public static final String spriteBase = "com.github.galdosd.betamax.sprites.";
    public static final String scriptBase = "com/github/galdosd/betamax/scripts/";
    public static final String shaderBase = "com/github/galdosd/betamax/shaders/";
    public static final String helpFile = "help.txt";
    public static final TextureName pausedTextureFile = new TextureName("paused.tif");
    public static final TextureName loadingTextureFile = new TextureName("loading.tif");
    public static final TextureName crashTextureFile = new TextureName("crash.tif");

    /** Optionally, you can have multiple scripts loaded in order, comma separated.
     *  This is necessary because since we load from resources so jars will work,
     *  we end up breaking normal python import keyword
     */

    public static final int targetFps = fromProperty("betamax.targetFps", 8);
    public static final  int devConsoleUpdateIntervalMillis = fromProperty("betamax.devConsoleUpdateInterval", 500);
    public static final boolean startFullScreen = fromProperty("betamax.startFullScreen", false);
    public final static String mainScript = fromProperty("betamax.mainScript");
    public final static String textureCacheDir = fromProperty("betamax.textureCacheDir");
    public static final boolean enableSound = fromProperty("betamax.enableSound", true);
    public static final int textureMaxFramesForResidentMemoryStrategy =
            fromProperty( "betamax.textureMaxFramesForResidentMemoryStrategy", 10);
    public static final int texturePreloadFrameLookahead = fromProperty(
            "betamax.texturePreloadFrameLookahead", 16
    );
    public static final int texturePreloadBatchSize = fromProperty("betamax.texturePreloadBatchSize", 1);
}
