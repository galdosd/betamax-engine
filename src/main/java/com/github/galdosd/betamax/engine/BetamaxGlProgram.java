package com.github.galdosd.betamax.engine;


import com.codahale.metrics.Timer;
import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.OurTool;
import com.github.galdosd.betamax.graphics.SpriteTemplateRegistry;
import com.github.galdosd.betamax.graphics.Texture;
import com.github.galdosd.betamax.graphics.TextureLoadAdvisor;
import com.github.galdosd.betamax.graphics.TextureRegistry;
import com.github.galdosd.betamax.gui.DevConsole;
import com.github.galdosd.betamax.opengl.ShaderProgram;
import com.github.galdosd.betamax.opengl.TextureCoordinate;
import com.github.galdosd.betamax.scripting.EventType;
import com.github.galdosd.betamax.scripting.ScriptWorld;
import com.github.galdosd.betamax.sound.SoundRegistry;
import com.github.galdosd.betamax.sound.SoundWorld;
import com.github.galdosd.betamax.sprite.Sprite;
import com.github.galdosd.betamax.sprite.SpriteEvent;
import com.github.galdosd.betamax.sprite.SpriteName;
import com.github.galdosd.betamax.sprite.SpriteRegistry;
import com.google.common.collect.ImmutableMap;
import lombok.Value;
import org.lwjgl.glfw.GLFW;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;
import static org.lwjgl.opengl.GL11.*;

/** Main entry point of BETAMAX game engine
 */
// FIXME this class is growing unwieldy, break it up. And I don't really understand what if any my principled
// division of duties between it and its parent GlProgramBase are. Whatever they should be if any, I also don't like
// the inheritance
public class BetamaxGlProgram extends GlProgramBase {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private final Timer lateTextureUploadPhaseTimer = Global.metrics.timer("lateTextureUploadPhaseTimer");
    private final Timer loadingPhaseTimer = Global.metrics.timer("loadingPhaseTimer");
    private final Timer allSpritesRenderTimer = Global.metrics.timer("allSpritesRenderTimer");
    private final Timer checkAllSpritesReadyTimer = Global.metrics.timer("checkAllSpritesReadyTimer");
    private final Timer pollEventsTimer = Global.metrics.timer("pollEventsTimer");
    private final Timer updateDevConsoleTimer = Global.metrics.timer("updateDevConsoleTimer");
    private final Timer processRamUnloadQueueTimer = Global.metrics.timer("processRamUnloadQueueTimer");

    private final SoundWorld soundWorld = new SoundWorld();
    private final SoundRegistry soundRegistry = new SoundRegistry(soundWorld);
    private final TextureRegistry textureRegistry = new TextureRegistry();
    private final SpriteTemplateRegistry spriteTemplateRegistry = new SpriteTemplateRegistry(soundRegistry, textureRegistry);
    private final DevConsole devConsole;
    private final SoundSyncer soundSyncer = new SoundSyncer();
    private ScriptWorld scriptWorld;
    private SpriteRegistry spriteRegistry;
    private Optional<SpriteName> highlightedSprite = Optional.empty();
    private OurShaders ourShaders;
    private Texture pausedTexture, loadingTexture, crashTexture;
    private boolean crashed = false;
    private boolean loading = false;

    private BetamaxGlProgram() {
        devConsole = getDebugMode() ? new DevConsole() : null;
    }

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
    }

    @Override protected void showInitialLoadingScreen() {
        // this crap should go into GlProgramBase
        // i still don't get what the difference between GlProgramBase and BetamaxGlProgram is tho.
        loadingTexture.render(TextureCoordinate.CENTER, ourShaders.DEFAULT);
    }

    @Override protected void expensiveInitialize() {
        spriteTemplateRegistry.preloadEverything();
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
            LOG.info("Creating sprite registry");
            spriteRegistry = new SpriteRegistry(spriteTemplateRegistry, getFrameClock());
            LOG.info("Setting texture load advisor");
            TextureLoadAdvisor textureLoadAdvisor = new TextureLoadAdvisorImpl(spriteRegistry, spriteTemplateRegistry);
            textureRegistry.setAdvisor(textureLoadAdvisor);
        }
        LOG.debug("Creating script world");
        scriptWorld = new ScriptWorld(spriteRegistry);
        String[] scriptNames = Global.mainScript.split(",");
        try {
            LOG.info("Loading scripts: {}", Global.mainScript);
            scriptWorld.loadScripts(scriptNames);
        } catch(Exception e) {
            handleCrash(e);
        }
        getFrameClock().resetLogicFrames();
        soundSyncer.reset();
        soundSyncer.needResync();
        resetPitch();
    }


    // XXX dirty hack
    @Value private static final class KeyPressEvent {
        int key, mods;
    }
    Queue<KeyPressEvent> enqueuedKeyPressEvents = new LinkedList<>();
    /** FIXME move to inputhandling */
    @Override protected void keyPressEvent(int key, int mods) {
       enqueuedKeyPressEvents.add(new KeyPressEvent(key,mods));
    }
    private void processKeyEvents() {
        KeyPressEvent keyPressEvent;
        while(null!=(keyPressEvent=enqueuedKeyPressEvents.poll())){
           doKeyPressEvent(keyPressEvent.getKey(), keyPressEvent.getMods());
        }
    }

    private void doKeyPressEvent(int key, int mods) {
        boolean controlKeyPressed = (mods & GLFW.GLFW_MOD_CONTROL) != 0;
        // exit upon ESC key
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            closeWindow();
            textureRegistry.setAdvisor(null); // we will get a flurry of silly loading otherwise
            LOG.info("Exiting");
        }
        else if(key == GLFW.GLFW_KEY_F1 && getDebugMode()) {
            devConsole.helpWindow();
        }
        // show FPS metrics upon pause/break key
        else if(key == GLFW.GLFW_KEY_PRINT_SCREEN) {
            reportMetrics();
        }
        else if(key == GLFW.GLFW_KEY_TAB && getFrameClock().getPaused() && getDebugMode()) {
            getFrameClock().stepFrame();
            updateLogic();
        }
        else if (key == GLFW.GLFW_KEY_HOME && controlKeyPressed) {
            soundSyncer.needResync();
        } else if (key == GLFW.GLFW_KEY_HOME && !controlKeyPressed) {
            loadGame();
        } else if (key == GLFW.GLFW_KEY_END) {
            saveGame();
        }
        else if (key == GLFW.GLFW_KEY_PAUSE) {
            hitPauseKey();
            // FIXME this will fuck up the metrics,we need a cooked Clock for Metrics to ignore
            // the passage of time during pause
            // page up/down to change target FPS
        } else if (key == GLFW.GLFW_KEY_PAGE_UP && getDebugMode()) {
            if(controlKeyPressed) {
                updateFps(getFrameClock().getTargetFps() * 2);
            } else {
                updateFps(getFrameClock().getTargetFps() + 1);
            }
        } else if (key == GLFW.GLFW_KEY_PAGE_DOWN && getDebugMode()) {
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
            // if debug mode is off, this always just restarts the world and place-in-time script reloading is
            // prevented
            newWorld(controlKeyPressed || !getDebugMode());
        }
    }

    private void loadGame() {
        List<String> saveFiles = Arrays.asList(new File(Global.snapshotDir).list());
        Future<Optional<String>> futureGameFile = OurTool.guiChoose("Choose save game file", saveFiles);
        try {

            Optional<String> optionalGameFile = futureGameFile.get();
            if(optionalGameFile.isPresent()) {
                GameplaySnapshot snapshot;
                try {
                    snapshot = GameplaySnapshot.readFromFile(Global.snapshotDir + optionalGameFile.get());
                } catch (IOException e) {
                    LOG.error("Could not load snapshot", e);
                    return;
                }
                LOG.info("Restoring file {} created {}", snapshot.getMnemonicName(), snapshot.getCreationDate());
                newWorld(true);
                scriptWorld.setGlobalShader(snapshot.getGlobalShader());
                getFrameClock().setCurrentFrame(snapshot.getCurrentFrame());
                spriteRegistry.restoreSnapshot(snapshot.getSprites());
                scriptWorld.setStateVariables(snapshot.getScriptVariables());
                LOG.info("Game state restore complete!");
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void hitPauseKey() {
        checkState(getFrameClock().getPaused() || !crashed);
        checkState(getFrameClock().getPaused() || !loading);
        if (crashed) {
            LOG.error("You can't unpause your way out of a crash. Use hotloading (F5 or Ctrl+F5) instead");
        } else if(loading) {
            LOG.warn("Cannot unpause until loading is done");
        } else {
            getFrameClock().setPaused(!getFrameClock().getPaused());
            if (getFrameClock().getPaused()) {
                soundWorld.globalPause();
            }
            else {
                soundWorld.globalUnpause();
                soundSyncer.needResync();
            }
        }
    }

    private void saveGame() {
        GameplaySnapshot snapshot = createSnapshot();
        try {
            LOG.debug("Created snapshot {}", snapshot.writeToFile());
        } catch (IOException e) {
            // FIXME for steam prod this should be kinder (GUI)
            LOG.error("Could not write snapshot to file!", e);
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
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && getDebugMode()) {
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
        soundSyncer.resyncIfNeeded(spritesInRenderOrder);

        boolean readyToRender;
        try(Timer.Context ignored = checkAllSpritesReadyTimer.time()) {
            /** wait for up to half a frame length to load textures before we give up and use a loading screen */
            readyToRender = textureRegistry.checkAllSpritesReadyToRender(
                    spritesInRenderOrder,
                    10 * Global.textureLoadGracePeriodFramePercent / getFrameClock().getTargetFps());
        }
        // we do this after waiting for sprites to (likely) be in RAM but before rendering because rendering
        // will evict MemoryStrategy.STREAMING sprite frames, and we need that frame to do mouse click collisions
        try(Timer.Context ignored = pollEventsTimer.time()) {
            pollEvents();
        }
        if(readyToRender) {
            exitLoadingMode();
            clearScreen();
            // FIXME this is a hack TBQH but there's no time for the more principled way right now
            try(Timer.Context ignored = processRamUnloadQueueTimer.time()) {
                textureRegistry.processRamUnloadQueue();
            }
            if (!getFrameClock().getPaused()) {
                // FIXME would be nice to render the previous frame even if paused but we just unloaded it, whoops
                try (Timer.Context ignored = allSpritesRenderTimer.time()) {
                    renderAllSprites(spritesInRenderOrder);
                }
            }
        } else {
            enterLoadingMode();
        }
        showPauseScreen();
        updateDevConsole();
        processKeyEvents();
        // FIXME last minute messy code
        if(scriptWorld.shouldWeRebootEverything()) {
            newWorld(true);
        }
    }
    private void enterLoadingMode() {
        if(!loading) {
            LOG.debug("entered LOADING state");
            getFrameClock().setPaused(true);
            loading = true;
            checkState(null == loadingPhaseTimerContext);
            loadingPhaseTimerContext = loadingPhaseTimer.time();
        }
    }

    private void exitLoadingMode() {
        checkState(getFrameClock().getPaused() || !loading);
        if(loading) {
            LOG.debug("exited LOADING state");
            loading = false;
            getFrameClock().setPaused(false);
            soundSyncer.needResync();
            checkState(null!=loadingPhaseTimerContext);
            loadingPhaseTimerContext.close();
            loadingPhaseTimerContext = null;
        }
    }

    private void showPauseScreen() {
        if(getFrameClock().getPaused()) {
            checkState(!crashed || !loading);
            // FIXME three independent booleans (crashed, loading, FrameClock#getPaused) should be merged into one enum
            // for safety
            Texture texture = pausedTexture;
            if(crashed) texture = crashTexture;
            if(loading) texture = loadingTexture;
            texture.render(TextureCoordinate.CENTER, ourShaders.DEFAULT);
        }
    }

    private void clearScreen() {
        ourShaders.DEFAULT.use();
        glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    private void renderAllSprites(List<Sprite> spritesInRenderOrder) {
        ShaderProgram globalShaderProgram = ourShaders.getShaderProgram(scriptWorld.getGlobalShader());
        try(Timer.Context ignored = lateTextureUploadPhaseTimer.time()) {
            for (Sprite sprite : spritesInRenderOrder) {
                sprite.uploadCurrentFrame();
            }
        }
        Optional<SpriteName> selectedSprite = Optional.empty();
        if(getDebugMode()) selectedSprite = devConsole.getSelectedSprite();
        for(Sprite sprite: spritesInRenderOrder) {
            boolean highlightSprite = getDebugMode()
                            && selectedSprite.isPresent()
                            && sprite.getName().equals(selectedSprite.get());
            updatePinnedToCursorSprite(sprite);
            sprite.render(highlightSprite ? ourShaders.HIGHLIGHT : globalShaderProgram);
        }
    }

    private void updatePinnedToCursorSprite(Sprite sprite) {
        if(sprite.getPinnedToCursor()) {
            TextureCoordinate cursorPosition = getCursorPosition();
            if(cursorPosition.isValid()) {
                sprite.setLocation(cursorPosition);
            }
        }
    }

    String getActionStateString() {
        return crashed ? "CRASH" :
                        (loading ? "LOADING" :
                                (getFrameClock().getPaused() ? "PAUSE" : "PLAY"));
    }

    private void updateDevConsole() {
        if(getDebugMode() && System.currentTimeMillis() > nextConsoleUpdate) {
            try(Timer.Context ignored = updateDevConsoleTimer.time()) {
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

    @Override protected String getWindowTitle() { return "MUTE"; }
    @Override protected int getWindowHeight() { return 540; }
    @Override protected int getWindowWidth() { return 960; }
    @Override protected boolean getDebugMode() { return Global.debugMode; }


    @Override public void close() {
        if(null!=spriteRegistry) {
            spriteRegistry.close();
        }
        spriteTemplateRegistry.close();
    }

    private GameplaySnapshot createSnapshot() {
        List<GameplaySnapshot.SpriteSnapshot> spriteSnapshots =
                spriteRegistry.getSpritesInRenderOrder().stream()
                        .map(Sprite::toSnapshot)
                        .collect(toList());
        return new GameplaySnapshot(
                scriptWorld.getGlobalShader(),
                getFrameClock().getCurrentFrame(),
                spriteSnapshots, ImmutableMap.copyOf(scriptWorld.getStateVariables())
        );
    }
}

