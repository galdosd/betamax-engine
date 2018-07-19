package com.github.galdosd.betamax.graphics;

import com.github.galdosd.betamax.opengl.TextureCoordinate;
import lombok.NonNull;
import org.lwjgl.opengl.GL11;
import org.slf4j.LoggerFactory;

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

    public static Texture simpleTexture(String filename) {
        Texture texture = new Texture(TextureImages.fromRgbaFile(filename, true, true));
        texture.bind(GL_TEXTURE_2D);
        texture.btSetParameters();
        texture.btUploadTextureUnit();
        return texture;
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
        textureImage.uploadGl(boundTarget);
    }

    public boolean isTransparentAtCoordinate(TextureCoordinate coordinate) {
        ColorSample color = textureImage.getPixel(coordinate);
        boolean transparentEnough = color.isTransparentEnough();
        LOG.trace("{}.isTransparentEnough() == {}", color, transparentEnough);
        return transparentEnough;
    }

    public void render() {
        // FIXME very leaky abstraction, bring more of the VBO and VAO stuff into Texture
        // while removing the VBO/VAO stuff from GlProgramBase
        bind(GL_TEXTURE_2D);
        glClear(GL_DEPTH_BUFFER_BIT);
        glDrawArrays(GL_TRIANGLES, 0, 3*2);
    }
}
