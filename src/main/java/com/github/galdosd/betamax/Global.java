package com.github.galdosd.betamax;

import com.codahale.metrics.MetricRegistry;

import static com.github.galdosd.betamax.OurTool.fromProperty;

/** It's not what it looks like honey! */
// TODO I know I know. Use DI or something. But only once it really matters. Extra magic ain't free
// in particular, (so far!) I've avoided sticking anything here that would impede easy mocks in unit tests
// that and i've put off making unit tests lol. jeez.
public final class Global {
    public static final MetricRegistry metrics = new MetricRegistry();
    public static final String spriteBase = "com.github.galdosd.betamax.sprites.";
    public static final String scriptBase = "com/github/galdosd/betamax/scripts/";
    public static final String helpFile = "help.txt";

    /** Optionally, you can have multiple scripts loaded in order, comma seperated.
     *  This is necessary because since we load from resources so jars will work,
     *  we end up breaking normal python import keyword
     */
    public static final String mainScript = fromProperty("betamax.mainScript", "objects 2.py");
    public static final String textureCacheDir = fromProperty("betamax.textureCacheDir");

    // TODO make these fromProperty
    public static final int defaultTargetFps = 8;
    public static final  int devConsoleUpdateIntervalMillis = 500;
    public static final boolean startFullScreen = false;
}
