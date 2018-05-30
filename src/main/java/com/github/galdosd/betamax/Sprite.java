package com.github.galdosd.betamax;

import com.google.common.base.Joiner;
import com.google.common.io.Resources;
import org.lwjgl.opengl.GL11;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;


/**
 * FIXME: Document this class
 */
public final class Sprite {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());
    private final List<Texture> textures;
    private final int textureCount;
    long initialFrame = 0;

    public Sprite(String pkgName) {
        Reflections reflections = new Reflections(pkgName , new ResourcesScanner());
        List<String> spriteFilenames = reflections.getResources(Pattern.compile(".*\\.tif"))
                .stream().sorted()
                .collect(toList());
        if(LOG.isTraceEnabled()) LOG.trace("Loading these sprite files for {}: {}",
                pkgName, Joiner.on("\n\t").join(spriteFilenames));
        textures = spriteFilenames.stream().map(name -> {
            Texture texture = new Texture();
            texture.bind(GL11.GL_TEXTURE_2D);
            texture.btSetParameters();
            texture.loadAlphaTiff(name);
            return texture;
        }).collect(toList());
        textureCount = textures.size();
    }

    public void resetFramecount(long currentFrame) {
        initialFrame = currentFrame;
    }

    public void render(long currentFrame) {
        int frame = (int)((currentFrame - initialFrame) % textureCount);
        Texture texture = textures.get(frame);
    }
}
