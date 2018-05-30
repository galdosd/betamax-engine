package com.github.galdosd.betamax;

import com.codahale.metrics.MetricRegistry;

/** It's not what it looks like honey! */
// TODO I know I know. Use DI or something. But only once it really matters. Extra magic ain't free
public final class Global {
    public final static MetricRegistry metrics = new MetricRegistry();
}
