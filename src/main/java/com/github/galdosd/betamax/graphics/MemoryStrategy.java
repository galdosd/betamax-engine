package com.github.galdosd.betamax.graphics;

import com.github.galdosd.betamax.Global;

/**
 * FIXME: Document this class
 */
public enum MemoryStrategy {
    /** always keep resident as much as possible */
    RESIDENT {
        public void afterRender(Texture texture) { /* always resident */ }

    },
    /** let TextureRegistry manage it based on its LRU strategy or whatever */
    MANAGED {
        public void afterRender(Texture texture) { /* FIXME give this to TextureRegistry */ }
    },
    /** always evict each frame after it is rendered */
    STREAMING {
        public void afterRender(Texture texture) {
            texture.setVramLoaded(false);
        }
    };

    public static MemoryStrategy choose(int size) {
        if(size > Global.textureMaxFramesForResidentMemoryStrategy) {
            return STREAMING;
        } else {
            return RESIDENT;
        }
    }

    public abstract void afterRender(Texture texture);
}
