package com.github.galdosd.betamax;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Timer;
import com.github.galdosd.betamax.graphics.GlDebugMessages;
import com.github.galdosd.betamax.graphics.TextureCoordinate;
import lombok.AllArgsConstructor;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.*;
import org.lwjgl.system.Configuration;
import org.slf4j.LoggerFactory;

import java.nio.DoubleBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkState;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

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

    // glfw window stuff
    private GLFWErrorCallback glfwErrorCallback;
    private long windowHandle;
    private final Set<Integer> downKeys = new HashSet<>();

    // FPS metrics
    private final Timer userInitTimer = Global.metrics.timer("userInitTimer");
    private final Timer renderTimer = Global.metrics.timer("renderTimer");
    private final Timer logicTimer = Global.metrics.timer("logicTimer");
    private final ConsoleReporter reporter = ConsoleReporter.forRegistry(Global.metrics)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();

    private int frameCount = 0;

    /** Typesafe enum for GLFW_KEY_DOWN/GLFW_KEY_UP constants */
    @AllArgsConstructor
    public static enum KeyAction {
        DOWN(GLFW_PRESS), UP(GLFW_RELEASE);

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

    public void run() {
        try {
            initializeGlBase();

            while (!glfwWindowShouldClose(windowHandle)) {
                loopOnce();
            }
        } finally {
            if(0!=windowHandle) {
                glfwDestroyWindow(windowHandle);
                glfwFreeCallbacks(windowHandle);
            }
            glfwTerminate();
            if(null!=glfwErrorCallback) glfwErrorCallback.free(); // IDRC if we need to do this
        }
    }

    private void initializeGlBase() {
        if(getDebugMode()) {
            // enable glfw debugging
            Configuration.DEBUG.set(true);
            Configuration.DEBUG_LOADER.set(true);
            Configuration.DEBUG_FUNCTIONS.set(true);
            // we only currently use try-with-resources for stack memory
            // Configuration.DEBUG_STACK.set(true);
            // we don't yet use MemoryUtils
            // Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
        }

        (glfwErrorCallback = GLFWErrorCallback.createThrow()).set();
        checkState(glfwInit(), "Could not initialize GLFW");

        createWindow();
        centerWindow();

        // set input callbacks
        glfwSetKeyCallback(windowHandle, GLFWKeyCallback.create(this::keyCallback));
        glfwSetMouseButtonCallback(windowHandle, GLFWMouseButtonCallback.create(this::mouseButtonCallback));

        glfwMakeContextCurrent(windowHandle);
        glfwSwapInterval(1); // wait for v sync (or whatever they do these days) when swapping buffers

        glfwShowWindow(windowHandle);
        // FIXME we are not multithreaded but if we were i think we have to do this more often
        GL.createCapabilities();

        if(getDebugMode()) {
            // enable opengl debugging
            GlDebugMessages.setupJavaStyleDebugMessageCallback(LOG);
            //glDisable(GL_CULL_FACE);
        }

        checkGlError();
        LOG.debug("Initialized GLFW and OpenGL");

        // reporter.start(60, TimeUnit.SECONDS);

        try(Timer.Context _unused_context = userInitTimer.time()) {
            initialize();
        }
        checkGlError();
        LOG.debug("User initialization done");

    }

    private final DoubleBuffer xMousePosBuffer = BufferUtils.createDoubleBuffer(1);
    private final DoubleBuffer yMousePosBuffer = BufferUtils.createDoubleBuffer(1);

    private void mouseButtonCallback(long window, int button, int action, int mods){
        if(action == GLFW_PRESS && button == GLFW_MOUSE_BUTTON_LEFT) {
            glfwGetCursorPos(window, xMousePosBuffer, yMousePosBuffer);
            double x = xMousePosBuffer.get(0);
            double y = yMousePosBuffer.get(0);
            leftMouseClickEvent(new TextureCoordinate(x/(double)getWindowWidth(), 1.0 - y/(double)getWindowHeight()));
        }
    }

    private void createWindow() {
        glfwDefaultWindowHints(); // probably unnecessary, but whatever, fuck the police
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        // we don't want to show the window till we're done making it...
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        if(getDebugMode()) glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);

        windowHandle = glfwCreateWindow(
                getWindowWidth(), getWindowHeight(), getWindowTitle(), NULL, NULL);
        checkState(NULL != windowHandle);
    }

    private boolean paused = false;
    private void keyCallback(long window, int key, int scancode, int action, int mods) {
        // exit upon ESC key
        if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
            closeWindow();
        }
        // show FPS metrics upon pause/break key
        else if (key == GLFW_KEY_PAUSE && action == GLFW_RELEASE) {
            paused = !paused;
            // FIXME this will fuck up the metrics,we need a cooked Clock for Metrics to ignore
            // the passage of time during pause
            if(paused) reporter.report();
        }
        else if(action!=GLFW_REPEAT) {
            KeyAction keyAction = KeyAction.fromInt(action);
            if(keyAction == KeyAction.DOWN) downKeys.add(key);
            if(keyAction == KeyAction.UP) downKeys.remove(key);
            keyboardEvent(key, keyAction);
        }
    }

    private void centerWindow() {
        GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor()); // get the resolution
        glfwSetWindowPos(
                windowHandle,
                (vidMode.width() - getWindowWidth()) / 2,
                (vidMode.height() - getWindowHeight()) / 2
        );
    }

    protected final void closeWindow() {
        glfwSetWindowShouldClose(windowHandle, true);
    }

    private void loopOnce() {
        try(Timer.Context _unused_context = logicTimer.time()) {
            // XXX: the pause function is very rudimentary for debugging, so it does not
            // actually stop logic updates! they will just happen over and over!!!!!
            updateLogic();
        }
        try(Timer.Context _unused_context = renderTimer.time()) {
            updateView();
        }
        if (!paused) frameCount++;
        checkGlError();
        glfwSwapBuffers(windowHandle);
        glfwPollEvents();
        checkGlError();
    }


    protected static void checkGlError() {
        int err = glGetError();
        checkState(0 == err, "glGetError == " + err);
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
