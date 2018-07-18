package com.github.galdosd.betamax;


import com.github.galdosd.betamax.gui.DevConsole;
import com.github.galdosd.betamax.opengl.*;
import com.github.galdosd.betamax.scripting.EventType;
import com.github.galdosd.betamax.scripting.ScriptWorld;
import com.github.galdosd.betamax.sprite.Sprite;
import com.github.galdosd.betamax.sprite.SpriteEvent;
import com.github.galdosd.betamax.sprite.SpriteName;
import com.github.galdosd.betamax.sprite.SpriteRegistry;
import com.github.galdosd.betamax.graphics.SpriteTemplateRegistry;
import org.lwjgl.glfw.GLFW;
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
    private final DevConsole devConsole = new DevConsole();
    private ScriptWorld scriptWorld;
    private SpriteRegistry spriteRegistry;
    private Optional<SpriteName> highlightedSprite = Optional.empty();
    private ShaderProgram defaultShaderProgram;
    private ShaderProgram highlightShaderProgram;

    public static void main(String[] args) {
        new BetamaxGlProgram().run();
    }

    @Override protected void initialize() {
        prepareShaders();
        prepareForDrawing();

        // enable transparency
        glEnable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glBlendFunc( GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        newWorld(true);
    }

    private void newWorld(boolean resetSprites) {
        LOG.info("Starting new world");
        if(resetSprites) {
            LOG.info("Resetting sprites");
            spriteRegistry = new SpriteRegistry(spriteTemplateRegistry, getFrameClock());
        }
        scriptWorld = new ScriptWorld(spriteRegistry);
        String[] scriptNames = Global.mainScript.split(",");
        scriptWorld.loadScripts(scriptNames);
        getFrameClock().resetLogicFrames();
    }

    // FIXME this should be paired with SpriteTemplate#renderTemplate, there is indirect dependency between them
    // via opengl VBO/VAO handles
    private void prepareForDrawing() {
        // load our triangle vertices
        VBO vbo = new VBO();
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

        // prepare vao
        VAO vao = new VAO();
        vao.bind();
        vbo.bind(GL_ARRAY_BUFFER);
        VAO.vertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        VAO.vertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
    }

    private void prepareShaders() {
        defaultShaderProgram = new ShaderProgram();
        defaultShaderProgram.attach(
                Shader.loadAndCompileShader("default.vert", GL_VERTEX_SHADER));
        defaultShaderProgram.attach(
                Shader.loadAndCompileShader("default.frag", GL_FRAGMENT_SHADER));

        highlightShaderProgram = new ShaderProgram();
        highlightShaderProgram.attach(
                Shader.loadAndCompileShader("default.vert", GL_VERTEX_SHADER));
        highlightShaderProgram.attach(
                Shader.loadAndCompileShader("highlight.frag", GL_FRAGMENT_SHADER));

        defaultShaderProgram.link();
        highlightShaderProgram.link();
    }

    @Override protected void keyPressEvent(int key, int mods) {
        // exit upon ESC key
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            closeWindow();
            LOG.info("Exiting");
        }
        else if(key == GLFW.GLFW_KEY_F1) {
            devConsole.helpWindow();
        }
        // show FPS metrics upon pause/break key
        else if(key == GLFW.GLFW_KEY_PRINT_SCREEN) {
            reportMetrics();
        }
        else if(key == GLFW.GLFW_KEY_TAB && getFrameClock().getPaused()) {
            getFrameClock().stepFrame();
            updateLogic();
        }
        else if (key == GLFW.GLFW_KEY_PAUSE) {
            getFrameClock().setPaused(!getFrameClock().getPaused());
            // FIXME this will fuck up the metrics,we need a cooked Clock for Metrics to ignore
            // the passage of time during pause
            // page up/down to change target FPS
        } else if (key == GLFW.GLFW_KEY_PAGE_UP) {
            getFrameClock().setTargetFps(getFrameClock().getTargetFps()+1);
        } else if (key == GLFW.GLFW_KEY_PAGE_DOWN) {
            if (getFrameClock().getTargetFps() > 1) {
                getFrameClock().setTargetFps(getFrameClock().getTargetFps()-1);
            }
        } else if(key == GLFW.GLFW_KEY_F5) {
            // restart the world if Ctrl+F5 is pressed
            // just reload scripts without affecting sprite state if just F5 is pressed
            // FIXME: right now state is managed in-python so the state will still be dropped
            // once we manage state in-engine we'll be fine
            newWorld((mods&GLFW.GLFW_MOD_CONTROL)!=0);
        }
    }

    @Override protected void mouseClickEvent(TextureCoordinate coordinate, int button) {
        LOG.debug("Clicked at {} x {}", coordinate.getX(), coordinate.getY());
        Optional<SpriteName> clickedSprite = spriteRegistry.getSpriteAtCoordinate(coordinate);
        if(clickedSprite.isPresent()) {
            LOG.debug("Clicked on sprite {} (button {})", clickedSprite.get(), button);
            handleSpriteClick(clickedSprite.get(), button);
        }
    }

    private void handleSpriteClick(SpriteName spriteName, int button) {
        if(button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            spriteRegistry.enqueueSpriteEvent(new SpriteEvent(EventType.SPRITE_CLICK, spriteName, 0));
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            Optional<SpriteName> selectedSprite = devConsole.getSelectedSprite();
            if(selectedSprite.isPresent() && spriteName.equals(selectedSprite.get())) {
                devConsole.clearHighlightedSprite();
            } else {
                highlightedSprite = Optional.of(spriteName);
            }
        }
    }

    /** cycle background color; can be helpful in debugging */
    private int colorcycler = 0;
    private long nextConsoleUpdate = System.currentTimeMillis();
    @Override protected void updateView() {
        defaultShaderProgram.use();
        glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        Optional<SpriteName> selectedSprite = devConsole.getSelectedSprite();
        for(Sprite sprite: spriteRegistry.getSpritesInRenderOrder()) {
            if(selectedSprite.isPresent() && sprite.getName().equals(selectedSprite.get())) {
                highlightShaderProgram.use();
            } else {
                defaultShaderProgram.use();
            }
            sprite.render();
        }
        if(System.currentTimeMillis() > nextConsoleUpdate) {
            devConsole.updateSprites(spriteRegistry.getSpritesInRenderOrder(), highlightedSprite);
            highlightedSprite = Optional.empty();
            nextConsoleUpdate = System.currentTimeMillis() + Global.devConsoleUpdateIntervalMillis;
        }
    }

    @Override protected void updateLogic() {

        spriteRegistry.dispatchEvents(scriptWorld);
    }

    @Override protected String getWindowTitle() { return "BETAMAX DEMO"; }
    @Override protected int getWindowHeight() { return 540; }
    @Override protected int getWindowWidth() { return 960; }
    @Override protected boolean getDebugMode() { return true; }
}

