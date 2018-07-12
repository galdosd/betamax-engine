package com.github.galdosd.betamax;


import com.github.galdosd.betamax.graphics.*;
import com.github.galdosd.betamax.opengl.Shader;
import com.github.galdosd.betamax.opengl.ShaderProgram;
import com.github.galdosd.betamax.opengl.VAO;
import com.github.galdosd.betamax.opengl.VBO;
import com.github.galdosd.betamax.scripting.EventType;
import com.github.galdosd.betamax.scripting.ScriptWorld;
import com.github.galdosd.betamax.sprite.SpriteEvent;
import com.github.galdosd.betamax.sprite.SpriteName;
import com.github.galdosd.betamax.sprite.SpriteRegistry;
import com.github.galdosd.betamax.sprite.SpriteTemplateRegistry;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

/**
 * FIXME: Document this class
 */
public class BetamaxGlProgram extends GlProgramBase {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private final SpriteTemplateRegistry spriteTemplateRegistry = new SpriteTemplateRegistry();

    private ScriptWorld scriptWorld;
    private SpriteRegistry spriteRegistry = new SpriteRegistry(spriteTemplateRegistry, getFrameClock());

    public static void main(String[] args) {
        new BetamaxGlProgram().run();
    }

    @Override protected void initialize() {

        // load our triangle vertices
        VBO vbo = new VBO();
        float textureScale = 2;
        vbo.bindAndLoad(GL_ARRAY_BUFFER, GL_STATIC_DRAW, new float[]{
            // two right triangles that cover the full screen
            // all our sprites are fullscreen! wow!
               //xpos   ypos      xtex  ytex
                -1.0f,  1.0f,     0.0f, 1.0f,
                 1.0f,  1.0f,     1.0f, 1.0f,
                -1.0f, -1.0f,     0.0f, 0.0f,

                 1.0f,  1.0f,     1.0f, 1.0f,
                 1.0f, -1.0f,     1.0f, 0.0f,
                -1.0f, -1.0f,     0.0f, 0.0f,
        });

        // prepare shaders
        ShaderProgram shaderProgram = new ShaderProgram();
        shaderProgram.attach(
                Shader.loadAndCompileShader("default.vert", GL_VERTEX_SHADER));
        shaderProgram.attach(
                Shader.loadAndCompileShader("default.frag", GL_FRAGMENT_SHADER));
        //shaderProgram.bindFragDataLocation(0, "outColor"); //unnecessary, we only have one output color
        shaderProgram.linkAndUse();

        // prepare vao
        VAO vao = new VAO();
        vao.bind();
        vbo.bind(GL_ARRAY_BUFFER);
        VAO.vertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        VAO.vertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);

        // enable transparency
        glEnable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glBlendFunc( GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        scriptWorld = new ScriptWorld(spriteRegistry);
        scriptWorld.loadScript(Global.mainScript);
    }


    @Override protected void keyboardEvent(int key) { }

    @Override protected void leftMouseClickEvent(TextureCoordinate coordinate) {
        LOG.trace("Clicked at {} x {}", coordinate.getX(), coordinate.getY());
        Optional<SpriteName> clickedSprite = spriteRegistry.getSpriteAtCoordinate(coordinate);
        if(clickedSprite.isPresent()) {
            LOG.info("Clicked on sprite {}", clickedSprite.get());
            spriteRegistry.enqueueSpriteEvent(new SpriteEvent(EventType.SPRITE_CLICK, clickedSprite.get(), 0));
        }
    }


    /** cycle background color; can be helpful in debugging */
    private int colorcycler = 0;
    @Override protected void updateView() {
        glClearColor(((float)Math.sin(colorcycler*0.05f) + 1)*0.5f, 0.8f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        spriteRegistry.renderAll();
    }

    @Override protected void updateLogic() {
        colorcycler++;
        spriteRegistry.dispatchEvents(scriptWorld);
    }

    @Override protected String getWindowTitle() { return "BETAMAX DEMO"; }
    @Override protected int getWindowHeight() { return 540; }
    @Override protected int getWindowWidth() { return 960; }
    @Override protected boolean getDebugMode() { return true; }
}
