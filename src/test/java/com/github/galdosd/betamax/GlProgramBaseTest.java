package com.github.galdosd.betamax;

import org.junit.Test;
import org.lwjgl.opengl.GL11;
import org.slf4j.LoggerFactory;

import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;

/**
 * FIXME: Document this class
 */
public class GlProgramBaseTest {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());
    @Test public void testBasic() throws Exception {
        new GlProgramBase() {

            private boolean startedThread;
            @Override protected void initialize() { }
            @Override protected void keyboardEvent(int key, KeyAction action) { }

            @Override protected void updateView() {
                glClearColor(1.0f, 1.0f, 0.0f, 0.0f);
                glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT); // clear the framebuffer
                if(!startedThread) {
                    startedThread=true;
                    new Thread(() -> {
                        try { Thread.sleep(2000); } catch (InterruptedException e) { }
                        closeWindow();
                    }).run();
                }
            }

            @Override protected void updateLogic() { }
            @Override protected String getWindowTitle() { return "test"; }
            @Override protected int getWindowHeight() { return 100; }
            @Override protected int getWindowWidth() { return 100; }
        }.run();
    }
}
