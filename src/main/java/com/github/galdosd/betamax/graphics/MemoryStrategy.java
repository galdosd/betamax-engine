package com.github.galdosd.betamax.graphics;

import com.github.galdosd.betamax.Global;

/**
 * FIXME: Document this class
 */
public enum MemoryStrategy {
    /** always keep resident as much as possible */
    RESIDENT {
        @Override public void afterRender(TextureRegistry textureRegistry, Texture texture) { /* always resident */ }

    },
    /** let TextureRegistry manage it based on its LRU strategy or whatever */
    MANAGED {
        @Override public void afterRender(TextureRegistry textureRegistry, Texture texture) {
            /* FIXME give this to TextureRegistry to manage*/ }
    },
    /** always evict each frame after it is rendered */
    STREAMING {
        @Override public void afterRender(TextureRegistry textureRegistry, Texture texture) {
            texture.setVramLoaded(false);
            textureRegistry.queueForRamUnload(texture);
        }
    };

    public static MemoryStrategy choose(int size) {
        if(size > Global.textureMaxFramesForResidentMemoryStrategy) {
            return STREAMING;
        } else {
            return RESIDENT;
        }
    }

    public abstract void afterRender(TextureRegistry textureRegistry, Texture texture);
}
