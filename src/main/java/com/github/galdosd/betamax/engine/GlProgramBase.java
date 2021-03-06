package com.github.galdosd.betamax.engine;

import com.codahale.metrics.*;
import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.opengl.GlWindow;
import com.github.galdosd.betamax.opengl.TextureCoordinate;
import javafx.application.Platform;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.slf4j.LoggerFactory;

import java.nio.DoubleBuffer;
import java.util.concurrent.TimeUnit;

import static com.github.galdosd.betamax.OurTool.checkGlError;
import static com.google.common.base.Preconditions.checkState;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;

/** FIXME: document this class
 * TODO instead of being subclassable, have an interface and accept that
 * TODO: for safety in debug mode use glGet to make sure the right target is bound for buffers,
 *  textures, etc. basically try as hard as possible to fight the dumbass statefullness of GL calls
 *  that require params to be "bound"
 */
public abstract class GlProgramBase implements AutoCloseable {
    //TODO all GL wrapped arguments passed from subclass/users of GlprogramBase should be typesafe, not
    //some fucking ints. fuck you C people.
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private GlWindow mainWindow;
    private final GameLoopFrameClock frameClock = new GameLoopFrameClock();

    // FPS performance metrics
    private final Timer userInitTimer = Global.metrics.timer("userInitTimer");
    private final Timer renderTimer = Global.metrics.timer("renderTimer");
    private final Timer logicTimer = Global.metrics.timer("logicTimer");
    private final Timer fullLogicTimer = Global.metrics.timer("fullLogicTimer");
    private final Timer idleTimer = Global.metrics.timer("idleTimer");
    private final Timer idleTimer5s = Global.metrics.timer("idleTimer5s", () ->
            new Timer(new SlidingTimeWindowReservoir(5, TimeUnit.SECONDS)));
    private final Timer videoFrameDriftTimer = Global.metrics.timer("videoFrameDriftTimer");
    private final Counter skippedFramesByLogicCounter = Global.metrics.counter("skippedFramesByLogic");
    private final Counter skippedFramesByRenderCounter = Global.metrics.counter("skippedFramesByRender");

    private final ConsoleReporter reporter = ConsoleReporter.forRegistry(Global.metrics)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();


    protected final GameLoopFrameClock getFrameClock() {
        return frameClock;
    }

    public final void run() {
        try {
            GlWindow.initGlfw(getDebugMode());
            try(GlWindow _mainWindow = new GlWindow(getWindowWidth(), getWindowHeight(), getWindowTitle(),
                    this::keyCallback, this::mouseButtonCallback, Global.startFullScreen)){
                mainWindow = _mainWindow;
                try {
                    try (Timer.Context _unused_context = userInitTimer.time()) {
                        initialize();
                        checkGlError();
                        try (GlWindow.RenderPhase __unused_context = mainWindow.renderPhase()) {
                            showInitialLoadingScreen();
                        }
                        checkGlError();
                        expensiveInitialize();
                        checkGlError();
                        LOG.debug("User initialization done");
                    }
                    frameClock.resetLogicFrames();
                    try {
                        while (!mainWindow.getShouldClose()) {
                            loopOnce();
                        }
                    } finally {
                        closeWindow();
                    }
                } catch(Exception e) {
                    LOG.error("Exiting due to exception", e);
                } finally {
                    close();
                }
            }
        } finally {
            GlWindow.shutdownGlfw();
            Platform.exit();
        }
    }


    private void mouseButtonCallback(long window, int button, int action, int mods){
        if(action == GLFW.GLFW_PRESS) {
            TextureCoordinate coord = mainWindow.getCursorPosition();
            if(coord.isValid()) {
                mouseClickEvent(coord, button);
            } else {
                LOG.warn("Out of bounds {} due to excessively delayed handling of mouse click");
            }
        }
    }

    protected final TextureCoordinate getCursorPosition() {
        return mainWindow.getCursorPosition();
    }

    private void keyCallback(long window, int key, int scancode, int action, int mods) {
        if(action == GLFW.GLFW_PRESS) {
            keyPressEvent(key, mods);
        }
    }

    protected final void reportMetrics() {
        reporter.report();
    }

    protected final void closeWindow() {
        mainWindow.setShouldClose(true);
    }

    private void loopOnce() {
        try (Timer.Context _unused_context = idleTimer.time()) {
            try (Timer.Context _unused_context_2 = idleTimer5s.time()) {
                if(!frameClock.sleepTillNextLogicFrame()) skippedFramesByRenderCounter.inc();
            }
        }
        try(Timer.Context ignored = videoFrameDriftTimer.time()) {
            // careful moving videoFrameDriftTimer. It should start exactly when frameClock increments in its
            // beginLogicFrame and exactly when glfwSwapBuffers is called at the end of RenderPhase#close.
            // That said we are double buffered I guess so I'm not accounting for the time between the glfwSwapBuffers
            // call and the actual screen update. We don't exactly enclose those points here but it's fine because
            // and only as long as the other operations in between are of negligible time. I did check those BTW
            // to verify that and you should too if you change this code region, or else suck it up and manually
            // mark the videoFrameDriftTimer
            try (Timer.Context _unused_context = fullLogicTimer.time()) {
                boolean skippingFrames = false;
                do {
                    if (skippingFrames) skippedFramesByLogicCounter.inc();
                    try (Timer.Context _unused_context_2 = logicTimer.time()) {
                        // the pause function continues logic updates because logic updates should be idempotent in the absence
                        // of user input, which can be useful. The frame clock should be checked and if duplicate frames are
                        // received, no new time-triggered events should happen. This is the responsibility of the updateLogic
                        // implementation.
                        frameClock.beginLogicFrame();
                        updateLogic();
                    }
                    skippingFrames = true;
                } while (frameClock.moreLogicFramesNeeded());
            }
            try (Timer.Context _unused_context = renderTimer.time()) {
                try (GlWindow.RenderPhase __unused_context = mainWindow.renderPhase()) {
                    updateView();
                }
            }
        }
    }

    protected final void pollEvents() {
        mainWindow.pollEvents();
    }


    // TODO composition instead of inheritance, turn the below into an interface
    protected abstract void initialize();
    protected abstract void keyPressEvent(int key, int mods);
    protected abstract void mouseClickEvent(TextureCoordinate coord, int button);
    /** updateView could be called every frame, more than once per frame, less often, etc. it must be idempotent */
    protected abstract void updateView();
    /** updateLogic will be called exactly once per logical frame, ie, once for frame 0, then once for frame 1, etc */
    protected abstract void updateLogic();
    protected abstract String getWindowTitle();
    protected abstract int getWindowHeight();
    protected abstract int getWindowWidth();
    protected abstract boolean getDebugMode(); //TODO get from cmd line property
    protected abstract void showInitialLoadingScreen();
    protected abstract void expensiveInitialize();
    public abstract void close();

}
