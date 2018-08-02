package com.github.galdosd.betamax.engine;

import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.opengl.Shader;
import com.github.galdosd.betamax.opengl.ShaderProgram;

import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import java.util.HashMap;
import java.util.Map;

/**
 * FIXME: Document this class
 */
public abstract class OurShadersBase {
      protected final ShaderProgram prepareShaderProgram(String vertexShader, String fragmentShader) {
        ShaderProgram shaderProgram;
        shaderProgram = new ShaderProgram();
        shaderProgram.attach(
                Shader.loadAndCompileShader(Global.shaderBase+vertexShader, GL_VERTEX_SHADER));
        shaderProgram.attach(
                Shader.loadAndCompileShader(Global.shaderBase+fragmentShader, GL_FRAGMENT_SHADER));
        shaderProgram.link();
        return shaderProgram;
    }

    private final Map<String,ShaderProgram> cachedShaderPrograms = new HashMap<>();

    public final ShaderProgram getShaderProgram(String globalShader) {
        if(null==globalShader) {
            return getShaderProgram("DEFAULT");
        }
        if(!cachedShaderPrograms.containsKey(globalShader)) {
            cachedShaderPrograms.put(globalShader, _getShaderProgram(globalShader));
        }
        return cachedShaderPrograms.get(globalShader);
    }

    private ShaderProgram _getShaderProgram(String globalShader) {
        try {
            return (ShaderProgram) getClass().getField(globalShader).get(this);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException("You dummy, there's no such shader in OurShaders.java: " + globalShader, e);
        }
    }
}
