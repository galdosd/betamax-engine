package com.github.galdosd.betamax;

import static com.github.galdosd.betamax.GlProgramBase.checkGlError;
import static com.google.common.base.Preconditions.checkArgument;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * FIXME: Document this class
 * For methods beginning with name of "bt" bind() must be called first
 */
public final class Texture {
    final int handle;
    int boundTarget = 0;
    public Texture() {
        handle = glGenTextures();
    }

    public void bind(int target) {
        glBindTexture(target, handle);
        boundTarget = target;
    }

    public void btSetParameters() {
        rebind();
        // glTexParameteri(target, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        // glTexParameteri(target, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);

        glTexParameteri(boundTarget, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(boundTarget, GL_TEXTURE_WRAP_T, GL_REPEAT);
        // FIXME will this segfault?
        glTexParameterfv(boundTarget, GL_TEXTURE_BORDER_COLOR, new float[]{1.0f, 0.5f, 0.8f, 0.5f});

        glTexParameteri(boundTarget, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(boundTarget, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        checkGlError();
    }

    private void rebind() {
        // TODO use glGet to ensure this really is the bound object. thanks opengl.
        checkArgument(0!=boundTarget, "call bind() to set GL target first");
        bind(boundTarget);
    }

    public void btLoadRgba(float[] texturePixels, int width, int height) {
        rebind();
        // FIXME this will segfault too right?
        glTexImage2D(boundTarget, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_FLOAT, texturePixels);
        glGenerateMipmap(boundTarget);
    }
}
