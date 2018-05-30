package com.github.galdosd.betamax;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Timer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.*;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkState;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * FIXME: Document this class
 * FIXME: break up this class
 * TODO: for safety in debug mode use glGet to make sure the right target is bound for buffers,
 *  textures, etc. basically try as hard as possible to fight the dumbass statefullness of GL calls
 *  that require params to be "bound"
 */
public abstract class GlProgramBase {
    //TODO all GL wrapped arguments passed from subclass/users of GlprogramBase should be typesafe, not
    //some fucking ints. fuck you C people.
    //TODO make a GL low level wrapper class that calls each glXYZ method but don't care which GL version and
    // includes the getGlError check. or use some stupid AOP. probably not though, that's too obtuse
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


    // framecount management
    @Getter private long frameCount = 0;

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

    public void run() {
        try {
            initializeGlBase();

            // loop forever until the user closes the window
            while (!glfwWindowShouldClose(windowHandle)) {
                loopOnce();
            }

        } finally {
            if(0!=windowHandle) {
                glfwDestroyWindow(windowHandle);
                glfwFreeCallbacks(windowHandle);
            }
            glfwTerminate();
            if(null!=glfwErrorCallback) glfwErrorCallback.free();

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

    private void keyCallback(long window, int key, int scancode, int action, int mods) {
        // exit upon ESC key
        if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
            closeWindow();
        }
        // show FPS metrics upon pause/break key
        else if (key == GLFW_KEY_PAUSE && action == GLFW_RELEASE) {
            reporter.report();
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
            updateLogic();
        }
        try(Timer.Context _unused_context = renderTimer.time()) {
            updateView();
        }
        checkGlError();
        glfwSwapBuffers(windowHandle);
        glfwPollEvents();
        checkGlError();
        frameCount++;
    }

    /** Typesafe wrapper for Vertex Buffer Object handle instead of a damned int */
    protected static final class VBO {
        private final int handle;

        public VBO() {
            handle = glGenBuffers();
        }

        public void bind(int target) {
            glBindBuffer(target, handle);
        }

        public void bindAndLoad(int target, int usage, float[] data) {
            bind(target);
            try(MemoryStack stack = stackPush()) {
                FloatBuffer floatBuffer = stack.callocFloat(data.length);
                floatBuffer.put(data);
                floatBuffer.flip();
                glBufferData(target, floatBuffer, usage);
            }
        }
    }

    protected static final class VAO {
        private final int handle;
        public VAO() {
            handle = glGenVertexArrays();
        }
        public void bind() {
            glBindVertexArray(handle);
        }
    }

    @Value protected static final class Shader {
        int handle;
    }

    protected static final class ShaderProgram {
        private final int handle;

        public ShaderProgram() {
            handle = glCreateProgram();
        }

        public void attach(Shader shader) {
            glAttachShader(handle, shader.getHandle());
        }

        public void linkAndUse() {
            glLinkProgram(handle);
            int linkStatus = glGetProgrami(handle, GL_LINK_STATUS);
            checkState(GL_TRUE == linkStatus, "glLinkProgram failed: " + glGetProgramInfoLog(handle));
            glUseProgram(handle);
        }

        private int getAttribLocation(String varName) {
            int result = glGetAttribLocation(handle, varName);
            checkState(-1 != result);
            return result;
        }

        public void bindFragDataLocation(int colorNumber, String colorName) {
            glBindFragDataLocation(handle, colorNumber, colorName);
        }
    }


    // TODO we can do this in a more sophisticated less verbose way but i'm holding off
    // in case we find somethign that already does this or a good reason not to
    protected final void vertexAttribPointer(
            int attribLocation,
            int arity, int type, boolean normalize, int stride, long offset) {
        glVertexAttribPointer(attribLocation, arity, type, normalize, stride, offset);
        glEnableVertexAttribArray(attribLocation);
    }

    protected final Shader loadAndCompileShader(String filename, int shaderType)  {
        String shaderSource = OurTool.loadResource(filename);
        int shader = glCreateShader(shaderType);
        glShaderSource(shader, shaderSource);
        glCompileShader(shader);

        int status = glGetShaderi(shader, GL_COMPILE_STATUS);
        checkState(GL_TRUE == status, "Shader compilation failure: %s", filename);
        return new Shader(shader);
    }

    protected static final void checkGlError() {
        int err = glGetError();
        checkState(0 == err, "glGetError == " + err);
    }

    // TODO composition instead of inheritance, turn the below into an interface
    protected abstract void initialize();
    protected abstract void keyboardEvent(int key, KeyAction action);
    protected abstract void updateView();
    protected abstract void updateLogic();
    protected abstract String getWindowTitle();
    protected abstract int getWindowHeight();
    protected abstract int getWindowWidth();
    protected abstract boolean getDebugMode(); //TODO get from cmd line property
}
