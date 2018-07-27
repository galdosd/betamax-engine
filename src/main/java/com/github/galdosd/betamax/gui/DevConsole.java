package com.github.galdosd.betamax.gui;

import com.codahale.metrics.Snapshot;
import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.OurTool;
import com.github.galdosd.betamax.engine.FrameClock;
import com.github.galdosd.betamax.scripting.ScriptCallback;
import com.github.galdosd.betamax.sprite.Sprite;
import com.github.galdosd.betamax.sprite.SpriteEvent;
import com.github.galdosd.betamax.sprite.SpriteName;
import com.google.common.collect.Ordering;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import lombok.Getter;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * FIXME: Document this class
 */
public final class DevConsole {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private final FxWindow window; // FIXME this gets replaced for some reason????
    private final Object $LOCK = new Object();
    @Getter private Optional<SpriteName> selectedSprite = Optional.empty();

    public DevConsole() {
        window = FxWindow.singleton();
        LOG.info("FxWindow launched");
    }

    public static Map<String, String> getDebugParameters(FrameClock frameClock, String actionState) {
        return new HashMap<String,String>() {{
            put("Frame#", String.valueOf(frameClock.getCurrentFrame()));
            put("FPS (target)", String.valueOf(frameClock.getTargetFps()));
            put("Frame Budget", String.valueOf(1000.0 / frameClock.getTargetFps()));
            put("Action State", actionState);
            Global.metrics.getCounters().entrySet().stream().forEach(entry -> {
                String key = entry.getKey();
                long count = entry.getValue().getCount();
                if(key.endsWith("Bytes")) {
                    count = count / 1024 / 1024;
                    key = key.replace("Bytes", "Megabytes");
                }
                put(key, String.valueOf(count));
            });
            Global.metrics.getGauges().entrySet().stream().forEach( entry ->
                put(entry.getKey(), String.valueOf(entry.getValue().getValue()))
            );
        }};
    }

    public void updateView(
            Collection<Sprite> sprites,
            Optional<SpriteName> highlightedSprite,
            Map<SpriteEvent,ScriptCallback> allCallbacks,
            Map<String, String> stateVariables,
            FrameClock frameClock,
            String actionStateString
    ) {
        Map<String, String> debugParameters = getDebugParameters(frameClock, actionStateString);
        // TODO obviously factor out these stream/sorted/map/collects in a generic manner
        final int[] tableIndex = {0};
        tableIndex[0] = 0;
        List<FxSprite> updatedFxSprites = sprites.stream()
                .sorted(Ordering.natural().onResultOf(Sprite::getAge).reverse())
                .map(sprite -> new FxSprite(tableIndex[0]++, sprite))
                .collect(toList());

        tableIndex[0] = 0;
        List<FxCallback> updatedCallbacks = allCallbacks.entrySet().stream()
                .sorted(Ordering.natural().onResultOf(entry -> entry.getKey().toString()))
                .map(entry -> new FxCallback(tableIndex[0]++, entry.getKey(), entry.getValue()))
                .collect(toList());

        tableIndex[0] = 0;
        List<FxVariable> updatedStateVariables = stateVariables.entrySet().stream()
                .sorted(Ordering.natural().onResultOf(entry -> entry.getKey()))
                .map(entry -> new FxVariable(tableIndex[0]++, entry.getKey(), entry.getValue()))
                .collect(toList());

        tableIndex[0] = 0;
        List<FxVariable> updatedMetricsVariables = debugParameters.entrySet().stream()
                .sorted(Ordering.natural().onResultOf(entry -> entry.getKey()))
                .map(entry -> new FxVariable(tableIndex[0]++, entry.getKey(), entry.getValue()))
                .collect(toList());

        tableIndex[0] = 0;
        List<FxVariable> updatedSystemProperties = System.getProperties().stringPropertyNames().stream()
                .sorted(Ordering.natural())
                .map(propertyName -> new FxVariable(tableIndex[0]++, propertyName, System.getProperty(propertyName)))
                .collect(toList());

        tableIndex[0] = 0;
        List<FxTimer> updatedTimers = Global.metrics.getTimers().entrySet().stream()
                .sorted(Ordering.natural().onResultOf(entry -> entry.getKey()))
                .map(entry -> new FxTimer(tableIndex[0]++, entry.getKey(), entry.getValue()))
                .collect(toList());

        Platform.runLater( () -> {
            window.getSpriteTable().updateRowData(updatedFxSprites);
            window.getCallbackTable().updateRowData(updatedCallbacks);
            window.getStateTable().updateRowData(updatedStateVariables);
            window.getMetricsTable().updateRowData(updatedMetricsVariables);
            window.getPropertyTable().updateRowData(updatedSystemProperties);
            window.getTimersTable().updateRowData(updatedTimers);
            if(highlightedSprite.isPresent()) {
                window.getSpriteTable().setSelectedSprite(highlightedSprite.get());
            }
            synchronized ($LOCK) {
                selectedSprite = window.getSpriteTable().getSelectedSprite();
            }
        });
    }

    public void helpWindow() {
        Platform.runLater(() -> {
            new Alert(Alert.AlertType.INFORMATION, OurTool.loadResource(Global.helpFile)).showAndWait();
        });
    }

    public void clearHighlightedSprite() {
        Platform.runLater( () -> {
            window.getSpriteTable().setSelectedSprite(null);
        });

    }
}
