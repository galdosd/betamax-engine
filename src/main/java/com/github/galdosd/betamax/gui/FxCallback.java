package com.github.galdosd.betamax.gui;

import com.github.galdosd.betamax.scripting.EventType;
import com.github.galdosd.betamax.scripting.ScriptCallback;
import com.github.galdosd.betamax.sprite.SpriteEvent;
import com.github.galdosd.betamax.sprite.SpriteName;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * FIXME: Document this class
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class FxCallback extends FxRow<SpriteEvent> {
    SimpleStringProperty eventType = new SimpleStringProperty();
    SimpleStringProperty spriteName = new SimpleStringProperty();
    SimpleIntegerProperty moment  = new SimpleIntegerProperty();
    SimpleStringProperty callbackRemarks = new SimpleStringProperty();

    public FxCallback(int tableIndex, SpriteEvent spriteEvent, ScriptCallback scriptCallback){
        setCallbackRemarks(scriptCallback.toString());
        setEventType(spriteEvent.eventType.name());
        setSpriteName(spriteEvent.spriteName.getName());
        setMoment(spriteEvent.moment);
    }

    @Override public SpriteEvent getID() {
        return new SpriteEvent(EventType.valueOf(getEventType()), new SpriteName(getSpriteName()), getMoment());
    }

    @Override public int getTableIndex() {
        return 0;
    }

    public String getEventType() {
        return eventType.get();
    }
    public void setEventType(String eventType) {
        this.eventType.set(eventType);
    }

    public String getSpriteName() {
        return spriteName.get();
    }
    public void setSpriteName(String spriteName) {
        this.spriteName.set(spriteName);
    }

    public int getMoment() {
        return moment.get();
    }
    public void setMoment(int moment) {
        this.moment.set(moment);
    }

    public String getCallbackRemarks() {
        return callbackRemarks.get();
    }
    public void setCallbackRemarks(String callbackRemarks) {
        this.callbackRemarks.set(callbackRemarks);
    }
}
