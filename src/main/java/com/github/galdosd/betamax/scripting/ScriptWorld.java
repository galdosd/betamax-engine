package com.github.galdosd.betamax.scripting;

import com.codahale.metrics.Timer;
import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.OurTool;
import com.github.galdosd.betamax.sprite.LogicHandler;
import com.github.galdosd.betamax.sprite.SpriteEvent;
import com.github.galdosd.betamax.sprite.SpriteRegistry;
import org.python.core.Py;
import org.python.util.PythonInterpreter;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;

/** Represent/Abstract away the whole scripting environment to the rest of the java program
 * and deal with anything python related
 *
 * Note: I tried to use JSR223 but it seemed broken (I didn't spend enough time to exhaustively prove this and thus
 * submit a JDK or jython patch though) -- anyway if we really want to support other languages, JSR223 is a very
 * thin wrapper anyway, it's trivial to abstract that out ourselves. JSR223 sounds cool as hell but actually adds
 * little value, which is probably why it's broken, nobody else cared enough to whine about it either!
 */
// TODO something is terrible and needs refactoring about ScriptWorld/SpriteRegistry/ScriptServicer
public class ScriptWorld implements LogicHandler {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private static final Timer invokeSpriteCallbackTimer = Global.metrics.timer("invokeSpriteCallbackTimer");

    private final SpriteRegistry spriteRegistry;
    private final PythonInterpreter pythonInterpreter;
    private final ScriptServicer servicer;

    public ScriptWorld(SpriteRegistry spriteRegistry) {
        this.spriteRegistry = spriteRegistry;
        servicer = new ScriptServicer(spriteRegistry);
        pythonInterpreter = PythonInterpreter.threadLocalStateInterpreter(null);
    }

    @Override public void onSpriteEvent(SpriteEvent event) {
        ScriptCallback callback = servicer.getCallback(event);
        // allow python style negative indices, eg -1 is the last frame
        if(null==callback && event.eventType==EventType.SPRITE_MOMENT) {
            int totalSpriteFrames = spriteRegistry.getSpriteByName(event.spriteName).getTotalFrames();
            SpriteEvent negativeEvent = event.withMoment(event.moment - totalSpriteFrames);
            callback = servicer.getCallback(negativeEvent);
        }
        if(null!=callback) {
            LOG.debug("Handling {}\nvia {}", event, callback);
            try(Timer.Context ignored = invokeSpriteCallbackTimer.time()) {
                callback.invoke();
            }
        }
    }

    @Override public void onBegin() {
       // spriteRegistry.createSprite("room", "sprite_room");
      //  spriteRegistry.createSprite("demowalk", "sprite_demowalk");
        LOG.info("onBegin");
        ScriptCallback callback = servicer.getCallback(EventType.BEGIN);
        if(null!=callback) {
            callback.invoke();
        }
    }

    public void loadScripts(String... scriptNames) {
        pythonInterpreter.set("_BETAMAX_INTERNAL_script_servicer", servicer);
        // I know it looks wacky. this is the best way i could figure to smuggle our ScriptServicer
        // instance in to the betamax module without making it too blatantly available to the main script
        // or otherwise potentially clashing with anything. just want to keep things simple -- for the python scripter,
        // at the expense of the java developer...
        // i also had the weird goal of forcing the python scripter to say "import betamax" in order to avoid the
        // appearance of magic :) after all it's jython, and jexplicit is jbetter than jimplicit, right?
        pythonInterpreter.exec(
                "import betamax as _BETAMAX_INTERNAL_servicer_registration; "+
                "_BETAMAX_INTERNAL_servicer_registration.register_script_servicer(_BETAMAX_INTERNAL_script_servicer);"
        );
        for(String scriptName: scriptNames) {
            try (InputStream scriptStream = OurTool.streamResource(Global.scriptBase + scriptName)) {
                spriteRegistry.setAcceptingCallbacks(true);
                LOG.info("Evaluating jython script from {}", scriptName);
                pythonInterpreter.execfile(scriptStream, scriptName);
                servicer.finishInit();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                spriteRegistry.setAcceptingCallbacks(false);
            }
        }
    }

    public Map<SpriteEvent,ScriptCallback> getAllCallbacks() {
        return servicer.getAllCallbacks();
    }

    public Map<String, String> getStateVariables() {
        return servicer.getAllStateVariables();
    }
}
