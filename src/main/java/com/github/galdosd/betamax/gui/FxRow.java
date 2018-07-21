package com.github.galdosd.betamax.gui;

import javafx.scene.control.Control;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.Getter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * FIXME: Document this class
 */
public abstract class FxRow<T_id> {
    @Getter @FxRow.IgnoreColumn protected int tableIndex;

    private static TableColumn tableColumn(String columnName) {
        TableColumn tableColumn = new TableColumn(columnName);
        tableColumn.setMinWidth(10*columnName.length() + 20);
        tableColumn.setPrefWidth(Control.USE_COMPUTED_SIZE);
        tableColumn.setSortable(false);
        tableColumn.setCellValueFactory(
                new PropertyValueFactory<>(tableColumn.getText()));
        return tableColumn;
    }

    TableColumn[] tableColumns() {
        return columns(this.getClass());
    }

    static TableColumn[] columns(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.getAnnotationsByType(IgnoreColumn.class).length == 0)
                .map(Field::getName)
                .map(FxRow::tableColumn)
                .toArray(TableColumn[]::new);
    }

    public abstract T_id getID();

    @Retention(RetentionPolicy.RUNTIME)
    public @interface IgnoreColumn { }
}
