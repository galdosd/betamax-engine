package com.github.galdosd.betamax.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;
import lombok.Getter;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

/**
 * FIXME: Document this class
 */
public class FxTable<T_row extends FxRow<T_id>, T_id> {

    private TableView<T_row> table;
    private final ObservableList<T_row> rowData = FXCollections.observableArrayList( );
    @Getter ObservableList<T_row> selectedItems;
    private Map<T_id,T_row> rowsById = new HashMap<>();

    public Tab start(Pane sceneRoot, TableColumn[] columns, String title) {
        table = new TableView<>();

        table.setEditable(false);

        table.setItems(rowData);
        table.getColumns().addAll(columns);

        table.prefWidthProperty().bind(sceneRoot.widthProperty());

        Tab spritesTab = new Tab();
        spritesTab.setClosable(false);
        spritesTab.setText(title);
        spritesTab.setContent(table);

        selectedItems = table.getSelectionModel().getSelectedItems();
        setSelectedSprite(null);
        return spritesTab;

    }

    public void updateSpriteData(List<T_row> updatedFxRows) {
        Stream<T_row> stream = updatedFxRows.stream();
        rowsById = stream.collect(toMap(
                T_row::getID,
                row -> row
        ));

        Optional<T_id> selectedSpriteName = getSelectedSprite();

        rowData.setAll(updatedFxRows);

        setSelectedSprite(selectedSpriteName.orElse(null));

        if(null!= table) {
            table.refresh();
        }
    }

    void setSelectedSprite(T_id selectedSprite) {
        if(null!=selectedSprite && null!= rowsById.get(selectedSprite)) {
            table.getSelectionModel().select(rowsById.get(selectedSprite).getTableIndex());
        } else {
            table.getSelectionModel().clearSelection();
        }
    }

    Optional<T_id> getSelectedSprite() {
        int selectedIndex = table.getSelectionModel().getFocusedIndex();
        if(selectedIndex != -1) {
            return Optional.of(rowData.get(selectedIndex).getID());
        } else {
            return Optional.empty();
        }
    }
}
