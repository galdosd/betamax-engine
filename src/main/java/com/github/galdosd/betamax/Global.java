package com.github.galdosd.betamax;

import com.codahale.metrics.MetricRegistry;

import static com.github.galdosd.betamax.OurTool.fromProperty;

/** It's not what it looks like honey! */
// TODO I know I know. Use DI or something. But only once it really matters. Extra magic ain't free
// in particular, (so far!) I've avoided sticking anything here that would impede easy mocks in unit tests
// that and i've put off making unit tests lol. jeez.
public final class Global {
    public final static MetricRegistry metrics = new MetricRegistry();
    public final static String spriteBase = "com.github.galdosd.betamax.sprites.";
    public final static String scriptBase = "com/github/galdosd/betamax/scripts/";

    public final static int defaultTargetFps = 8;
    /** Optionally, you can have multiple scripts loaded in order, comma seperated.
     *  This is necessary because since we load from resources so jars will work,
     *  we end up breaking normal python import keyword
     */
    public final static String mainScript = fromProperty("betamax.mainScript", "objects.py");
    public final static String textureCacheDir = fromProperty("betamax.textureCacheDir");
    public final static  int devConsoleUpdateIntervalMillis = 500;
}
