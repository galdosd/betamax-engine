package com.github.galdosd.betamax.gui;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import lombok.Getter;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * FIXME: Document this class
 */
public class FxWindow extends Application {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private TableView<FxSprite> spriteTable;
    private final ObservableList<FxSprite> spriteData = FXCollections.observableArrayList( );

    @Override public void start(Stage stage) {
        spriteTable = new TableView<>();

        spriteTable.setEditable(false);

        spriteTable.setItems(spriteData);
        spriteTable.getColumns().addAll(new FxSprite().tableColumns());

        Pane sceneRoot = new Pane();
        spriteTable.prefWidthProperty().bind(sceneRoot.widthProperty());

        TabPane tabPane = new TabPane();
        Tab spritesTab = new Tab();
        spritesTab.setClosable(false);
        tabPane.prefWidthProperty().bind(sceneRoot.widthProperty());
        spritesTab.setText("Sprites");
        spritesTab.setContent(spriteTable);
        tabPane.getTabs().addAll(spritesTab);

        sceneRoot.getChildren().addAll(tabPane);

        Scene scene = new Scene(sceneRoot);
        stage.setScene(scene);
        stage.setTitle("Betamax Developer Console");
        stage.setWidth(640);
        stage.setHeight(640);
        stage.show();


        LOG.debug("scene been shown");

        FxSprite fxSprite = new FxSprite();
        fxSprite.setMoment(64);
        fxSprite.setSpriteName("dog pound");
        spriteData.add(fxSprite);
        LOG.debug("fxsprite been added");
    }

    public void doLaunch() {
        new Thread( () -> {
            launch();
        }).start();;
    }

    public void updateSpriteData(List<FxSprite> updatedFxSprites) {
        spriteData.setAll(updatedFxSprites);
        if(null!=spriteTable) {
            spriteTable.refresh();
            LOG.debug("spriteTable.refresh()");
        }

    }
}

