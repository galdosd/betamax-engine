package com.github.galdosd.betamax.graphics;

import com.codahale.metrics.Counter;
import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.opengl.TextureCoordinate;
import com.github.galdosd.betamax.opengl.VAO;
import com.github.galdosd.betamax.opengl.VBO;
import lombok.NonNull;
import org.lwjgl.opengl.GL11;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;

/**
 * FIXME: Document this class
 * For methods beginning with name of "bt" bind() must be called first
 */
public final class Texture implements  AutoCloseable {
    private static final Counter vramImageBytesCounter = Global.metrics.counter("vramImageBytes");
    private static final Counter texturesCounter = Global.metrics.counter("textures");

    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private final int handle;
    public final TextureImage textureImage;
    private int boundTarget = 0;
    // FIXME unload from VRAM when done

    private Texture(@NonNull TextureImage textureImage) {
        handle = GL11.glGenTextures();
        this.textureImage = textureImage;
        texturesCounter.inc();
    }

    public static Texture simpleTexture(String filename) {
        Texture texture = new Texture(TextureImages.fromRgbaFile(filename, true, true));
        texture.bind(GL_TEXTURE_2D);
        texture.btSetParameters();
        texture.btUploadTextureUnit();
        return texture;
    }

    private void bind(int target) {
        glBindTexture(target, handle);
        boundTarget = target;
    }

    private void btSetParameters() {
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

    private void btUploadTextureUnit() {
        rebind();
        textureImage.uploadGl(boundTarget);
        vramImageBytesCounter.inc(textureImage.getBytePixelData().capacity());
    }

    public boolean isTransparentAtCoordinate(TextureCoordinate coordinate) {
        ColorSample color = textureImage.getPixel(coordinate);
        boolean transparentEnough = color.isTransparentEnough();
        LOG.trace("{}.isTransparentEnough() == {}", color, transparentEnough);
        return transparentEnough;
    }

    public void render() {
        checkState(null!=vbo && null!=vao);
        vao.bind();
        vbo.bind(GL_ARRAY_BUFFER);
        bind(GL_TEXTURE_2D);
        glClear(GL_DEPTH_BUFFER_BIT);
        glDrawArrays(GL_TRIANGLES, 0, 3*2);
    }

    private static VBO vbo;
    private static VAO vao;

    // FIXME this should be paired with SpriteTemplate#renderTemplate, there is indirect dependency between them
    // via opengl VBO/VAO handles
    public static void prepareForDrawing() {
        // load our triangle vertices
        vbo = new VBO();
        vbo.bindAndLoad(GL_ARRAY_BUFFER, GL_STATIC_DRAW, new float[]{
            // two right triangles that cover the full screen
            // all our sprites are fullscreen! wow!
               //xpos   ypos      xtex  ytex
                -1.0f,  1.0f,     0.0f, 1.0f,
                 1.0f,  1.0f,     1.0f, 1.0f,
                -1.0f, -1.0f,     0.0f, 0.0f,

                 1.0f,  1.0f,     1.0f, 1.0f,
                 1.0f, -1.0f,     1.0f, 0.0f,
                -1.0f, -1.0f,     0.0f, 0.0f,
        });

        // prepare vao
        vao = new VAO();
        vao.bind();
        vbo.bind(GL_ARRAY_BUFFER);
        VAO.vertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        VAO.vertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
    }

    @Override public void close() {
        textureImage.close();
        GL11.glDeleteTextures(handle);
        texturesCounter.dec();
        vramImageBytesCounter.dec(textureImage.getBytePixelData().capacity());
    }

    public void ensureUploaded() { }
}
