package com.github.galdosd.betamax;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;

/**
 * FIXME: Document this class
 */
public class BetamaxGlProgram extends GlProgramBase {
    public static void main(String[] args) {
        new BetamaxGlProgram().run();
    }

    @Override protected void initialize() {
        VBO vbo = genBuffer();
        vbo.bind(GL15.GL_ARRAY_BUFFER);
        FloatBuffer vertices = FloatBuffer.wrap(new float[]{
                 0.0f,  0.5f,
                 0.5f, -0.5f,
                -0.5f, -0.5f,
        });
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);
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
