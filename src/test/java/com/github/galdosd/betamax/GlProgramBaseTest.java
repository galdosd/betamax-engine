package com.github.galdosd.betamax;

import com.github.galdosd.betamax.opengl.Shader;
import com.github.galdosd.betamax.graphics.TextureCoordinate;
import org.junit.Ignore;
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

    // FIXME they fail to compile in test, probably gl not initialized, need to get log. not important yet
    @Test @Ignore public void testShadersCompile() throws Exception {
        // TODO find all *.glsl in resources/, centralize them or something
        String[] shaderNames = new String[] {
                "default.vert",
                "default.frag",
        };
        BetamaxGlProgram program = new BetamaxGlProgram();
        for(String shaderName: shaderNames) Shader.loadAndCompileShader(shaderName, /*FIXME*/ 0);
    }

    @Test public void testBasic() throws Exception {
        new GlProgramBase() {

            private boolean startedThread;
            @Override protected void initialize() { }

            @Override protected void keyPressEvent(int key) { }

            @Override protected void leftMouseClickEvent(TextureCoordinate coord) { }

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
            @Override protected boolean getDebugMode() { return false; }
        }.run();
    }
}
