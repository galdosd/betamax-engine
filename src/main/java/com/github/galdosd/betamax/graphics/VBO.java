package com.github.galdosd.betamax.graphics;

import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.system.MemoryStack.stackPush;

/** Typesafe wrapper for Vertex Buffer Object handle instead of a damned int */
public final class VBO {
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
