package com.github.galdosd.betamax;

/**
 * FIXME: Document this class
 */
public class BetamaxGlProgram extends GlProgramBase {
    public static void main(String[] args) {
        new BetamaxGlProgram().run();
    }

    @Override protected void initialize() { }

    @Override protected void keyboardEvent(int key, KeyAction action) { }

    @Override protected void updateView() {
        // glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
        // glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
    }

    @Override protected void updateLogic() {
    }

    @Override protected String getWindowTitle() { return "BETAMAX DEMO"; }
    @Override protected int getWindowHeight() { return 600; }
    @Override protected int getWindowWidth() { return 800; }
}
