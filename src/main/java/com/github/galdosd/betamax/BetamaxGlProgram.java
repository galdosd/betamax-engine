package com.github.galdosd.betamax;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;

import static com.google.common.base.Preconditions.checkState;
import static org.lwjgl.opengl.GL11.*;

/**
 * FIXME: Document this class
 */
public class BetamaxGlProgram extends GlProgramBase {
    public static void main(String[] args) {
        new BetamaxGlProgram().run();
    }

    @Override protected void initialize() {
        // load our triangle vertices
        VBO vbo = genBuffer();
        vbo.bindAndLoad(GL15.GL_ARRAY_BUFFER, GL15.GL_STATIC_DRAW, new float[]{
                 0.0f,  0.5f,
                 0.5f, -0.5f,
                -0.5f, -0.5f,
        });

        // prepare shaders
        ShaderProgram shaderProgram = new ShaderProgram();
        shaderProgram.attach(loadAndCompileShader("default-vertex.glsl"));
        shaderProgram.attach(loadAndCompileShader("default-fragment.glsl"));
        shaderProgram.linkAndUse();
    }



    @Override protected void keyboardEvent(int key, KeyAction action) { }

    @Override protected void updateView() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
    }

    @Override protected void updateLogic() {
    }

    @Override protected String getWindowTitle() { return "BETAMAX DEMO"; }
    @Override protected int getWindowHeight() { return 600; }
    @Override protected int getWindowWidth() { return 800; }
}
