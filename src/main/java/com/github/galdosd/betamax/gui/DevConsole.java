package com.github.galdosd.betamax.gui;

import com.github.galdosd.betamax.sprite.Sprite;
import com.google.common.collect.Ordering;
import javafx.application.Platform;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * FIXME: Document this class
 */
public final class DevConsole {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private final FxWindow window; // FIXME this gets replaced for some reason????

    public DevConsole() {
        window = FxWindow.singleton();
        LOG.info("FxWindow launched");
    }

    public void updateSprites(Collection<Sprite> sprites) {
        final int[] tableIndex = {0};
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
                    return fxSprite;
                })
                .collect(toList());

        Platform.runLater( () -> {
            window.updateSpriteData(updatedFxSprites);
        });
    }

}
