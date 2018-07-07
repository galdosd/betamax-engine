package com.github.galdosd.betamax;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Timer;
import com.github.galdosd.betamax.graphics.GlWindow;
import com.github.galdosd.betamax.graphics.TextureCoordinate;
import lombok.AllArgsConstructor;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.slf4j.LoggerFactory;

import java.nio.DoubleBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
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
    private boolean paused = false;
    private int frameCount = 0;

    // input management
    private final Set<Integer> downKeys = new HashSet<>();
    private final DoubleBuffer xMousePosBuffer = BufferUtils.createDoubleBuffer(1);
    private final DoubleBuffer yMousePosBuffer = BufferUtils.createDoubleBuffer(1);

    // FPS metrics
    private final Timer userInitTimer = Global.metrics.timer("userInitTimer");
    private final Timer renderTimer = Global.metrics.timer("renderTimer");
    private final Timer logicTimer = Global.metrics.timer("logicTimer");
    private final ConsoleReporter reporter = ConsoleReporter.forRegistry(Global.metrics)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();

    /** Typesafe enum for GLFW_KEY_DOWN/GLFW_KEY_UP constants */
    @AllArgsConstructor
    public static enum KeyAction {
        DOWN(GLFW.GLFW_PRESS), UP(GLFW.GLFW_RELEASE);

        public final int glfwConstant;

        public static KeyAction fromInt(int code) {
            return Arrays.stream(KeyAction.values())
                    .filter(action -> action.glfwConstant == code)
                    .findFirst().get();
        }
    }

    protected FrameClock getFrameClock() {
        return () -> frameCount;
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

    private void keyCallback(long window, int key, int scancode, int action, int mods) {
        // exit upon ESC key
        if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE) {
            closeWindow();
        }
        // show FPS metrics upon pause/break key
        else if (key == GLFW.GLFW_KEY_PAUSE && action == GLFW.GLFW_RELEASE) {
            paused = !paused;
            // FIXME this will fuck up the metrics,we need a cooked Clock for Metrics to ignore
            // the passage of time during pause
            if(paused) reporter.report();
        }
        else if(action!=GLFW.GLFW_REPEAT) {
            KeyAction keyAction = KeyAction.fromInt(action);
            if(keyAction == KeyAction.DOWN) downKeys.add(key);
            if(keyAction == KeyAction.UP) downKeys.remove(key);
            keyboardEvent(key, keyAction);
        }
    }

    protected final void closeWindow() {
        mainWindow.setShouldClose(true);
    }

    private void loopOnce() {
        try(Timer.Context _unused_context = logicTimer.time()) {
            // XXX: the pause function is very rudimentary for debugging, so it does not
            // actually stop logic updates! they will just happen over and over!!!!!
            updateLogic();
        }
        try(Timer.Context _unused_context = renderTimer.time()) {
            try (GlWindow.RenderPhase __unused_context = mainWindow.renderPhase()) {
                updateView();
            }
        }
        if (!paused) frameCount++;
    }

    // TODO composition instead of inheritance, turn the below into an interface
    protected abstract void initialize();
    protected abstract void keyboardEvent(int key, KeyAction action);
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
