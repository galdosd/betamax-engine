package com.github.galdosd.betamax;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Timer;
import com.github.galdosd.betamax.opengl.GlWindow;
import com.github.galdosd.betamax.graphics.TextureCoordinate;
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
public abstract class GlProgramBase {
    //TODO all GL wrapped arguments passed from subclass/users of GlprogramBase should be typesafe, not
    //some fucking ints. fuck you C people.
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private GlWindow mainWindow;
    private final GameLoopFrameClock frameClock = new GameLoopFrameClock();

    // input management
    private final DoubleBuffer xMousePosBuffer = BufferUtils.createDoubleBuffer(1);
    private final DoubleBuffer yMousePosBuffer = BufferUtils.createDoubleBuffer(1);

    // FPS performance metrics
    private final Timer userInitTimer = Global.metrics.timer("userInitTimer");
    private final Timer renderTimer = Global.metrics.timer("renderTimer");
    private final Timer logicTimer = Global.metrics.timer("logicTimer");
    private final Timer idleTimer = Global.metrics.timer("idleTimer");
    private final ConsoleReporter reporter = ConsoleReporter.forRegistry(Global.metrics)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();


    protected final FrameClock getFrameClock() {
        return frameClock;
    }

    public final void run() {
        try {
            GlWindow.initGlfw(getDebugMode());
            try(GlWindow _mainWindow = new GlWindow(getWindowWidth(), getWindowHeight(), getWindowTitle(),
                    this::keyCallback, this::mouseButtonCallback)){
                mainWindow = _mainWindow;
                try (Timer.Context _unused_context = userInitTimer.time()) {
                    initialize();
                    checkGlError();
                    LOG.debug("User initialization done");
                }

                frameClock.resetLogicFrames();
                while (!mainWindow.getShouldClose()) {
                    loopOnce();
                }
            }
        } finally {
            GlWindow.shutdownGlfw();
        }
    }

    private void mouseButtonCallback(long window, int button, int action, int mods){
        if(action == GLFW.GLFW_PRESS && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            glfwGetCursorPos(window, xMousePosBuffer, yMousePosBuffer);
            double x = xMousePosBuffer.get(0);
            double y = yMousePosBuffer.get(0);
            leftMouseClickEvent(new TextureCoordinate(x/(double)getWindowWidth(), 1.0 - y/(double)getWindowHeight()));
        }
    }

    // TODO break out input handling
    private void keyCallback(long window, int key, int scancode, int action, int mods) {
        if(action == GLFW.GLFW_PRESS) {
            // exit upon ESC key
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                closeWindow();
            }
            // show FPS metrics upon pause/break key
            else if(key == GLFW.GLFW_KEY_PRINT_SCREEN) {
               reporter.report();
            }
            else if (key == GLFW.GLFW_KEY_PAUSE) {
                frameClock.setPaused(!frameClock.getPaused());
                // FIXME this will fuck up the metrics,we need a cooked Clock for Metrics to ignore
                // the passage of time during pause
            // page up/down to change target FPS
            } else if (key == GLFW.GLFW_KEY_PAGE_UP) {
                frameClock.setTargetFps(frameClock.getTargetFps()+1);
            } else if (key == GLFW.GLFW_KEY_PAGE_DOWN) {
                if (frameClock.getTargetFps() > 1) {
                    frameClock.setTargetFps(frameClock.getTargetFps()-1);
                }
            } else {
                keyPressEvent(key);
            }
        }
    }

    protected final void closeWindow() {
        mainWindow.setShouldClose(true);
    }

    private void loopOnce() {
        try (Timer.Context _unused_context = idleTimer.time()) {
            frameClock.sleepTillNextLogicFrame();
        }
        do {
            try (Timer.Context _unused_context = logicTimer.time()) {
                // the pause function continues logic updates because logic updates should be idempotent in the absence
                // of user input, which can be useful. The frame clock should be checked and if duplicate frames are
                // received, no new time-triggered events should happen. This is the responsibility of the updateLogic
                // implementation.
                frameClock.beginLogicFrame();
                updateLogic();
            }
        } while(frameClock.moreLogicFramesNeeded());
        try (Timer.Context _unused_context = renderTimer.time()) {
            try (GlWindow.RenderPhase __unused_context = mainWindow.renderPhase()) {
                updateView();
            }
        }
    }


    // TODO composition instead of inheritance, turn the below into an interface
    protected abstract void initialize();
    protected abstract void keyPressEvent(int key);
    protected abstract void leftMouseClickEvent(TextureCoordinate coord);
    /** updateView could be called every frame, more than once per frame, less often, etc. it must be idempotent */
    protected abstract void updateView();
    /** updateLogic will be called exactly once per logical frame, ie, once for frame 0, then once for frame 1, etc */
    protected abstract void updateLogic();
    protected abstract String getWindowTitle();
    protected abstract int getWindowHeight();
    protected abstract int getWindowWidth();
    protected abstract boolean getDebugMode(); //TODO get from cmd line property

}
