package com.github.galdosd.betamax;


import static com.google.common.base.Preconditions.checkState;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

/**
 * FIXME: Document this class
 */
public class BetamaxGlProgram extends GlProgramBase {
    public static void main(String[] args) {
        new BetamaxGlProgram().run();
    }

    @Override protected void initialize() {

        // load our triangle vertices
        VBO vbo = new VBO();
        float textureScale = 50;
        vbo.bindAndLoad(GL_ARRAY_BUFFER, GL_STATIC_DRAW, new float[]{
               //xpos   ypos      xtex                 ytex
                 0.0f,  0.5f,     textureScale*0.50f,  textureScale*0.75f,
                 0.5f, -0.5f,     textureScale*0.75f,  textureScale*0.25f,
                -0.5f, -0.5f,     textureScale*0.25f,  textureScale*0.25f,
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
                0.1f, 0.1f, 0.0f, 1.0f,     1.0f, 0.9f, 1.0f, 1.0f,
                1.0f, 0.9f, 1.0f, 1.0f,     0.1f, 0.1f, 0.0f, 1.0f,
        };

        Texture checkerboardTexture = new Texture();
        checkerboardTexture.bind(GL_TEXTURE_2D);
        checkerboardTexture.btSetParameters();
        checkerboardTexture.btLoadRgba(texturePixels, 2, 2);


    }


    @Override protected void keyboardEvent(int key, KeyAction action) { }

    int colorcycler = 0;
    @Override protected void updateView() {
        glClearColor(((float)Math.sin(colorcycler++*0.2f) + 1)*0.5f, 0.8f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        //glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        glDrawArrays(GL_TRIANGLES, 0, 3);
    }

    @Override protected void updateLogic() {
    }

    @Override protected String getWindowTitle() { return "BETAMAX DEMO"; }
    @Override protected int getWindowHeight() { return 600; }
    @Override protected int getWindowWidth() { return 800; }
    @Override protected boolean getDebugMode() { return true; }
}
