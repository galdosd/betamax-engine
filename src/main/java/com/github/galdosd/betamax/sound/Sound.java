package com.github.galdosd.betamax.sound;

import lombok.Value;

import java.nio.ShortBuffer;

/**
 * FIXME: Document this class
 */
public final class Sound {
    int channel, sampleRate;
    ShortBuffer data;

    Sound(int channel, int sampleRate, ShortBuffer data) {
        this.channel = channel;
        this.sampleRate = sampleRate;
        this.data = data;
    }

}
