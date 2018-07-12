package com.github.galdosd.betamax.opengl;

import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

/**
 * FIXME: Document this class
 */
public final class VAO {
    private final int handle;

    public VAO() {
        handle = glGenVertexArrays();
    }

    // TODO we can do this in a more sophisticated less verbose way but i'm holding off
    // in case we find somethign that already does this or a good reason not to
    public static void vertexAttribPointer(
            int attribLocation,
            int arity, int type, boolean normalize, int stride, long offset) {
        glVertexAttribPointer(attribLocation, arity, type, normalize, stride, offset);
        glEnableVertexAttribArray(attribLocation);
    }

    public void bind() {
        glBindVertexArray(handle);
    }
}
