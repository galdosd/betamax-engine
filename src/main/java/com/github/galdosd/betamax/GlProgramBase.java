package com.github.galdosd.betamax;

import lombok.AllArgsConstructor;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * FIXME: Document this class
 */
public abstract class GlProgramBase {
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

        initialize();

    }

    private void createWindow() {
        glfwDefaultWindowHints(); // probably unnecessary, but whatever, fuck the police
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        // we don't want to show the window till we're done making it...
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);

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

    protected abstract void initialize();
    protected abstract void keyboardEvent(int key, KeyAction action);
    protected abstract void updateView();
    protected abstract void updateLogic();
    protected abstract String getWindowTitle();
    protected abstract int getWindowHeight();
    protected abstract int getWindowWidth();
}
