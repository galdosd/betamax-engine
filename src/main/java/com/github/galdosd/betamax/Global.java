package com.github.galdosd.betamax;

import com.codahale.metrics.MetricRegistry;

/** It's not what it looks like honey! */
// TODO I know I know. Use DI or something. But only once it really matters. Extra magic ain't free
// in particular, (so far!) I've avoided sticking anything here that would impede easy mocks in unit tests
// that and i've put off making unit tests lol. jeez.
public final class Global {
    public final static MetricRegistry metrics = new MetricRegistry();
    public final static String spriteBase = "com.github.galdosd.betamax.sprites.";
    public final static String mainScript = "simplerexample.py";
    public final static String scriptBase = "com/github/galdosd/betamax/scripts/";
}
