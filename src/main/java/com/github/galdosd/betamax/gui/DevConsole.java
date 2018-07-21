package com.github.galdosd.betamax.gui;

import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.OurTool;
import com.github.galdosd.betamax.scripting.ScriptCallback;
import com.github.galdosd.betamax.sprite.Sprite;
import com.github.galdosd.betamax.sprite.SpriteEvent;
import com.github.galdosd.betamax.sprite.SpriteName;
import com.google.common.collect.Ordering;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import lombok.Getter;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    public void updateView(
            Collection<Sprite> sprites,
            Optional<SpriteName> highlightedSprite,
            Map<SpriteEvent,ScriptCallback> allCallbacks
    ) {
        final int[] tableIndex = {0,0};
        List<FxSprite> updatedFxSprites = sprites.stream()
                .sorted(Ordering.natural().onResultOf(Sprite::getAge).reverse())
                .map(sprite -> new FxSprite(tableIndex[0]++, sprite))
                .collect(toList());

        List<FxCallback> updatedCallbacks = allCallbacks.entrySet().stream()
                .sorted(Ordering.natural().onResultOf(entry -> entry.getKey().toString()))
                .map(entry -> new FxCallback(tableIndex[1]++, entry.getKey(), entry.getValue()))
                .collect(toList());

        Platform.runLater( () -> {
            window.getSpriteTable().updateRowData(updatedFxSprites);
            window.getCallbackTable().updateRowData(updatedCallbacks);
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
