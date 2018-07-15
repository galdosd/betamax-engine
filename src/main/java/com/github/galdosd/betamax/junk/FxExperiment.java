package com.github.galdosd.betamax.junk;

import javafx.application.Application;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.slf4j.LoggerFactory;

/**
 * FIXME: Document this class
 */
public class FxExperiment extends Application {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private final TableView<FxSprite> table = new TableView<>();
    private final ObservableList<FxSprite> data = FXCollections.observableArrayList( );

    public static void main(String[] args) {
        launch(args);
        LOG.debug("launch over");
    }

    @Override public void start(Stage stage) {

        table.setEditable(false);

        TableColumn spriteNameCol = new TableColumn("Sprite Name");
        spriteNameCol.setMinWidth(80);
        spriteNameCol.setCellValueFactory(
                new PropertyValueFactory<FxSprite, String>("spriteName"));

        TableColumn momentCol = new TableColumn("Moment");
        momentCol.setMinWidth(80);
        momentCol.setCellValueFactory(
                new PropertyValueFactory<FxSprite, Integer>("moment"));

        table.setItems(data);
        table.getColumns().addAll(spriteNameCol, momentCol);

        Pane sceneRoot = new Pane();
        table.prefWidthProperty().bind(sceneRoot.widthProperty());
        sceneRoot.getChildren().addAll(table);

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
        data.add(fxSprite);

        LOG.debug("fxsprite been added");
    }

    @FieldDefaults(makeFinal = true, level= AccessLevel.PRIVATE)
    public static class FxSprite {
        SimpleStringProperty spriteName = new SimpleStringProperty();
        SimpleStringProperty template = new SimpleStringProperty();
        SimpleIntegerProperty moment = new SimpleIntegerProperty();
        SimpleIntegerProperty length = new SimpleIntegerProperty();
        SimpleIntegerProperty age = new SimpleIntegerProperty();

        public String getSpriteName() { return spriteName.get(); }
        public void setSpriteName(String spriteName) { this.spriteName.set(spriteName); }

        public String getTemplate() { return template.get(); }
        public void setTemplate(String template) { this.template.set(template); }

        public int getMoment() { return moment.get(); }
        public void setMoment(int moment) { this.moment.set(moment); }

        public int getLength() { return length.get(); }
        public void setLength(int length) { this.length.set(length); }

        public int getAge() { return age.get(); }
        public void setAge(int age) { this.age.set(age); }
    }
}

