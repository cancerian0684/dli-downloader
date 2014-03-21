package org.shunya.javafx;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import javafx.util.StringConverter;

/**
 * A simple table that uses cell factories to enable editing of boolean and
 * String values in the table.
 */
public class TableCellFactoryApp extends Application {
 
    public Parent createContent() {
        final ObservableList<PublicationVO> data = FXCollections.observableArrayList(
                new PublicationVO("Jacob", "Smith", "jacob.smith@example.com", 1, null, true),
                new PublicationVO("Isabella", "Johnson", "isabella.johnson@example.com", 2, null, true),
                new PublicationVO("Ethan", "Williams", "ethan.williams@example.com", 3, null, true),
                new PublicationVO("Ethan", "Williams", "ethan.williams@example.com", 6, null, true),
                new PublicationVO("Emma", "Jones", "emma.jones@example.com", 6, null, true),
                new PublicationVO("Michael", "Brown", "michael.brown@example.com", 6, null, true));
        StringConverter<Object> sc = new StringConverter<Object>() {
            @Override
            public String toString(Object t) {
                return t == null ? null : t.toString();
            }
 
            @Override
            public Object fromString(String string) {
                return string;
            }
        };
 
        TableColumn invitedCol = new TableColumn<>();
        invitedCol.setText("Invited");
        invitedCol.setMinWidth(70);
        invitedCol.setCellValueFactory(new PropertyValueFactory("invited"));
        invitedCol.setCellFactory(CheckBoxTableCell.forTableColumn(invitedCol));
 
        TableColumn firstNameCol = new TableColumn();
        firstNameCol.setText("First");
        firstNameCol.setCellValueFactory(new PropertyValueFactory("firstName"));
        firstNameCol.setCellFactory(TextFieldTableCell.<String, Object>forTableColumn(sc));
 
        TableColumn lastNameCol = new TableColumn();
        lastNameCol.setText("Last");
        lastNameCol.setCellValueFactory(new PropertyValueFactory("lastName"));
        lastNameCol.setCellFactory(TextFieldTableCell.<String, Object>forTableColumn(sc));
 
        TableColumn emailCol = new TableColumn();
        emailCol.setText("Email");
        emailCol.setMinWidth(200);
        emailCol.setCellValueFactory(new PropertyValueFactory("email"));
        emailCol.setCellFactory(TextFieldTableCell.<String, Object>forTableColumn(sc));
 
        TableView tableView = new TableView();
        tableView.setItems(data);
        tableView.setEditable(true);
        tableView.getColumns().addAll(invitedCol, firstNameCol, lastNameCol, emailCol);
        return tableView;
    }
 
    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(createContent()));
        primaryStage.show();
    }
 
    /**
     * Java main for when running without JavaFX launcher
     */
    public static void main(String[] args) {
        launch(args);
    }
}