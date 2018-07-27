package com.github.galdosd.betamax.graphics;

import com.codahale.metrics.Timer;
import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.OurTool;
import com.github.galdosd.betamax.sprite.Sprite;
import com.github.galdosd.betamax.sprite.SpriteName;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * FIXME: Document this class
 * Couple performance improvement that could be made:
 *   - we dumbly use a sort of wait/sleep loop to wait for loaded sprites in checkAllSpritesReadyToRender from
 *     main thread waiting for loader thread. we should instead wait to be notified by the loader thread
 *   - since loader thread actively calls TextureLoadAdvisor instead of TextureLoadAdvisor pushing changes once per
 *     frame, we also use a stupid ineffecient wait/sleep loop there, and loader thread should wait to be notified by
 *     main thread. that said it's nice the advisory work is mostly done in the loader thread, it can take a few dozen
 *     ms and should not hold up a render, so it should just be a notify once per frame
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
        Texture texture = Texture.simpleTexture(imageFilename, false);
        checkState(!textures.containsKey(imageFilename));
        textures.put(imageFilename, texture);
        return texture;
    }

    public boolean checkAllSpritesReadyToRender(List<Sprite> sprites, int waitTimeMs) {
        long deadline = System.currentTimeMillis() + waitTimeMs;
        Set<TextureName> missingTextures = sprites.stream().map(sprite -> sprite.getTextureName(0)).collect(toSet());
        while(System.currentTimeMillis() < deadline && missingTextures.size() > 0) {
            List<TextureName> missingTexturesCopy = new ArrayList<>(missingTextures);
            for(TextureName textureName: missingTexturesCopy) {
                if(isCurrentlyLoaded(textureName)) missingTextures.remove(textureName);
            }
            OurTool.yield();
        }
        return missingTextures.size() == 0;
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
                OurTool.yield();
            } else {
                LOG.trace("Loading {} textures in background: {}", texturesToLoad.size(), texturesToLoad);
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
