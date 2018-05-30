package com.github.galdosd.betamax;


import com.codahale.metrics.Timer;

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

    public static void main(String[] args) {
        new BetamaxGlProgram().run();
    }

    @Override protected void initialize() {

        // load our triangle vertices
        VBO vbo = new VBO();
        float textureScale = 2;
        vbo.bindAndLoad(GL_ARRAY_BUFFER, GL_STATIC_DRAW, new float[]{
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
        // textures

        float[] texturePixels = new float[] {
                1.0f, 0.0f, 0.0f, 1.0f,     0.0f, 0.0f, 1.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,     1.0f, 0.0f, 0.0f, 1.0f,
        };

        glEnable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glBlendFunc( GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);


        // load textures

        checkerboardTexture = new Texture();
        checkerboardTexture.bind(GL_TEXTURE_2D);
        checkerboardTexture.btSetParameters();
        //checkerboardTexture.btLoadRgba(texturePixels, 2, 2);
        try(Timer.Context _unused = textureLoadTimer.time()) {
            checkerboardTexture.loadAlphaTiff("sprite0.tif");
        }


    }


    @Override protected void keyboardEvent(int key, KeyAction action) { }

    int colorcycler = 0;
    @Override protected void updateView() {
        try(Timer.Context _unused = textureUploadTimer.time()) {
            checkerboardTexture.bind(GL_TEXTURE_2D);
            checkerboardTexture.btUploadTextureUnit();
        }

        glClearColor(((float)Math.sin(colorcycler*0.05f) + 1)*0.5f, 0.8f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        //glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        glDrawArrays(GL_TRIANGLES, 0, 3*2);
    }

    @Override protected void updateLogic() {
        colorcycler++;
    }

    @Override protected String getWindowTitle() { return "BETAMAX DEMO"; }
    @Override protected int getWindowHeight() { return 540; }
    @Override protected int getWindowWidth() { return 960; }
    @Override protected boolean getDebugMode() { return true; }
}
