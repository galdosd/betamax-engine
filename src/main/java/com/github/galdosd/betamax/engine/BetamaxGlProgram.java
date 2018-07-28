package com.github.galdosd.betamax.engine;


import com.codahale.metrics.Timer;
import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.graphics.SpriteTemplateRegistry;
import com.github.galdosd.betamax.graphics.Texture;
import com.github.galdosd.betamax.graphics.TextureLoadAdvisor;
import com.github.galdosd.betamax.graphics.TextureRegistry;
import com.github.galdosd.betamax.gui.DevConsole;
import com.github.galdosd.betamax.opengl.TextureCoordinate;
import com.github.galdosd.betamax.scripting.EventType;
import com.github.galdosd.betamax.scripting.ScriptWorld;
import com.github.galdosd.betamax.sound.SoundRegistry;
import com.github.galdosd.betamax.sound.SoundWorld;
import com.github.galdosd.betamax.sprite.Sprite;
import com.github.galdosd.betamax.sprite.SpriteEvent;
import com.github.galdosd.betamax.sprite.SpriteName;
import com.github.galdosd.betamax.sprite.SpriteRegistry;
import org.lwjgl.glfw.GLFW;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;
import static org.lwjgl.opengl.GL11.*;

/**
 * FIXME: Document this class
 */
public class BetamaxGlProgram extends GlProgramBase {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private final Timer lateTextureUploadPhaseTimer = Global.metrics.timer("lateTextureUploadPhaseTimer");
    private final Timer loadingPhaseTimer = Global.metrics.timer("loadingPhaseTimer");

    private final SoundWorld soundWorld = new SoundWorld();
    private final SoundRegistry soundRegistry = new SoundRegistry(soundWorld);
    private final TextureRegistry textureRegistry = new TextureRegistry();
    private final SpriteTemplateRegistry spriteTemplateRegistry = new SpriteTemplateRegistry(soundRegistry, textureRegistry);
    private final DevConsole devConsole = new DevConsole();
    private ScriptWorld scriptWorld;
    private SpriteRegistry spriteRegistry;
    private Optional<SpriteName> highlightedSprite = Optional.empty();
    private OurShaders ourShaders;
    private Texture pausedTexture, loadingTexture, crashTexture;
    private boolean crashed = false;
    private boolean loading = false;
    private TextureLoadAdvisor textureLoadAdvisor;

    public static void main(String[] args) {
        new BetamaxGlProgram().run();
    }

    @Override protected void initialize() {
        if(Global.mainScript == null) {
            LOG.error("No main logic script defined (-Dbetamax.mainScript), exiting");
            throw new IllegalArgumentException("No main logic script defined");
        }
        ourShaders = new OurShaders();
        Texture.prepareForDrawing();
        prepareBuiltinTextures();

        // enable transparency
        glEnable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glBlendFunc( GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        newWorld(true);
    }

    private void prepareBuiltinTextures() {
        pausedTexture = Texture.simpleTexture(Global.pausedTextureFile, true);
        loadingTexture = Texture.simpleTexture(Global.loadingTextureFile, true);
        crashTexture = Texture.simpleTexture(Global.crashTextureFile, true);
    }

    private void newWorld(boolean resetSprites) {
        LOG.info("Starting new world");
        if(crashed) {
            LOG.warn("Restarting world after a crash. Unpredictable behavior could occur! (but maybe worth a shot...)");
            if(!resetSprites) {
                LOG.warn("F5 to resume is even more dangerous than Ctrl+F5, the crashed frame will not be repeated"
                        +"but its effects prior to the crash will still have taken place. It's your funeral buddy.");
            }
        }
        if(crashed) {
            soundWorld.globalUnpause();
        }
        crashed = false;
        loading = false;
        if(loadingPhaseTimerContext!=null) {
            loadingPhaseTimerContext.close();
            loadingPhaseTimerContext = null;
        }
        if(resetSprites) {
            LOG.info("Resetting sprites");
            textureRegistry.setAdvisor(null);
            if(null!=spriteRegistry) {
                spriteRegistry.close();
            }
            spriteRegistry = new SpriteRegistry(spriteTemplateRegistry, getFrameClock());
            textureLoadAdvisor = new TextureLoadAdvisorImpl(spriteRegistry, spriteTemplateRegistry);
            textureRegistry.setAdvisor(textureLoadAdvisor);

        }
        scriptWorld = new ScriptWorld(spriteRegistry);
        String[] scriptNames = Global.mainScript.split(",");
        try {
            scriptWorld.loadScripts(scriptNames);
        } catch(Exception e) {
            handleCrash(e);
        }
        getFrameClock().resetLogicFrames();
        resetPitch();
    }


    /** FIXME move to inputhandling */
    @Override protected void keyPressEvent(int key, int mods) {
        boolean controlKeyPressed = (mods & GLFW.GLFW_MOD_CONTROL) != 0;
        // exit upon ESC key
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            closeWindow();
            textureRegistry.setAdvisor(null); // we will get a flurry of silly loading otherwise
            LOG.info("Exiting");
        }
        else if(key == GLFW.GLFW_KEY_F1) {
            devConsole.helpWindow();
        }
        // show FPS metrics upon pause/break key
        else if(key == GLFW.GLFW_KEY_PRINT_SCREEN) {
            reportMetrics();
        }
        else if(key == GLFW.GLFW_KEY_TAB && getFrameClock().getPaused()) {
            // FIXME this does not advance sound!
            getFrameClock().stepFrame();
            updateLogic();
        }
        else if (key == GLFW.GLFW_KEY_PAUSE) {
            checkState(getFrameClock().getPaused() || !crashed);
            checkState(getFrameClock().getPaused() || !loading);
            if (crashed) {
                LOG.error("You can't unpause your way out of a crash. Use hotloading (F5 or Ctrl+F5) instead");
            } else if(loading) {
                LOG.warn("Cannot unpause until loading is done");
            } else {
                getFrameClock().setPaused(!getFrameClock().getPaused());
                if (getFrameClock().getPaused()) soundWorld.globalPause();
                else soundWorld.globalUnpause();
            }
            // FIXME this will fuck up the metrics,we need a cooked Clock for Metrics to ignore
            // the passage of time during pause
            // page up/down to change target FPS
        } else if (key == GLFW.GLFW_KEY_PAGE_UP) {
            if(controlKeyPressed) {
                updateFps(getFrameClock().getTargetFps() * 2);
            } else {
                updateFps(getFrameClock().getTargetFps() + 1);
            }
        } else if (key == GLFW.GLFW_KEY_PAGE_DOWN) {
            if(controlKeyPressed) {
                updateFps(getFrameClock().getTargetFps() / 2);
            } else {
                updateFps(getFrameClock().getTargetFps() - 1);
            }
        } else if(key == GLFW.GLFW_KEY_F5) {
            // restart the world if Ctrl+F5 is pressed
            // just reload scripts without affecting sprite state if just F5 is pressed
            // FIXME: right now state is managed in-python so the state will still be dropped
            // once we manage state in-engine we'll be fine
            newWorld(controlKeyPressed);
        }
    }

    private void updateFps(int newFps) {
        if(newFps <= 0) return;
        getFrameClock().setTargetFps(newFps);
        resetPitch();
    }

    /** make sound slow down / speed up as animation does */
    private void resetPitch() {
        soundWorld.globalPitch(((float)getFrameClock().getTargetFps()) / ((float)Global.targetFps));
    }

    @Override protected void mouseClickEvent(TextureCoordinate coordinate, int button) {
        LOG.debug("Clicked at {} x {}", coordinate.getX(), coordinate.getY());
        Optional<SpriteName> clickedSprite = spriteRegistry.getSpriteAtCoordinate(coordinate);
        if(clickedSprite.isPresent()) {
            handleSpriteClick(clickedSprite.get(), button);
        }
    }

    private void handleSpriteClick(SpriteName spriteName, int button) {
        if(getFrameClock().getPaused()) {
            return;
        }
        LOG.debug("Clicked on {} button {}", spriteName, button);
        if(button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            spriteRegistry.enqueueSpriteEvent(new SpriteEvent(EventType.SPRITE_CLICK, spriteName, 0));
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            Optional<SpriteName> selectedSprite = devConsole.getSelectedSprite();
            if(selectedSprite.isPresent() && spriteName.equals(selectedSprite.get())) {
                devConsole.clearHighlightedSprite();
            } else {
                highlightedSprite = Optional.of(spriteName);
            }
        }
    }

    private Timer.Context loadingPhaseTimerContext = null;
    private long nextConsoleUpdate = System.currentTimeMillis();
    @Override protected void updateView() {
        List<Sprite> spritesInRenderOrder = spriteRegistry.getSpritesInRenderOrder();

        /** wait for up to half a frame length to load textures before we give up and use a loading screen */
        boolean readyToRender = textureRegistry.checkAllSpritesReadyToRender(
                spritesInRenderOrder,
                10 * Global.textureLoadGracePeriodFramePercent / getFrameClock().getTargetFps());
        // we do this after waiting for sprites to (likely) be in RAM but before rendering because rendering
        // will evict MemoryStrategy.STREAMING sprite frames, and we need that frame to do mouse click collisions
        pollEvents();
        if(readyToRender) {
            checkState(getFrameClock().getPaused() || !loading);
            if(loading) {
                LOG.debug("exited LOADING state");
                loading = false;
                getFrameClock().setPaused(false);
                checkState(null!=loadingPhaseTimerContext);
                loadingPhaseTimerContext.close();
                loadingPhaseTimerContext = null;
            }
            clearScreen();
            // FIXME this is a hack TBQH but there's no time for the more principled way right now
            textureRegistry.processRamUnloadQueue();
            renderAllSprites(spritesInRenderOrder);
        } else {
            if(!loading) {
                LOG.debug("entered LOADING state");
                getFrameClock().setPaused(true);
                loading = true;
                checkState(null == loadingPhaseTimerContext);
                loadingPhaseTimerContext = loadingPhaseTimer.time();
            }
        }
        showPauseScreen();
        updateDevConsole();
    }

    private void showPauseScreen() {
        if(getFrameClock().getPaused()) {
            ourShaders.DEFAULT.use();
            checkState(!crashed || !loading);
            // FIXME three independent booleans (crashed, loading, FrameClock#getPaused) should be merged into one enum
            // for safety
            Texture texture = pausedTexture;
            if(crashed) texture = crashTexture;
            if(loading) texture = loadingTexture;
            texture.render();
        }
    }

    private void clearScreen() {
        ourShaders.DEFAULT.use();
        glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    private void renderAllSprites(List<Sprite> spritesInRenderOrder) {
        try(Timer.Context ignored = lateTextureUploadPhaseTimer.time()) {
            for (Sprite sprite : spritesInRenderOrder) {
                sprite.uploadCurrentFrame();
            }
        }
        Optional<SpriteName> selectedSprite = devConsole.getSelectedSprite();
        for(Sprite sprite: spritesInRenderOrder) {
            if(selectedSprite.isPresent() && sprite.getName().equals(selectedSprite.get())) {
                ourShaders.HIGHLIGHT.use();
            } else {
                ourShaders.DEFAULT.use();
            }
            sprite.render();
        }
    }

    String getActionStateString() {
        return crashed ? "CRASH" :
                        (loading ? "LOADING" :
                                (getFrameClock().getPaused() ? "PAUSE" : "PLAY"));
    }

    private void updateDevConsole() {
        if(System.currentTimeMillis() > nextConsoleUpdate) {
            devConsole.updateView(
                    spriteRegistry.getSpritesInRenderOrder(),
                    highlightedSprite,
                    scriptWorld.getAllCallbacks(),
                    scriptWorld.getStateVariables(),
                    getFrameClock(),
                    getActionStateString()
            );
            highlightedSprite = Optional.empty();
            nextConsoleUpdate = System.currentTimeMillis() + Global.devConsoleUpdateIntervalMillis;
        }
    }

    @Override protected void updateLogic() {
        try {
            if(!getFrameClock().getPaused()) spriteRegistry.dispatchEvents(scriptWorld);
        } catch(Exception e) {
            handleCrash(e);
        }
    }

    private void handleCrash(Exception e) {
        LOG.error("Crashed! This is usually due to a python script bug, in which case you can try resuming", new RuntimeException(e));
        getFrameClock().setPaused(true);
        soundWorld.globalPause();
        crashed = true;
    }

    @Override protected String getWindowTitle() { return "BETAMAX DEMO"; }
    @Override protected int getWindowHeight() { return 540; }
    @Override protected int getWindowWidth() { return 960; }
    @Override protected boolean getDebugMode() { return true; }

    @Override public void close() {
        if(null!=spriteRegistry) {
            spriteRegistry.close();
        }
        spriteTemplateRegistry.close();
    }
}

