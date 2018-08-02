package com.github.galdosd.betamax.engine;

import com.github.galdosd.betamax.opengl.ShaderProgram;


/**
 * FIXME: Document this class
 */
public final class OurShaders extends OurShadersBase {
    public final ShaderProgram DEFAULT = prepareShaderProgram("default.vert", "default.frag");
    public final ShaderProgram HIGHLIGHT = prepareShaderProgram("default.vert", "highlight.frag");
}
