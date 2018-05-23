package com.github.galdosd.betamax;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import org.lwjgl.opengl.*;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;

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

        // load our triangle vertices
        VBO vbo = new VBO();
        vbo.bindAndLoad(GL_ARRAY_BUFFER, GL_STATIC_DRAW, new float[]{
                 0.0f,  0.5f,
                 0.5f, -0.5f,
                -0.5f, -0.5f,
        });

        // prepare shaders
        ShaderProgram shaderProgram = new ShaderProgram();
        shaderProgram.attach(loadAndCompileShader("default-vertex.glsl", GL20.GL_VERTEX_SHADER));
        shaderProgram.attach(loadAndCompileShader("default-fragment.glsl", GL20.GL_FRAGMENT_SHADER));
        shaderProgram.linkAndUse();


        // prepare VAO
        VAO vao = new VAO();
        vao.bind();

        vbo.bind(GL_ARRAY_BUFFER);
        vertexAttribPointer(shaderProgram, "position", 2, GL_FLOAT, false, 0, 0);


    }



    @Override protected void keyboardEvent(int key, KeyAction action) { }

    @Override protected void updateView() {
        // glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        // glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
        glDrawArrays(GL_TRIANGLES, 0, 3);
    }

    @Override protected void updateLogic() {
    }

    @Override protected String getWindowTitle() { return "BETAMAX DEMO"; }
    @Override protected int getWindowHeight() { return 600; }
    @Override protected int getWindowWidth() { return 800; }
    @Override protected boolean getDebugMode() { return true; }
}
