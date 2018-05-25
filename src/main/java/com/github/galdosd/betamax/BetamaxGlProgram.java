package com.github.galdosd.betamax;

import org.lwjgl.opengl.*;

import static com.google.common.base.Preconditions.checkState;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;

/**
 * FIXME: Document this class
 */
public class BetamaxGlProgram extends GlProgramBase {
    public static void main(String[] args) {
        new BetamaxGlProgram().run();
    }

    @Override protected void initialize() {
        VAO vao = new VAO();
        vao.bind();

        // load our triangle vertices
        VBO vbo = new VBO();
        vbo.bindAndLoad(GL_ARRAY_BUFFER, GL_STATIC_DRAW, new float[]{
                 0.0f,  0.5f,
                 0.5f, -0.5f,
                -0.5f, -0.5f,
        });

        // prepare shaders
        ShaderProgram shaderProgram = new ShaderProgram();
        shaderProgram.attach(
                loadAndCompileShader("default.vert", GL20.GL_VERTEX_SHADER));
        shaderProgram.attach(
                loadAndCompileShader("default.frag", GL20.GL_FRAGMENT_SHADER));
        shaderProgram.bindFragDataLocation(0, "outColor");
        shaderProgram.linkAndUse();



        vertexAttribPointer(shaderProgram, "position", 2, GL_FLOAT, false, 0, 0);


    }


    @Override protected void keyboardEvent(int key, KeyAction action) { }

    int colorcycler = 0;
    @Override protected void updateView() {
        glClearColor(1.0f, colorcycler++ / 100.0f, 0.0f, 1.0f);
        colorcycler = colorcycler % 100;
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
