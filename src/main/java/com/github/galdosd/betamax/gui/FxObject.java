package com.github.galdosd.betamax.gui;

import javafx.scene.control.Control;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * FIXME: Document this class
 */
public class FxObject {
    private static TableColumn tableColumn(String columnName) {
        TableColumn tableColumn = new TableColumn(columnName);
        tableColumn.setMinWidth(10*columnName.length() + 20);
        tableColumn.setPrefWidth(Control.USE_COMPUTED_SIZE);
        tableColumn.setCellValueFactory(
                new PropertyValueFactory<>(tableColumn.getText()));
        return tableColumn;
    }

    public TableColumn[] tableColumns() {
        return columns(this.getClass());
    }

    private static TableColumn[] columns(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName)
                .map(FxObject::tableColumn)
                .toArray(TableColumn[]::new);
//        return new TableColumn[]{
//                tableColumn("spriteName"),
//                tableColumn("template"),
//                tableColumn("moment"),
//                tableColumn("length"),
//                tableColumn("age"),
//        };
    }
}
