package com.github.galdosd.betamax.graphics;

import com.github.galdosd.betamax.engine.FrameClock;
import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.opengl.TextureCoordinate;
import com.github.galdosd.betamax.sprite.Sprite;
import com.github.galdosd.betamax.sprite.SpriteName;
import lombok.Getter;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.toList;


/** a full screen sprite
 * a single SpriteTemplate can be used to have several sprites on the screen at once or at different times
 * each of which can be at different points in time in their animation (see Sprite interface)
 */
public final class SpriteTemplate {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());
    private final List<Texture> textures;
    private final String soundFilename;
    private final int textureCount;
    private static int nextCreationSerial = 0;
    private final String templateName;

    public SpriteTemplate(String templateName) {
        this.templateName = templateName;
        String pkgName = Global.spriteBase+templateName;
        Reflections reflections = new Reflections(pkgName+".", new ResourcesScanner());
        // TODO eliminate Reflections log line
        List<String> spriteFilenames = reflections.getResources(Pattern.compile(".*\\.tif"))
                .stream().sorted().collect(toList());
        List<String> soundFilenames = reflections.getResources(Pattern.compile(".*\\.ogg"))
                .stream().sorted().collect(toList());
        checkArgument(soundFilenames.size() <= 1, "Too many OGG files for sprite template %s", templateName);
        checkArgument(0!=spriteFilenames.size(), "no sprite frame files found for " + pkgName);
        LOG.debug("Loading {}-frame sprite {}", spriteFilenames.size(), pkgName);
        textures = spriteFilenames.stream().map(Texture::simpleTexture).collect(toList());
        if(soundFilenames.size() > 0) {
            soundFilename = soundFilenames.get(0);
        } else {
            soundFilename = null;
        }
        textureCount = textures.size();
    }

    public Sprite create(SpriteName name, FrameClock frameClock) {
        return new SpriteImpl(name, frameClock);
    }

    private void renderTemplate(int whichFrame) {
        Texture texture = textures.get(whichFrame);
        texture.render();
    }

    /**
     * A specific instance of a particular sprite template
     * the visual data is in the template, but the sprite has the state like current frame, layer depth,
     * position, etc. After all, you could have a Goomba SpriteTemplate but 28 goomba Sprites on the screen all in
     * different places, for example.
     */
    private class SpriteImpl implements Sprite {
        @Getter private final SpriteName name;
        private final FrameClock frameClock;
        @Getter private final int creationSerial;
        private int initialFrame;
        private boolean clickableEverywhere = false;
        @Getter private int layer = 0;
        @Getter private int repetitions = 1;

        private SpriteImpl(SpriteName name, FrameClock frameClock){
            this.frameClock = frameClock;
            this.name = name;
            initialFrame = frameClock.getCurrentFrame();
            creationSerial = nextCreationSerial++;
        }

        @Override public void render() {
            renderTemplate(getRenderedTexture());
        }

        @Override public int getCurrentFrame() {
            return (frameClock.getCurrentFrame() - initialFrame) % getTotalFrames();
        }

        @Override public String toString() {
            return "Sprite(" + getName() + ")";
        }

        @Override public boolean isClickableAtCoordinate(TextureCoordinate coord) {
            if(clickableEverywhere) {
                return true;
            }
            boolean transparentAtCoordinate = textures.get(getRenderedTexture()).isTransparentAtCoordinate(coord);
            LOG.trace("{}.isClickableAtCoordinate({}) == {}", this, coord, !transparentAtCoordinate);
            return !transparentAtCoordinate;
        }

        private int getRenderedTexture() {
            return getCurrentFrame() % textureCount;
        }

        @Override public void setClickableEverywhere(boolean clickableEverywhere) {
            LOG.debug("{}.setClickableEverywhere({})", this, clickableEverywhere);
            this.clickableEverywhere = clickableEverywhere;
        }

        @Override public void setLayer(int layer) {
            LOG.debug("{}.setLayer({})", this, layer);
            this.layer = layer;
        }

        @Override public int getTotalFrames() {
            return textureCount * repetitions;
        }

        @Override public void setRepetitions(int repetitions) {
            checkArgument(getAge()==0, "Only can setRepetitions when sprite is first created");
            checkArgument(repetitions>0);
            this.repetitions = repetitions;
        }

        @Override public String getTemplateName() {
            return templateName;
        }

        @Override public int getAge() {
            return frameClock.getCurrentFrame() - initialFrame;
        }

    }
}
