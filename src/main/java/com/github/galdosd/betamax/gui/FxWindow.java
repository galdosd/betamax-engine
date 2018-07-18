package com.github.galdosd.betamax.gui;

import com.github.galdosd.betamax.sprite.SpriteName;
import com.google.common.base.Preconditions;
import javafx.application.Application;
import javafx.application.Platform;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

/**
 * FIXME: Document this class
 */
public final class FxWindow extends Application {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private static final CompletableFuture<FxWindow> singleton = new CompletableFuture<>();
    private static final Object LOCK$singleton = new Object();
    private TableView<FxSprite> spriteTable;
    private final ObservableList<FxSprite> spriteData = FXCollections.observableArrayList( );

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


    @Getter ObservableList<FxSprite> selectedItems;
    @Override public void start(Stage stage) {

        spriteTable = new TableView<>();

        spriteTable.setEditable(false);

        spriteTable.setItems(spriteData);
        spriteTable.getColumns().addAll(new FxSprite(0).tableColumns());

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
        tabPane.prefHeightProperty().bind(sceneRoot.heightProperty());

        Scene scene = new Scene(sceneRoot);
        stage.setScene(scene);
        stage.setTitle("Betamax Developer Console");
        stage.setWidth(640);
        stage.setHeight(640);
        stage.show();
        stage.setOnCloseRequest( x -> { x.consume(); LOG.info("Closing developer console forbidden"); } );
        selectedItems = spriteTable.getSelectionModel().getSelectedItems();
        setSelectedSprite(null);
        completeSingleton();
    }

    private void completeSingleton() {
        synchronized (LOCK$singleton) {
            Preconditions.checkState(!singleton.isDone());
            singleton.complete(this);
            LOG.debug("singleton completed");
        }
    }

    private Map<SpriteName,FxSprite> spritesByName = new HashMap<>();
    public void updateSpriteData(List<FxSprite> updatedFxSprites) {
        spritesByName = updatedFxSprites.stream().collect(toMap(
            FxSprite::getID,
            sprite -> sprite
        ));

        Optional<SpriteName> selectedSpriteName = getSelectedSprite();

        spriteData.setAll(updatedFxSprites);

        setSelectedSprite(selectedSpriteName.orElse(null));

        if(null!=spriteTable) {
            spriteTable.refresh();
        }
    }

    void setSelectedSprite(SpriteName selectedSprite) {
        if(null!=selectedSprite) {
            spriteTable.getSelectionModel().select(spritesByName.get(selectedSprite).getTableIndex());
        } else {
            spriteTable.getSelectionModel().clearSelection();
        }
    }

    Optional<SpriteName> getSelectedSprite() {
        int selectedIndex = spriteTable.getSelectionModel().getFocusedIndex();
        if(selectedIndex != -1) {
            return Optional.of(spriteData.get(selectedIndex).getID());
        } else {
            return Optional.empty();
        }
    }

    public void selectSpriteIndex(int index) {
        spriteTable.getSelectionModel().select(index);

    }
}

