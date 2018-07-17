package com.github.galdosd.betamax.opengl;

import static com.google.common.base.Preconditions.checkState;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindFragDataLocation;

/**
 * FIXME: Document this class
 */
public final class ShaderProgram {
    private final int handle;
    boolean linked = false;

    public ShaderProgram() {
        handle = glCreateProgram();
    }

    public void attach(Shader shader) {
        checkState(!linked);
        glAttachShader(handle, shader.getHandle());
    }

    public void linkAndUse() {
        link();
        use();
    }

    public void use() {
        checkState(linked);
        glUseProgram(handle);
    }

//    private int getAttribLocation(String varName) {
//        int result = glGetAttribLocation(handle, varName);
//        checkState(-1 != result);
//        return result;
//    }
//
//    public void bindFragDataLocation(int colorNumber, String colorName) {
//        glBindFragDataLocation(handle, colorNumber, colorName);
//    }

    public void link() {
        checkState(!linked);
        glLinkProgram(handle);
        int linkStatus = glGetProgrami(handle, GL_LINK_STATUS);
        checkState(GL_TRUE == linkStatus, "glLinkProgram failed: " + glGetProgramInfoLog(handle));
        linked = true;
    }
}
