package com.github.galdosd.betamax.gui;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * FIXME: Document this class
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public final class FxSprite extends FxObject {
    SimpleStringProperty spriteName = new SimpleStringProperty();
    SimpleStringProperty template = new SimpleStringProperty();
    SimpleIntegerProperty moment = new SimpleIntegerProperty();
    SimpleIntegerProperty length = new SimpleIntegerProperty();
    SimpleIntegerProperty age = new SimpleIntegerProperty();

    public String getSpriteName() {
        return spriteName.get();
    }
    public void setSpriteName(String spriteName) {
        this.spriteName.set(spriteName);
    }

    public String getTemplate() {
        return template.get();
    }
    public void setTemplate(String template) {
        this.template.set(template);
    }

    public int getMoment() {
        return moment.get();
    }
    public void setMoment(int moment) {
        this.moment.set(moment);
    }

    public int getLength() {
        return length.get();
    }
    public void setLength(int length) {
        this.length.set(length);
    }

    public int getAge() {
        return age.get();
    }
    public void setAge(int age) {
        this.age.set(age);
    }

}
