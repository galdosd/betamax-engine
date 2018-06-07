package com.github.galdosd.betamax.sprite;

import com.github.galdosd.betamax.FrameClock;
import com.github.galdosd.betamax.Texture;
import com.google.common.base.Joiner;
import lombok.Getter;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.toList;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;


/** a full screen sprite
 */
public final class SpriteTemplate {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());
    private final List<Texture> textures;
    private final int textureCount;
    private final FrameClock frameClock;

    public SpriteTemplate(String pkgName, FrameClock frameClock) {
        this.frameClock = frameClock;
        Reflections reflections = new Reflections(pkgName , new ResourcesScanner());
        List<String> spriteFilenames = reflections.getResources(Pattern.compile(".*\\.tif"))
                .stream().sorted()
                .collect(toList());
        checkArgument(0!=spriteFilenames.size(), "no sprite frame files found for " + pkgName);
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

    public Sprite create() {
        return new SpriteImpl();
    }


    private void renderTemplate(int whichFrame) {
        // FIXME very leaky abstraction, bring more of the VBO and VAO stuff into SpriteTemplate
        // while removing the VBO/VAO stuff from GlProgramBase
        Texture texture = textures.get(whichFrame);
        texture.bind(GL_TEXTURE_2D);
        glDrawArrays(GL_TRIANGLES, 0, 3*2);
    }

    /**
     * A specific instance of a particular sprite template
     * the visual data is in the template, but the sprite has the state like current frame, layer depth,
     * position, etc. After all, you could have a Goomba SpriteTemplate but 28 goomba Sprites on the screen all in
     * different placees, for example.
     */
    private class SpriteImpl implements Sprite {
        int initialFrame = 0;

        private SpriteImpl(){
            initialFrame = frameClock.getCurrentFrame();
        }

        @Override public void render() {
            int currentFrame = frameClock.getCurrentFrame();
            int renderedFrame = (currentFrame - initialFrame) % textureCount;
            renderTemplate(renderedFrame);
        }

        @Override public void resetFramecount() {
            initialFrame = frameClock.getCurrentFrame();
        }

        @Override public void advanceFramecount(int frames) {
            initialFrame += frames;
        }

    }
}
