package com.github.galdosd.betamax.graphics;

import com.github.galdosd.betamax.engine.FrameClock;
import com.github.galdosd.betamax.opengl.TextureCoordinate;
import com.github.galdosd.betamax.sound.SoundBuffer;
import com.github.galdosd.betamax.sound.SoundName;
import com.github.galdosd.betamax.sound.SoundRegistry;
import com.github.galdosd.betamax.sound.SoundSource;
import com.github.galdosd.betamax.sprite.Sprite;
import com.github.galdosd.betamax.sprite.SpriteName;
import lombok.Getter;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;


/** a full screen sprite
 * a single SpriteTemplate can be used to have several sprites on the screen at once or at different times
 * each of which can be at different points in time in their animation (see Sprite interface)
 */
public final class SpriteTemplate implements  AutoCloseable {
    // TODO Textures and SoundBuffers should be unloaded, implement AutoCloseable down the line
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());
    private final List<Texture> textures;
    private Optional<SoundName> soundName;
    private Optional<SoundBuffer> soundBuffer = Optional.empty();
    private final int textureCount;
    private static int nextCreationSerial = 0;
    private final String templateName;
    private final MemoryStrategy memoryStrategy;


    public SpriteTemplate(SpriteTemplateManifest manifest, TextureRegistry textureRegistry) {
        this.templateName = manifest.getTemplateName();
        this.textures = manifest.getSpriteFilenames().stream().map(textureRegistry::getTexture).collect(toList());
        textureCount = textures.size();
        this.soundName = manifest.getSoundName();
        this.memoryStrategy = MemoryStrategy.choose(textures.size());
        LOG.debug("Constructed {}-frame sprite template {}", manifest.getSpriteFilenames().size(), templateName);
    }

    public void loadSoundBuffer(SoundRegistry soundRegistry) {
        if(soundBuffer.isPresent() || !soundName.isPresent()) return;
        soundBuffer = Optional.of(soundRegistry.getSoundBuffer(soundName.get()));
    }

    public Sprite create(SpriteName name, FrameClock frameClock) {
        return new SpriteImpl(name, frameClock);
    }

    private void renderTemplate(int whichFrame) {
        Texture texture = textures.get(whichFrame);
        texture.render();
        memoryStrategy.afterRender(texture);
    }
    private void uploadTexture(int whichFrame) {
        Texture texture = textures.get(whichFrame);
        texture.setVramLoaded(true);

    }

    public void close() {
        textures.forEach(Texture::close);
    }

    // FIXME break out SpriteImpl
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
        private boolean paused = false;
        private boolean hidden = false;
        private int pausedFrame = 0;
        private final Optional<SoundSource> soundSource;

        private SpriteImpl(SpriteName name, FrameClock frameClock){
            this.frameClock = frameClock;
            this.name = name;
            if(soundBuffer.isPresent()) {
                LOG.debug("Playing sound for sprite {}", getName());
                soundSource = Optional.of(soundBuffer.get().beginPlaying());
            } else {
                checkState(!soundName.isPresent(), "Sound was not loaded!");
                soundSource = Optional.empty();
            }
            initialFrame = frameClock.getCurrentFrame();
            creationSerial = nextCreationSerial++;
        }

        @Override public void render() {
            if(!getHidden()) renderTemplate(getRenderedTexture());
        }

        @Override public int getCurrentFrame() {
            if(paused) {
                return pausedFrame;
            } else {
                return (frameClock.getCurrentFrame() - initialFrame) % getTotalFrames();
            }
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

        @Override public TextureName getTextureName(int framesAhead) {
            return textures.get((getCurrentFrame() + framesAhead)% textureCount).getName();
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

        @Override public boolean getPaused() {
            return paused;
        }

        @Override public void setPaused(boolean paused) {
            if(paused == this.paused) return;
            if(paused) doPause();
            if(!paused) doUnpause();
            this.paused = paused;
        }

        @Override public boolean getHidden() {
            return hidden;
        }

        private void doUnpause() {
            initialFrame = frameClock.getCurrentFrame() - pausedFrame;
            pausedFrame = 0;
            if(soundSource.isPresent()) soundSource.get().resume();
        }

        private void doPause() {
            pausedFrame = getCurrentFrame();
            if(soundSource.isPresent()) soundSource.get().pause();
        }

        @Override public void setHidden(boolean hidden) {
            if(this.hidden == hidden) return;
            if(hidden && soundSource.isPresent()) soundSource.get().mute();
            if(!hidden && soundSource.isPresent()) soundSource.get().unmute();
            this.hidden = hidden;
        }

        @Override public void close() {
            if(soundSource.isPresent()) soundSource.get().close();
        }

        @Override public void uploadCurrentFrame() {
            uploadTexture(getRenderedTexture());
        }

        @Override public int getSoundPauseLevel() {
            if(soundSource.isPresent()) {
                return soundSource.get().getPauseLevel();
            } else {
                return -1;
            }
        }
    }

}
