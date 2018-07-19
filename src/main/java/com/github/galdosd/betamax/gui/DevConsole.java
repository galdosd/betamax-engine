package com.github.galdosd.betamax.gui;

import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.OurTool;
import com.github.galdosd.betamax.sprite.Sprite;
import com.github.galdosd.betamax.sprite.SpriteName;
import com.google.common.collect.Ordering;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import lombok.Getter;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
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

    public void updateSprites(Collection<Sprite> sprites, Optional<SpriteName> highlightedSprite) {
        final int[] tableIndex = {0};
        List<FxSprite> updatedFxSprites = sprites.stream()
                .sorted(Ordering.natural().onResultOf(Sprite::getAge).reverse())
                .map(sprite -> {
                    FxSprite fxSprite = new FxSprite(tableIndex[0]++);
                    fxSprite.load(sprite);
                    return fxSprite;
                })
                .collect(toList());

        Platform.runLater( () -> {
            window.updateSpriteData(updatedFxSprites);
            if(highlightedSprite.isPresent()) {
                window.setSelectedSprite(highlightedSprite.get());
            }
            synchronized ($LOCK) {
                selectedSprite = window.getSelectedSprite();
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
            window.setSelectedSprite(null);
        });

    }
}
