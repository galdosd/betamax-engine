package com.github.galdosd.betamax;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.*;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

import static com.google.common.base.Preconditions.checkState;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * FIXME: Document this class
 * FIXME: break up this class
 */
public abstract class GlProgramBase {
    //TODO all GL wrapped arguments passed from subclass/users of GlprogramBase should be typesafe, not
    //some fucking ints. fuck you C people.
    //TODO make a GL low level wrapper class that calls each glXYZ method but don't care which GL version and
    // includes the getGlError check. or use some stupid AOP. probably not though, that's too obtuse
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    // we save the callbacks so they won't be garbage collected early -- GLFW uses weak references?
    private GLFWKeyCallback keyCallback;
    private GLFWErrorCallback errorCallback;
    private long windowHandle;
    Set<Integer> downKeys = new HashSet<>();

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
            initializeGLDemoBase();
            LOG.info("Initialized GLFW and OpenGL");

            // loop forever until the user closes the window
            while (glfwWindowShouldClose(windowHandle) == GLFW_FALSE) {
                loopOnce();
            }

            glfwDestroyWindow(windowHandle);
            keyCallback.release();
        } finally {
            glfwTerminate();
            errorCallback.release();
        }
    }

    private void initializeGLDemoBase() {
        glfwSetErrorCallback(errorCallback =
                GLFWErrorCallback.create((errno, msg) -> LOG.error("GLFW Error {} ({})")));
        checkState(glfwInit() == GLFW_TRUE, "Could not initialize GLFW");
        createWindow();

        // set input callbacks
        glfwSetKeyCallback(windowHandle, keyCallback = GLFWKeyCallback.create(this::keyCallback));

        centerWindow();

        glfwMakeContextCurrent(windowHandle);
        glfwSwapInterval(1); // wait for v sync (or whatever they do these days) when swapping buffers

        glfwShowWindow(windowHandle);
        GL.createCapabilities();

        if(getDebugMode()) {
            enableDebugMode();
        }

        initialize();

    }

    private void createWindow() {
        glfwDefaultWindowHints(); // probably unnecessary, but whatever, fuck the police
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        // we don't want to show the window till we're done making it...
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
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
        glfwSetWindowShouldClose(windowHandle, GLFW_TRUE);
    }

    private void loopOnce() {
        // TODO maintain and report FPS
        updateLogic();
        updateView();
        glfwSwapBuffers(windowHandle);
        glfwPollEvents();
    }

    /** Typesafe wrapper for Vertex Buffer Object handle instead of a damned int */
    protected static final class VBO {
        private final int handle;

        public VBO() {
            handle = glGenBuffers();        checkGlError();
        }

        public void bind(int target) {
            glBindBuffer(target, handle);   checkGlError();
        }

        public void bindAndLoad(int target, int usage, float[] data) {
            bind(target);
            glBufferData(target, FloatBuffer.wrap(data), usage);      checkGlError();
        }
    }

    protected static final void checkGlError() {
        int err = glGetError();
        checkState(0 == err, "glGetError == " + err);
    }

    protected static final class VAO {
        private final int handle;
        public VAO() {
            handle = glGenVertexArrays();       checkGlError();
        }
        public void bind() {
            glBindVertexArray(handle);          checkGlError();
        }
    }

    @Value protected static final class Shader {
        int handle;
    }

    protected static final class ShaderProgram {
        private final int handle;

        public ShaderProgram() {
            handle = glCreateProgram();         checkGlError();
        }

        public void attach(Shader shader) {
            glAttachShader(handle, shader.getHandle());     checkGlError();
        }

        public void linkAndUse() {
            glLinkProgram(handle);      checkGlError();
            int linkStatus = glGetProgrami(handle, GL_LINK_STATUS);
            checkState(GL_TRUE == linkStatus, "glLinkProgram failed: " + glGetProgramInfoLog(handle));
            checkGlError();
            glUseProgram(handle);       checkGlError();
        }

        private int getAttribLocation(String varName) {
            int result = glGetAttribLocation(handle, varName);      checkGlError();
            return result;
        }

        public void bindFragDataLocation(int colorNumber, String colorName) {
            glBindFragDataLocation(handle, colorNumber, colorName);
        }
    }

    protected final void enableDebugMode() {
        // install GL debug message callbacks

        // FIXME we should IllegalStateException on a sufficiently severe error
        glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS);      checkGlError();
        GLDebugMessageCallback.SAM msgCallback =
                (int source, int type, int id, int severity, int length, long message, long _ignored) -> {

                    String msg = "" + GLDebugMessageCallback.getMessage(length, message);

                    // yes this is gross. method references... TODO. whatever.
                    // TODO translate source and type to constants also
                    String logMsg = "GL (src{} ty{} id{}): {} ";
                    if(severity == GL_DEBUG_SEVERITY_HIGH)
                        LOG.error(logMsg, source, type, id, msg);
                    if(severity == GL_DEBUG_SEVERITY_MEDIUM)
                        LOG.warn(logMsg, source, type, id, msg);
                    if(severity == GL_DEBUG_SEVERITY_LOW)
                        LOG.info(logMsg, source, type, id, msg);
                    if(severity == GL_DEBUG_SEVERITY_NOTIFICATION)
                        LOG.debug(logMsg, source, type, id, msg);
        };
        glDebugMessageCallback(GLDebugMessageCallback.create(msgCallback), 0);              checkGlError();
        glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DONT_CARE, 0, null, true);     checkGlError();

        //glDisable(GL_CULL_FACE);
    }

    protected final void vertexAttribPointer(
            ShaderProgram shaderProgram, String varName,
            int arity, int type, boolean normalize, int stride, long offset) {
        int attribLocation = shaderProgram.getAttribLocation(varName);
        glEnableVertexAttribArray(attribLocation);                                          checkGlError();
        glVertexAttribPointer(attribLocation, arity, type, normalize, stride, offset);      checkGlError();
    }

    protected final Shader loadAndCompileShader(String filename, int shaderType)  {
        String shaderSource = OurTool.loadResource(filename);
        int shader = glCreateShader(shaderType);                    checkGlError();
        glShaderSource(shader, shaderSource);                       checkGlError();
        glCompileShader(shader);                                    checkGlError();

        int status = glGetShaderi(shader, GL_COMPILE_STATUS);       checkGlError();
        checkState(GL_TRUE == status, "Shader compilation failure: %s", filename);
        return new Shader(shader);
    }

    protected abstract void initialize();
    protected abstract void keyboardEvent(int key, KeyAction action);
    protected abstract void updateView();
    protected abstract void updateLogic();
    protected abstract String getWindowTitle();
    protected abstract int getWindowHeight();
    protected abstract int getWindowWidth();
    protected abstract boolean getDebugMode();
}
