package com.github.galdosd.betamax.opengl;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Configuration;
import org.slf4j.LoggerFactory;

import static com.github.galdosd.betamax.OurTool.checkGlError;
import static com.google.common.base.Preconditions.checkState;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/** Deal with GLFW and represent one window (we can have many windows, though this is not tested)
 * Probably not threadsafe. Dedicate a single thread to all GLFW/OpenGL operations and if you need other threads, use
 * them for abstract expensive tasks that can ultimately be boiled down to marching orders for the GLFW/OpenGL dedicated
 * thread.
 */
public final class GlWindow implements AutoCloseable {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private static GLFWErrorCallback glfwErrorCallback;
    private static boolean debugMode;

    private final long windowHandle;
    private final int windowWidth;
    private final int windowHeight;
    private final String title;

    private boolean isDestroyed = false;
    private final boolean fullScreen;

    /** This must be called exactly once. It must be called before any GlWindow can be created */
    public static void initGlfw(boolean _debugMode) {
        debugMode = _debugMode;
        if(debugMode) {
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
    }

    /** This must be called exactly once. No GlWindows may be created after it is called. */
    public static void shutdownGlfw(){
        if(null!=glfwErrorCallback) glfwErrorCallback.free(); // IDRC if we need to do this
        glfwTerminate();
    }

    public GlWindow(int windowWidth, int windowHeight, String title,
                    GLFWKeyCallbackI keyCallback, GLFWMouseButtonCallbackI mouseButtonCallback, boolean fullScreen) {
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.title = title;
        this.fullScreen = fullScreen;
        windowHandle = createWindow();

        glfwSetKeyCallback(windowHandle, GLFWKeyCallback.create(keyCallback));
        glfwSetMouseButtonCallback(windowHandle, GLFWMouseButtonCallback.create(mouseButtonCallback));


        checkGlError();
        LOG.debug("Created and showed window {} and completed setup of OpenGL context", windowHandle);
    }

    private long createWindow() {
        glfwDefaultWindowHints(); // probably unnecessary, but whatever, fuck the police
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        // we don't want to show the window till we're done making it...
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        if(debugMode) glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);

        long monitor = fullScreen ? glfwGetPrimaryMonitor() : NULL;
        long createdWindowHandle = glfwCreateWindow(windowWidth, windowHeight, title, monitor, NULL);
        checkState(NULL != createdWindowHandle);
        if(!fullScreen) {
            centerWindow(createdWindowHandle);
        }

        glfwMakeContextCurrent(createdWindowHandle);
        glfwShowWindow(createdWindowHandle); // this has to be done before GL.createCapabilities()
        GL.createCapabilities();
        glfwSwapInterval(1); // wait for v sync (or whatever they do these days) when swapping buffers

        if(debugMode) {
            // enable opengl debugging
            GlDebugMessages.setupJavaStyleDebugMessageCallback(LOG);
            //glDisable(GL_CULL_FACE);
        }

        return createdWindowHandle;
    }

    public boolean getShouldClose() {
        checkState(!isDestroyed);
        return glfwWindowShouldClose(windowHandle);
    }

    private void centerWindow(long createdWindowHandle) {
        GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor()); // get the resolution
        glfwSetWindowPos(
                createdWindowHandle,
                (vidMode.width() - windowWidth) / 2,
                (vidMode.height() - windowHeight) / 2
        );
    }

    public void setShouldClose(boolean shouldClose) {
        checkState(!isDestroyed);
        glfwSetWindowShouldClose(windowHandle, shouldClose);
    }

    @Override public void close() {
        checkState(!isDestroyed);
        glfwDestroyWindow(windowHandle);
        glfwFreeCallbacks(windowHandle);
        isDestroyed = true;
    }

    public TextureCoordinate windowToTextureCoord(double x, double y) {
        double fieldWidth, fieldHeight;
        if(fullScreen) {
            GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor()); // get the resolution
            fieldWidth = vidMode.width();
            fieldHeight = vidMode.height();
        } else {
            fieldWidth = windowWidth;
            fieldHeight = windowHeight;
        }
        return new TextureCoordinate(x / fieldWidth, 1.0 - y / fieldHeight);
    }

    public final class RenderPhase implements AutoCloseable {
        @Override public void close() {
            checkState(!isDestroyed);
            checkGlError();
            glfwSwapBuffers(windowHandle);
        }
        private RenderPhase(){}
    }

    public GlWindow.RenderPhase renderPhase() {
        checkState(!isDestroyed);
        glfwMakeContextCurrent(windowHandle);
        glfwPollEvents();
        checkGlError();
        return new RenderPhase();
    }
}
