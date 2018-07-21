package com.github.galdosd.betamax.gui;

import com.github.galdosd.betamax.sprite.SpriteEvent;
import com.github.galdosd.betamax.sprite.SpriteName;
import com.google.common.base.Preconditions;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import lombok.Getter;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.util.stream.Collectors.toMap;

/**
 * FIXME: Document this class
 */
public final class FxWindow extends Application {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private static final CompletableFuture<FxWindow> singleton = new CompletableFuture<>();
    private static final Object LOCK$singleton = new Object();

    @Getter private FxTable<FxSprite,SpriteName> spriteTable;
    @Getter private FxTable<FxCallback,SpriteEvent> callbackTable;
    @Getter private FxTable<FxVariable,String> stateTable;
    @Getter private FxTable<FxVariable,String> paramTable;

    public static FxWindow singleton() {
        new Thread( () -> {
            Platform.setImplicitExit(false);
            launch();
        }).start();
        for(;;) {
            try {
                return singleton.get();
            } catch (InterruptedException e) {
                LOG.debug("Interrupted, trying again", e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override public void start(Stage stage) {
        Pane sceneRoot = new Pane();
        TabPane tabPane = new TabPane();
        tabPane.prefWidthProperty().bind(sceneRoot.widthProperty());
        tabPane.prefHeightProperty().bind(sceneRoot.heightProperty());
        sceneRoot.getChildren().addAll(tabPane);

        addTabs(sceneRoot, tabPane);

        Scene scene = new Scene(sceneRoot);
        stage.setScene(scene);
        stage.setTitle("Betamax Developer Console");
        stage.setWidth(800);
        stage.setHeight(640);
        stage.show();
        stage.setOnCloseRequest( x -> { x.consume(); LOG.info("Closing developer console forbidden"); } );
        completeSingleton();
    }

    private void addTabs(Pane sceneRoot, TabPane tabPane) {
        spriteTable = new FxTable<>();
        tabPane.getTabs().addAll(spriteTable.start(sceneRoot, FxSprite.class, "Sprites"));

        callbackTable = new FxTable<>();
        tabPane.getTabs().addAll(callbackTable.start(sceneRoot, FxCallback.class, "Callbacks"));

        stateTable = new FxTable<>();
        tabPane.getTabs().addAll(stateTable.start(sceneRoot, FxVariable.class, "State"));

        paramTable = new FxTable<>();
        tabPane.getTabs().addAll(paramTable.start(sceneRoot, FxVariable.class, "Parameters"));
    }

    private void completeSingleton() {
        synchronized (LOCK$singleton) {
            Preconditions.checkState(!singleton.isDone());
            singleton.complete(this);
            LOG.debug("singleton completed");
        }
    }
}

