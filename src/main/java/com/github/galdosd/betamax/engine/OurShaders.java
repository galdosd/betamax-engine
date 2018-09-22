package com.github.galdosd.betamax.engine;

import com.github.galdosd.betamax.opengl.ShaderProgram;


/**
 * FIXME: Document this class
 */
public final class OurShaders extends OurShadersBase {
    public final ShaderProgram DEFAULT = prepareShaderProgram("default.vert", "default.frag");
    public final ShaderProgram HIGHLIGHT = prepareShaderProgram("default.vert", "highlight.frag");

    // User shaders. FIXME: this is the only place where users have to edit a java file, which is irresponsibly leaky and
    // unnecessary since we look them up by string anyway. Just use filenames instead.
    public final ShaderProgram NIGHT = prepareShaderProgram("default.vert", "night.frag");
    public final ShaderProgram NIGHT2 = prepareShaderProgram("default.vert", "night_more_saturation.frag");
    public final ShaderProgram DARKEN = prepareShaderProgram("default.vert", "darken.frag");
    public final ShaderProgram NIGHT_COMBINED = prepareShaderProgram("default.vert", "night_combined.frag");
    public final ShaderProgram BLACK = prepareShaderProgram("default.vert", "black.frag");
    public final ShaderProgram UPWIPE_TEST = prepareShaderProgram("upwipe.vert", "default.frag");
}
