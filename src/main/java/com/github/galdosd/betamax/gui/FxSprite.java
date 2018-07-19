package com.github.galdosd.betamax.gui;

import com.github.galdosd.betamax.sprite.Sprite;
import com.github.galdosd.betamax.sprite.SpriteName;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * FIXME: Document this class
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public final class FxSprite extends FxObject {
    @Getter @FxObject.IgnoreColumn final int tableIndex;

    SimpleStringProperty spriteName = new SimpleStringProperty();
    SimpleStringProperty template = new SimpleStringProperty();
    SimpleIntegerProperty moment = new SimpleIntegerProperty();
    SimpleIntegerProperty length = new SimpleIntegerProperty();
    SimpleIntegerProperty age = new SimpleIntegerProperty();
    SimpleIntegerProperty layer = new SimpleIntegerProperty();
    SimpleIntegerProperty serial = new SimpleIntegerProperty();
    SimpleIntegerProperty repetitions = new SimpleIntegerProperty();


    public FxSprite(int tableIndex) {
        this.tableIndex = tableIndex;
    }

    public SpriteName getID() {
        return new SpriteName(getSpriteName());
    }

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

    public int getLayer() {
        return layer.get();
    }
    public void setLayer(int layer) {
        this.layer.set(layer);
    }

    public int getSerial() {
        return serial.get();
    }
    public void setSerial(int serial) {
        this.serial.set(serial);
    }

    public int getRepetitions() {
        return repetitions.get();
    }
    public void setRepetitions(int repetitions) {
        this.repetitions.set(repetitions);
    }

    public void load(Sprite sprite) {
        setSpriteName( sprite.getName().getName() );
        setTemplate( sprite.getTemplateName() );
        setMoment( sprite.getCurrentFrame() );
        setLength( sprite.getTotalFrames() );
        setAge( sprite.getAge() );
        setSerial( sprite.getCreationSerial() );
        setLayer( sprite.getLayer() );
        setRepetitions(sprite.getRepetitions());
    }
}
