package com.github.galdosd.betamax.graphics;

import lombok.NonNull;
import org.lwjgl.opengl.GL11;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static com.google.common.base.Preconditions.checkArgument;
import static org.lwjgl.opengl.GL11.*;

/**
 * FIXME: Document this class
 * For methods beginning with name of "bt" bind() must be called first
 */
public final class Texture {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private final int handle;
    public final TextureImage textureImage;
    private int boundTarget = 0;
    // FIXME unload from VRAM when done

    public Texture(@NonNull TextureImage textureImage) {
        handle = GL11.glGenTextures();
        this.textureImage = textureImage;
    }

    public void bind(int target) {
        glBindTexture(target, handle);
        boundTarget = target;
    }

    public void btSetParameters() {
        rebind();
        // glTexParameteri(target, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        // glTexParameteri(target, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        // FIXME will this segfault?
        // glTexParameterfv(boundTarget, GL_TEXTURE_BORDER_COLOR, new float[]{1.0f, 0.5f, 0.8f, 0.5f});

        glTexParameteri(boundTarget, GL11.GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(boundTarget, GL11.GL_TEXTURE_WRAP_T, GL_REPEAT);

        glTexParameteri(boundTarget, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        glTexParameteri(boundTarget, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    }

    private void rebind() {
        // TODO use glGet to ensure this really is the bound object. thanks opengl.
        checkArgument(0!=boundTarget, "call bind() to set GL target first");
        bind(boundTarget);
    }

    public void btUploadTextureUnit() {
        rebind();
        ByteBuffer bytePixelData = textureImage.getBytePixelData();
        bytePixelData.position(0);
        glTexImage2D( // TODO add a metrics timer here
                // TODO GL_INT or something would be more precise maybe but i couldn't get it to work
                boundTarget, 0, GL_RGBA, textureImage.getWidth(), textureImage.getHeight(), 0,
                GL_RGBA, GL_FLOAT, bytePixelData
        );
    }

    public boolean isTransparentAtCoordinate(TextureCoordinate coordinate) {
        ColorSample color = textureImage.getPixel(coordinate);
        boolean transparentEnough = color.isTransparentEnough();
        LOG.trace("{}.isTransparentEnough() == {}", color, transparentEnough);
        return transparentEnough;
    }

}
