package org.shunya.javafx;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
 
public class SearchBox extends Region {

    private final TabPaneApp tabPaneApp;
    private TextField textBox;
    private Button clearButton;
 
    public SearchBox(TabPaneApp tabPaneApp) {
        this.tabPaneApp = tabPaneApp;
        setId("SearchBox");
//        getStyleClass().add("search-box");
        setMinHeight(30);
        setPrefSize(420, 30);
        setMaxSize(Control.USE_COMPUTED_SIZE, Control.USE_COMPUTED_SIZE);
        textBox = new TextField();
        textBox.setPromptText("Search");
        clearButton = new Button();
        clearButton.setVisible(false);
        getChildren().addAll(textBox);
        clearButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                textBox.setText("");
                textBox.requestFocus();
            }
        });
        textBox.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                clearButton.setVisible(textBox.getText().length() != 0);
                System.out.println("Textbox listener called");
                tabPaneApp.onQueryChange(textBox.getText());
            }
        });
    }
 
    @Override
    protected void layoutChildren() {
        textBox.resize(getWidth(), getHeight());
//        clearButton.resizeRelocate(getWidth() - 18, 6, 12, 13);
    }
}