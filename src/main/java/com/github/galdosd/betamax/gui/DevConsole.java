package com.github.galdosd.betamax.gui;

import com.github.galdosd.betamax.sprite.Sprite;
import com.github.galdosd.betamax.sprite.SpriteName;
import com.google.common.collect.Ordering;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Alert;
import lombok.Getter;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
        final int[] highlightedIndex = {-1};
        List<FxSprite> updatedFxSprites = sprites.stream()
                .sorted(Ordering.natural().onResultOf(Sprite::getAge).reverse())
                .map(sprite -> {
                    FxSprite fxSprite = new FxSprite(tableIndex[0]++);
                    fxSprite.setSpriteName( sprite.getName().getName() );
                    fxSprite.setTemplate( sprite.getTemplateName() );
                    fxSprite.setMoment( sprite.getRenderedFrame() );
                    fxSprite.setLength( sprite.getTotalFrames() );
                    fxSprite.setAge( sprite.getAge() );
                    fxSprite.setSerial( sprite.getCreationSerial() );
                    fxSprite.setLayer( sprite.getLayer() );
                    if(highlightedSprite.isPresent() && sprite.getName().equals(highlightedSprite.get())) {
                        highlightedIndex[0] = fxSprite.getTableIndex();
                    }
                    return fxSprite;
                })
                .collect(toList());

        Platform.runLater( () -> {
            window.updateSpriteData(updatedFxSprites);
            if(-1 != highlightedIndex[0]) {
                window.selectSpriteIndex(highlightedIndex[0]);
            }
            synchronized ($LOCK) {
                selectedSprite = window.getSelectedSprite();
            }
        });
    }

    public void helpWindow() {
        Platform.runLater(() -> {
            new Alert(Alert.AlertType.INFORMATION, "Help info\n\n no help for you\n you are stuck").showAndWait();
        });
    }
    public void clearHighlightedSprite() {
        Platform.runLater( () -> {
            window.setSelectedSprite(null);
        });

    }
}
