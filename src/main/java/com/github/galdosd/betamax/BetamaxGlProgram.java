package com.github.galdosd.betamax;


import com.codahale.metrics.Timer;
import com.github.galdosd.betamax.sprite.Sprite;
import com.github.galdosd.betamax.sprite.SpriteRegistry;
import com.github.galdosd.betamax.sprite.SpriteTemplate;

import static com.google.common.base.Preconditions.checkState;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

/**
 * FIXME: Document this class
 */
public class BetamaxGlProgram extends GlProgramBase {
    Timer textureLoadTimer = Global.metrics.timer("BetamaxGlProgram.textureLoadtimer");
    Timer textureUploadTimer = Global.metrics.timer("BetamaxGlProgram.textureUploadtimer");

    Texture checkerboardTexture;
    ScriptWorld scriptWorld;
    SpriteRegistry spriteRegistry = new SpriteRegistry(getFrameClock());

    public static void main(String[] args) {
        new BetamaxGlProgram().run();
    }

    @Override protected void initialize() {

        // load our triangle vertices
        VBO vbo = new VBO();
        float textureScale = 2;
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

        // prepare shaders
        ShaderProgram shaderProgram = new ShaderProgram();
        shaderProgram.attach(
                loadAndCompileShader("default.vert", GL_VERTEX_SHADER));
        shaderProgram.attach(
                loadAndCompileShader("default.frag", GL_FRAGMENT_SHADER));
        //shaderProgram.bindFragDataLocation(0, "outColor"); //unnecessary, we only have one output color
        shaderProgram.linkAndUse();

        // prepare vao
        VAO vao = new VAO();
        vao.bind();
        vbo.bind(GL_ARRAY_BUFFER);
        vertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        vertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);

        // enable transparency
        glEnable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glBlendFunc( GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        scriptWorld = new ScriptWorld("example.py", spriteRegistry);
        scriptWorld.onBegin();
    }


    @Override protected void keyboardEvent(int key, KeyAction action) { }


    /** cycle background color; can be helpful in debugging */
    private int colorcycler = 0;
    @Override protected void updateView() {
        glClearColor(((float)Math.sin(colorcycler*0.05f) + 1)*0.5f, 0.8f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        spriteRegistry.renderAll();
    }

    @Override protected void updateLogic() {
        colorcycler++;
        spriteRegistry.dispatchEvents(scriptWorld);
    }

    @Override protected String getWindowTitle() { return "BETAMAX DEMO"; }
    @Override protected int getWindowHeight() { return 540; }
    @Override protected int getWindowWidth() { return 960; }
    @Override protected boolean getDebugMode() { return true; }
}
