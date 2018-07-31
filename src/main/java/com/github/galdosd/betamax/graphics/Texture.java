package com.github.galdosd.betamax.graphics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.imageio.ColorSample;
import com.github.galdosd.betamax.opengl.FramebufferCoordinate;
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
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;

/** A texture stored in opengl. But it can load and unload itself into and out of VRAM on demand transparently
 * For methods beginning with name of "bt" bind() must be called first
 */
public final class Texture implements  AutoCloseable {
    private static final Counter rendertimeUploadsCounter = Global.metrics.counter("rendertimeUploads");
    private static final Timer jitMouseTextureLoadTimer = Global.metrics.timer("jitMousetextureLoadTimer");
    private static final Counter vramImageBytesCounter = Global.metrics.counter("vramImageBytes");
    private static final Counter vramTexturesCounter = Global.metrics.counter("vramTextures");
    private static final Counter virtualTexturesCounter = Global.metrics.counter("virtualTextures");

    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private int handle = -1;
    private boolean vramLoaded = false;
    public final LazyTextureImage textureImage;
    private int boundTarget = 0;

    private Texture(@NonNull LazyTextureImage textureImage) {
        this.textureImage = textureImage;
        virtualTexturesCounter.inc();
    }

    public TextureName getName() {
        return textureImage.getName();
    }

    public static Texture simpleTexture(TextureName textureName, boolean preloaded) {
        Texture texture = new Texture(new LazyTextureImage(textureName));
        if(preloaded) {
            texture.setRamLoaded(true);
            //texture.setVramLoaded(true);
        }
        return texture;
    }

    void setRamLoaded(boolean ramLoaded) {
        textureImage.setLoaded(ramLoaded);
    }

    boolean getRamLoaded() {
        return textureImage.getLoaded();
    }

    private void bind(int target) {
        checkState(getVramLoaded());
        checkState(handle>0);
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
    }

    // because BetamaxGlProgram#loopOnce called RenderPhase#close (which calls glfwPollEvents) in the same frame
    // this should check against the same frame textures we JUST rendered, so we should
    // never end up JIT loading just for a damn collision check
    public boolean isTransparentAtCoordinate(TextureCoordinate coordinate) {
        if(!textureImage.getLoaded()) {
            try(Timer.Context ignored = jitMouseTextureLoadTimer.time()) {
                LOG.warn("Had to load texture just to process mouse click check: {}", this);
                textureImage.setLoaded(true);
            }
        }
        ColorSample color = textureImage.getPixel(coordinate);
        boolean transparentEnough = color.isTransparentEnough();
        LOG.trace("{}.isTransparentEnough() == {}", color, transparentEnough);
        return transparentEnough;
    }

    public void render(TextureCoordinate location) {
        checkState(null != vbo && null != vao);
        if (!getVramLoaded()) {
            LOG.warn("Uploading texture to VRAM at rendertime: {}", textureImage);
            rendertimeUploadsCounter.inc();
        }
        setVramLoaded(true);
        bind(GL_TEXTURE_2D);
        vao.bind();
        FramebufferCoordinate fbCoord = location.toFramebufferCoordinate();

        float ox = fbCoord.getX();
        float oy = fbCoord.getY();
        // FIXME now we log like crazy through GlDebugMessages because of the below bindAndLoad
        // or wait maybe it is the vao.bind()
        // and it only happens on windows i think or maybe it is something else in the mute big sprite asset set
        vbo.bindAndLoad(GL_ARRAY_BUFFER, GL_DYNAMIC_DRAW, new float[]{
            // two right triangles that cover the full screen
            // all our sprites are fullscreen! wow!
               //xpos   ypos      xtex  ytex
                -1.0f+ox,  1.0f+oy,     0.0f, 1.0f,
                 1.0f+ox,  1.0f+oy,     1.0f, 1.0f,
                -1.0f+ox, -1.0f+oy,     0.0f, 0.0f,

                 1.0f+ox,  1.0f+oy,     1.0f, 1.0f,
                 1.0f+ox, -1.0f+oy,     1.0f, 0.0f,
                -1.0f+ox, -1.0f+oy,     0.0f, 0.0f,
        });
        glClear(GL_DEPTH_BUFFER_BIT);
        glDrawArrays(GL_TRIANGLES, 0, 3 /*three points in a triangle */ * 2 /* two triangles */);
    }

    private static VBO vbo;
    private static VAO vao;

    public static void prepareForDrawing() {
        vbo = new VBO();
        vao = new VAO();
        vao.bind();
        vbo.bind(GL_ARRAY_BUFFER);
        VAO.vertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        VAO.vertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
    }

    @Override public void close() {
        setVramLoaded(false);
        textureImage.close();
        virtualTexturesCounter.dec();
    }

    public void setVramLoaded(boolean vramLoaded) {
        if(this.vramLoaded==vramLoaded) return;
        // TODO if an exception causes this to fail the object will be in a bad state where it can't be closed either
        // whatever.
        this.vramLoaded = vramLoaded;
        if(!vramLoaded) vramUnload();
        if(vramLoaded) vramLoad();
    }

    private void vramLoad() {
        checkState(handle==-1);
        handle = GL11.glGenTextures();
        checkState(handle>0);
        bind(GL_TEXTURE_2D);
        btSetParameters();
        btUploadTextureUnit();
        vramTexturesCounter.inc();
        vramImageBytesCounter.inc(textureImage.getByteCount());
    }

    private void vramUnload() {
        checkState(handle>0);
        GL11.glDeleteTextures(handle);
        handle = -1;
        vramTexturesCounter.dec();
        vramImageBytesCounter.dec(textureImage.getByteCount());
    }

    public boolean getVramLoaded() {
        return vramLoaded;
    }
}
