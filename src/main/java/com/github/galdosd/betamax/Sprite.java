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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.toList;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;


/** a full screen sprite
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
        checkArgument(0!=spriteFilenames.size(), "no sprite frame files found!");
        if(LOG.isTraceEnabled()) LOG.trace("Loading these sprite files for {}: {}",
                pkgName, Joiner.on("\n\t").join(spriteFilenames));
        textures = spriteFilenames.stream().map(name -> {
            Texture texture = new Texture();
            texture.bind(GL_TEXTURE_2D);
            texture.btSetParameters();
            texture.loadAlphaTiff(name);
            texture.btUploadTextureUnit();
            return texture;
        }).collect(toList());
        textureCount = textures.size();
    }

    public void resetFramecount(long currentFrame) {
        initialFrame = currentFrame;
    }

    public void render(long currentFrame) {
        // FIXME very leaky abstraction, bring more of the VBO and VAO stuff into Sprite
        // while removing the VBO/VAO stuff from GlProgramBase
        int frame = (int)((currentFrame - initialFrame) % textureCount);
        Texture texture = textures.get(frame);
        texture.bind(GL_TEXTURE_2D);
        glDrawArrays(GL_TRIANGLES, 0, 3*2);
    }
}
