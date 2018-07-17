package com.github.galdosd.betamax.gui;

import com.github.galdosd.betamax.FrameClock;
import com.github.galdosd.betamax.sprite.Sprite;
import com.google.common.collect.Ordering;
import javafx.application.Platform;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * FIXME: Document this class
 */
public final class DevConsole {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private final FxWindow window; // FIXME this gets replaced for some reason????

    public DevConsole() {
        window = new FxWindow();
        window.doLaunch();
        LOG.info("FxWindow launched");
    }
    public void updateSprites(Collection<Sprite> sprites) {
        List<FxSprite> updatedFxSprites = sprites.stream()
                .map(sprite -> {
                    FxSprite fxSprite = new FxSprite();
                    fxSprite.setSpriteName( sprite.getName().getName() );
                    fxSprite.setTemplate( sprite.getTemplateName() );
                    fxSprite.setMoment( sprite.getRenderedFrame() );
                    fxSprite.setLength( sprite.getTotalFrames() );
                    fxSprite.setAge( sprite.getAge() );
                    return fxSprite;
                })
                .sorted(Ordering.natural().onResultOf(FxSprite::getAge).reverse())
                .collect(toList());

        Platform.runLater( () -> {
            LOG.debug("Updating sprite report: {}", updatedFxSprites);

            window.updateSpriteData(updatedFxSprites);
        });
    }

}
