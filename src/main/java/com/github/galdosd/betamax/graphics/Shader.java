package com.github.galdosd.betamax.graphics;

import com.github.galdosd.betamax.OurTool;
import lombok.Value;

import static com.google.common.base.Preconditions.checkState;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL20.*;

/**
 * FIXME: Document this class
 */
@Value
public final class Shader {
    int handle;

    public static Shader loadAndCompileShader(String filename, int shaderType)  {
        String shaderSource = OurTool.loadResource(filename);
        int shader = glCreateShader(shaderType);
        glShaderSource(shader, shaderSource);
        glCompileShader(shader);

        int status = glGetShaderi(shader, GL_COMPILE_STATUS);
        checkState(GL_TRUE == status, "Shader compilation failure: %s", filename);
        return new Shader(shader);
    }
}
