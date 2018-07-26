package com.github.galdosd.betamax.graphics;

import com.codahale.metrics.Timer;
import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.sprite.Sprite;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;

/**
 * FIXME: Document this class
 */
public final class TextureRegistry {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private final Object LOCK$advisor = new Object();
    private TextureLoadAdvisor advisor;

    private final Timer texturePreloadAdvisingTimer = Global.metrics.timer("texturePreloadAdvisingTimer");

    private final Map<TextureName,Texture> textures = new ConcurrentHashMap<>();

    public TextureRegistry() {
        Thread daemonThread = new Thread(this::loadingThread, "TextureRegistry-loadingThread");
        daemonThread.setDaemon(true);
        daemonThread.start();
    }

    public Texture getTexture(TextureName imageFilename) {
        Texture texture = Texture.simpleTexture(imageFilename, true);
        checkState(!textures.containsKey(imageFilename));
        textures.put(imageFilename, texture);
        return texture;
    }

    public boolean checkAllSpritesReadyToRender(List<Sprite> sprites, int waitTimeMs) {
        return true;
    }

    public void setAdvisor(TextureLoadAdvisor advisor) {
        synchronized(LOCK$advisor) {
            this.advisor = advisor;
        }
    }

    public void loadingThread() {
        LOG.debug("started loading thread");
        for(;;) {
            List<TextureName> texturesToLoad = getNeededTextures();
            if(texturesToLoad.size() == 0) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) { }
            } else {
                LOG.debug("Loading {} textures in background: {}", texturesToLoad.size(), texturesToLoad);
                texturesToLoad.forEach(this::loadTextureImage);
            }
        }
    }

    private List<TextureName> getNeededTextures() {
        try (Timer.Context ignored = texturePreloadAdvisingTimer.time()) {
            synchronized (LOCK$advisor) {
                if (null == advisor) return new ArrayList<>();
                return advisor.getMostNeededTextures(Global.texturePreloadFrameLookahead).stream()
                        .filter(name -> !isCurrentlyLoaded(name))
                        .limit(Global.texturePreloadBatchSize)
                        .collect(toList());
            }
        }
    }

    private boolean isCurrentlyLoaded(TextureName name) {
        Texture texture = textures.get(name);
        checkState(null!=texture);
        return texture.getRamLoaded();
    }

    private void loadTextureImage(TextureName name) {
        Texture texture = textures.get(name);
        checkState(null!=texture);
        texture.setRamLoaded(true);
    }
}
