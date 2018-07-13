package com.github.galdosd.betamax.sprite;

import com.github.galdosd.betamax.FrameClock;
import com.github.galdosd.betamax.graphics.Texture;
import com.github.galdosd.betamax.graphics.TextureCoordinate;
import com.github.galdosd.betamax.graphics.TextureImages;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.toList;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;


/** a full screen sprite
 * a single SpriteTemplate can be used to have several sprites on the screen at once or at different times
 * each of which can be at different points in time in their animation (see Sprite interface)
 */
public final class SpriteTemplate {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());
    private final List<Texture> textures;
    private final int textureCount;

    public SpriteTemplate(String pkgName) {
        Reflections reflections = new Reflections(pkgName+".", new ResourcesScanner());
        // TODO eliminate Reflections log line
        List<String> spriteFilenames = reflections.getResources(Pattern.compile(".*\\.tif"))
                .stream().sorted()
                .collect(toList());
        checkArgument(0!=spriteFilenames.size(), "no sprite frame files found for " + pkgName);
        LOG.debug("Loading {}-frame sprite {}", spriteFilenames.size(), pkgName);
        textures = spriteFilenames.stream().map(name -> {
            Texture texture = new Texture(TextureImages.fromRgbaFile(name, true, true));
            texture.bind(GL_TEXTURE_2D);
            texture.btSetParameters();
            texture.btUploadTextureUnit();
            return texture;
        }).collect(toList());
        textureCount = textures.size();
    }

    public Sprite create(SpriteName name, FrameClock frameClock) {
        return new SpriteImpl(name, frameClock);
    }


    private void renderTemplate(int whichFrame) {
        // FIXME very leaky abstraction, bring more of the VBO and VAO stuff into SpriteTemplate
        // while removing the VBO/VAO stuff from GlProgramBase
        Texture texture = textures.get(whichFrame);
        texture.bind(GL_TEXTURE_2D);
        glClear(GL_DEPTH_BUFFER_BIT);
        glDrawArrays(GL_TRIANGLES, 0, 3*2);
    }

    /**
     * A specific instance of a particular sprite template
     * the visual data is in the template, but the sprite has the state like current frame, layer depth,
     * position, etc. After all, you could have a Goomba SpriteTemplate but 28 goomba Sprites on the screen all in
     * different places, for example.
     */
    private class SpriteImpl implements Sprite {
        private final SpriteName name;
        private final FrameClock frameClock;
        private int initialFrame;
        private boolean clickableEverywhere = false;

        private SpriteImpl(SpriteName name, FrameClock frameClock){
            this.frameClock = frameClock;
            this.name = name;
            initialFrame = frameClock.getCurrentFrame();
        }

        @Override public void render() {
            renderTemplate(getRenderedFrame());
        }

        @Override public void resetRenderedFrame() {
            initialFrame = frameClock.getCurrentFrame();
        }

        @Override public int getRenderedFrame() {
            return (frameClock.getCurrentFrame() - initialFrame) % textureCount;
        }

        @Override public void advanceRenderedFrame(int frames) {
            initialFrame += frames;
        }

        @Override public SpriteName getName() {
            return name;
        }

        @Override public String toString() {
            return "Sprite(" + getName() + ")";
        }
        @Override public boolean isClickableAtCoordinate(TextureCoordinate coord) {
            if(clickableEverywhere) {
                return true;
            }
            boolean transparentAtCoordinate = textures.get(getRenderedFrame()).isTransparentAtCoordinate(coord);
            LOG.trace("{}.isClickableAtCoordinate({}) == {}", this, coord, !transparentAtCoordinate);
            return !transparentAtCoordinate;
        }

        @Override public void setClickableEverywhere(boolean clickableEverywhere) {
           this.clickableEverywhere = clickableEverywhere;
        }

        @Override public int getTotalFrames() {
            return textureCount;
        }

    }
}
