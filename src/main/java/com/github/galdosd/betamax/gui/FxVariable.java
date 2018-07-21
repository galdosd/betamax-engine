package com.github.galdosd.betamax.gui;

import javafx.beans.property.SimpleStringProperty;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * FIXME: Document this class
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class FxVariable extends FxRow<String> {
    SimpleStringProperty key = new SimpleStringProperty();
    SimpleStringProperty value = new SimpleStringProperty();

    public FxVariable(int tableIndex, String key, String value) {
        this.tableIndex = tableIndex;
        setKey(key);
        setValue(value);
    }

    public String getKey() {
        return key.get();
    }
    public void setKey(String key) {
        this.key.set(key);
    }

    public String getValue() {
        return value.get();
    }
    public void setValue(String value) {
        this.value.set(value);
    }

    @Override public String getID() {
        return getKey();
    }
}
