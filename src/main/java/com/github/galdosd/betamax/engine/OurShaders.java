package com.github.galdosd.betamax.engine;

import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.opengl.Shader;
import com.github.galdosd.betamax.opengl.ShaderProgram;

import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;

/**
 * FIXME: Document this class
 */
public final class OurShaders {
    final ShaderProgram DEFAULT = prepareShaderProgram("default.vert", "default.frag");
    final ShaderProgram HIGHLIGHT = prepareShaderProgram("default.vert", "highlight.frag");

    OurShaders() {}

    private ShaderProgram prepareShaderProgram(String vertexShader, String fragmentShader) {
        ShaderProgram shaderProgram;
        shaderProgram = new ShaderProgram();
        shaderProgram.attach(
                Shader.loadAndCompileShader(Global.shaderBase+vertexShader, GL_VERTEX_SHADER));
        shaderProgram.attach(
                Shader.loadAndCompileShader(Global.shaderBase+fragmentShader, GL_FRAGMENT_SHADER));
        shaderProgram.link();
        return shaderProgram;
    }
}
