package com.github.galdosd.betamax.sound;

/**
 * FIXME: Document this class
 */
public class SoundRegistry implements  AutoCloseable {
    private final SoundWorld soundWorld;
    private final SoundSource soundSource;

    public SoundRegistry() {
        soundWorld = new SoundWorld();
        soundSource = soundWorld.newSource();
    }

    @Override public void close() throws Exception {
        soundSource.close();
    }
}
