package com.github.galdosd.betamax.gui;

import com.github.galdosd.betamax.sprite.Sprite;
import com.github.galdosd.betamax.sprite.SpriteName;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * FIXME: Document this class
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public final class FxSprite extends FxRow<SpriteName> {
    SimpleStringProperty spriteName = new SimpleStringProperty();
    SimpleStringProperty template = new SimpleStringProperty();
    SimpleIntegerProperty moment = new SimpleIntegerProperty();
    @ColumnWidth(60)
    SimpleIntegerProperty length = new SimpleIntegerProperty();
    SimpleIntegerProperty age = new SimpleIntegerProperty();
    SimpleIntegerProperty layer = new SimpleIntegerProperty();
    @ColumnWidth(60)
    SimpleIntegerProperty serial = new SimpleIntegerProperty();
    SimpleIntegerProperty reps = new SimpleIntegerProperty();
    SimpleBooleanProperty hidden = new SimpleBooleanProperty();
    SimpleBooleanProperty paused = new SimpleBooleanProperty();
    @ColumnWidth(60)
    SimpleIntegerProperty sndLvl = new SimpleIntegerProperty();
    SimpleStringProperty location = new SimpleStringProperty();
    @ColumnWidth(200)
    SimpleStringProperty soundRemarks = new SimpleStringProperty();
    SimpleStringProperty soundDrift = new SimpleStringProperty();

    public FxSprite(int tableIndex, Sprite sprite) {
        this.tableIndex = tableIndex;
        setSpriteName( sprite.getName().getName() );
        setTemplate( sprite.getTemplateName() );
        setMoment( sprite.getCurrentFrame() );
        setLength( sprite.getTotalFrames() );
        setAge( sprite.getAge() );
        setSerial( sprite.getCreationSerial() );
        setLayer( sprite.getLayer() );
        setReps(sprite.getRepetitions());
        setHidden(sprite.getHidden());
        setPaused(sprite.getPaused());
        setSndLvl(sprite.getSoundPauseLevel());
        setLocation(sprite.getLocation().toShortString());
        setSoundRemarks( sprite.getSoundRemarks() );
        setSoundDrift( String.format("%+.3fs", sprite.getSoundDrift()) );
    }

    @Override public SpriteName getID() {
        return new SpriteName(getSpriteName());
    }

    public String getSoundDrift() { return soundDrift.get(); }
    public void setSoundDrift(String soundDrift) { this.soundDrift.set(soundDrift); }

    public String getLocation() { return location.get(); }
    public void setLocation(String location) { this.location.set(location); }

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

    public int getReps() {
        return reps.get();
    }
    public void setReps(int reps) {
        this.reps.set(reps);
    }

    public boolean getHidden() {
        return hidden.get();
    }
    public void setHidden(boolean hidden) {
        this.hidden.set(hidden);
    }

    public boolean getPaused() {
        return paused.get();
    }
    public void setPaused(boolean paused) {
        this.paused.set(paused);
    }

    public int getSndLvl() {
        return sndLvl.get();
    }
    public void setSndLvl(int sndLvl) {
        this.sndLvl.set(sndLvl);
    }

    public String getSoundRemarks() { return soundRemarks.get(); }
    public void setSoundRemarks(String soundRemarks) { this.soundRemarks.set(soundRemarks); }
}
