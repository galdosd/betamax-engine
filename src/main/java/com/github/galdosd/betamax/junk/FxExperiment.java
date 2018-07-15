package com.github.galdosd.betamax.junk;

import javafx.application.Application;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
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

    public static void main(String[] args) {
        launch(args);
        LOG.debug("launch over");
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("BETAMAX SCRIPT CONSOLE");
        Button btn = new Button();
        btn.setText("Say 'Hello World'");
        btn.setOnAction(event -> LOG.debug("hi word"));

        StackPane root = new StackPane();
        root.getChildren().add(btn);
        primaryStage.setScene(new Scene(root, 300, 250));
        primaryStage.show();
        LOG.debug("scene been shown");
    }

    @FieldDefaults(makeFinal = true, level= AccessLevel.PRIVATE)
    public static class FxSprite {
        SimpleStringProperty name;
        SimpleStringProperty template;
        SimpleIntegerProperty moment;
        SimpleIntegerProperty length;
        SimpleIntegerProperty age;

        public String getName() { return name.get(); }
        public void setName(String name) { this.name.set(name); }

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

