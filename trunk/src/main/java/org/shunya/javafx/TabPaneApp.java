package org.shunya.javafx;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.shunya.dli.AppContext;
import org.shunya.dli.LuceneIndexer;
import org.shunya.dli.LuceneSearcher;
import org.shunya.dli.Utils;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static org.shunya.dli.AppConstants.DLI_SETTINGS_XML;

public class TabPaneApp extends Application {
    private TabPane tabPane;
    private Tab tab1;
    private Tab tab2;
    private Tab tab3;
    private Tab internalTab;
    private ObservableList<PublicationVO> data;
    private LuceneSearcher searcher;
    private LuceneIndexer indexer;
    private AppContext appContext;
    private TableView tableView;
    private TableView<TestTask> downloadTable;

    public Parent createContent() throws InstantiationException, IllegalAccessException, IOException, ParseException {
        appContext = Utils.load(AppContext.class, DLI_SETTINGS_XML);
        indexer = new LuceneIndexer(appContext);
        searcher = new LuceneSearcher(indexer.getWriter());
        //Each tab illustrates different capabilities
        tabPane = new TabPane();
        tabPane.setPrefSize(420, 580);
        tabPane.setMinSize(TabPane.USE_PREF_SIZE, TabPane.USE_PREF_SIZE);
        tabPane.setMaxSize(TabPane.USE_PREF_SIZE, TabPane.USE_PREF_SIZE);
        tab1 = new Tab();
        tab2 = new Tab();
        tab3 = new Tab();
        internalTab = new Tab();

        tabPane.setRotateGraphic(false);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setSide(Side.TOP);
        String searchBoxCss = TabPaneApp.class.getResource("/SearchBox.css").toExternalForm();
        final VBox vbox = new VBox();
        vbox.getStylesheets().add(searchBoxCss);
        vbox.setSpacing(5);
        vbox.setTranslateX(1);
        vbox.setTranslateY(1);
        vbox.setFillWidth(true);
        // Initial tab with buttons for experimenting
        tab1.setText("Search");
        tab1.setTooltip(new Tooltip("Tab 1 Tooltip"));
        final Image image = new Image(getClass().getClassLoader().getResourceAsStream("tab_16.png"));
        final ImageView imageView = new ImageView();
        imageView.setImage(image);
//        tab1.setGraphic(imageView);

        setUpControlButtons(vbox);

        BorderPane root = new BorderPane();
        root.getStylesheets().add(searchBoxCss);
        root.setTop(new SearchBox(this));
        root.setCenter(createTable());

        tab1.setContent(root);
        tabPane.getTabs().add(tab1);
        // Tab2 has longer label and toggles tab closing 
        tab2.setText("Downloads");
        final VBox vboxLongTab = new VBox();
        vboxLongTab.setSpacing(3);
        vboxLongTab.setTranslateX(5);
        vboxLongTab.setTranslateY(5);

        Label explainRadios = new Label("Closing policy for tabs:");
        vboxLongTab.getChildren().add(explainRadios);
        ToggleGroup closingPolicy = new ToggleGroup();

        for (TabPane.TabClosingPolicy policy : TabPane.TabClosingPolicy.values()) {
            final RadioButton radioButton = new RadioButton(policy.name());
            radioButton.setMnemonicParsing(false);
            radioButton.setToggleGroup(closingPolicy);
            radioButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.valueOf(radioButton.getText()));
                }
            });
            if (policy.name().equals(TabPane.TabClosingPolicy.SELECTED_TAB.name())) {
                radioButton.setSelected(true);
            }
            vboxLongTab.getChildren().add(radioButton);
        }
        tab2.setContent(createDownloadTable());
        tabPane.getTabs().add(tab2);
        // Tab 3 has a checkbox for showing/hiding tab labels
        tab3.setText("Settings");
        final VBox vboxTab3 = new VBox();
        vboxTab3.setSpacing(3);
        vboxTab3.setTranslateX(5);
        vboxTab3.setTranslateY(5);

        final CheckBox cb = new CheckBox("Show labels on original tabs");
        cb.setSelected(true);
        cb.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (cb.isSelected()) {
                    tab1.setText("Tab 1");
                    tab2.setText("Longer Tab");
                    tab3.setText("Tab 3");
                    internalTab.setText("Internal Tabs");

                } else {
                    tab1.setText("");
                    tab2.setText("");
                    tab3.setText("");
                    internalTab.setText("");
                }
            }
        });
        vboxTab3.getChildren().add(cb);
        tab3.setContent(vboxTab3);
        tabPane.getTabs().add(tab3);
        //Internal Tabs   
        internalTab.setText("About Us");
        setupInternalTab();
        tabPane.getTabs().add(internalTab);
        return tabPane;
    }

    private void toggleTabPosition(TabPane tabPane) {
        Side pos = tabPane.getSide();
        if (pos == Side.TOP) {
            tabPane.setSide(Side.RIGHT);
        } else if (pos == Side.RIGHT) {
            tabPane.setSide(Side.BOTTOM);
        } else if (pos == Side.BOTTOM) {
            tabPane.setSide(Side.LEFT);
        } else {
            tabPane.setSide(Side.TOP);
        }
    }

    private void toggleTabMode(TabPane tabPane) {
        if (!tabPane.getStyleClass().contains(TabPane.STYLE_CLASS_FLOATING)) {
            tabPane.getStyleClass().add(TabPane.STYLE_CLASS_FLOATING);
        } else {
            tabPane.getStyleClass().remove(TabPane.STYLE_CLASS_FLOATING);
        }
    }

    private void setupInternalTab() {
        StackPane internalTabContent = new StackPane();
        final TabPane internalTabPane = new TabPane();
        internalTabPane.getStyleClass().add(TabPane.STYLE_CLASS_FLOATING);
        internalTabPane.setSide(Side.LEFT);

        internalTabPane.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        final Tab innerTab = new Tab();
        innerTab.setText("Tab 1");
        final VBox innerVbox = new VBox();
        innerVbox.setSpacing(2);
        innerVbox.setTranslateX(5);
        innerVbox.setTranslateY(5);
        Button innerTabPosButton = new Button("Toggle Tab Position");
        innerTabPosButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                toggleTabPosition(internalTabPane);
            }
        });
        innerVbox.getChildren().add(innerTabPosButton);
        {
            Button innerTabModeButton = new Button("Toggle Tab Mode");
            innerTabModeButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    toggleTabMode(internalTabPane);
                }
            });
            innerVbox.getChildren().add(innerTabModeButton);
        }
        innerTab.setContent(innerVbox);
        internalTabPane.getTabs().add(innerTab);

        for (int i = 1; i < 5; i++) {
            Tab tab = new Tab();
            tab.setText("Tab " + i);
            tab.setContent(new Region());
            internalTabPane.getTabs().add(tab);
        }
        internalTabContent.getChildren().add(internalTabPane);
        internalTab.setContent(internalTabContent);
    }

    private void setUpControlButtons(VBox vbox) {
        // Toggle style class floating
        vbox.getChildren().add(new SearchBox(this));
        vbox.getChildren().add(createTable());
        final Button tabModeButton = new Button("Toggle Tab Mode");
        tabModeButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                toggleTabMode(tabPane);
            }
        });
//        vbox.getChildren().add(tabModeButton);
        // Tab position
        final Button tabPositionButton = new Button("Toggle Tab Position");
        tabPositionButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                toggleTabPosition(tabPane);
            }
        });
        // Add tab and switch to it
        final Button newTabButton = new Button("Switch to New Tab");
        newTabButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                Tab t = new Tab("Testing");
                t.setContent(new Button("Howdy"));
                tabPane.getTabs().add(t);
                tabPane.getSelectionModel().select(t);
            }
        });
//        vbox.getChildren().add(newTabButton);
        // Add tab
        final Button addTabButton = new Button("Add Tab");
        addTabButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                Tab t = new Tab("New Tab");
                t.setContent(new Region());
                tabPane.getTabs().add(t);
            }
        });
//        vbox.getChildren().add(addTabButton);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        setUserAgentStylesheet(STYLESHEET_MODENA);
        primaryStage.setFullScreen(false);
        primaryStage.setTitle("DLI Downloader");
        primaryStage.setScene(new Scene(createContent()));
        primaryStage.getIcons().add(new Image(getClass().getClassLoader().getResourceAsStream("tab_16.png")));
        primaryStage.show();
    }

    public void onQueryChange(String query) {
        try {
            final List<org.shunya.dli.Publication> publications = searcher.search(query, 50);
            tableView.getItems().clear();
            int i = 0;
            for (org.shunya.dli.Publication publication : publications) {
                i++;
                boolean available = false;
                if (publication.getLocalPath() != null && !publication.getLocalPath().isEmpty())
                    available = true;
                tableView.getItems().add(new PublicationVO(publication.getBarcode(), publication.getAuthor(), publication.getTitle(), i, publication, available));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Parent createTable() {
        data = FXCollections.observableArrayList();
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
        invitedCol.setText("Select");
        invitedCol.setPrefWidth(40);
        invitedCol.setCellValueFactory(new PropertyValueFactory("seq"));
        invitedCol.setCellFactory(TextFieldTableCell.<String, Object>forTableColumn(sc));

//        TableColumn firstNameCol = new TableColumn();
//        firstNameCol.setText("Barcode");
//        firstNameCol.setCellValueFactory(new PropertyValueFactory("barcode"));
//        firstNameCol.setCellFactory(TextFieldTableCell.<String, Object>forTableColumn(sc));
//
//        TableColumn lastNameCol = new TableColumn();
//        lastNameCol.setText("Author");
//        lastNameCol.setCellValueFactory(new PropertyValueFactory("author"));
//        lastNameCol.setCellFactory(TextFieldTableCell.<String, Object>forTableColumn(sc));

        TableColumn titleColumn = new TableColumn();
        titleColumn.setText("Title");
        titleColumn.setPrefWidth(350);
//        titleColumn.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4)");
        titleColumn.setCellValueFactory(new PropertyValueFactory("title"));
//        titleColumn.setCellFactory(TextFieldTableCell.<String, Object>forTableColumn(sc));
        titleColumn.setCellFactory(new Callback<TableColumn<PublicationVO, String>, TableCell<PublicationVO, String>>() {
            @Override
            public TableCell<PublicationVO, String> call(TableColumn<PublicationVO, String> param) {
                final TableCell<PublicationVO, String> cell = new TableCell<PublicationVO, String>() {

                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (!isEmpty()) {
//                            if(item ==null)
//                                item="";
//                            text = new Text(item.toString());
//                            text.setWrappingWidth(titleColumn.getWidth()); // Setting the wrapping width to the Text
//                            setGraphic(text);
                            setGraphic(null);
                            setText(item);
                            TableRow currentRow = getTableRow();
                            PublicationVO currentPub = currentRow == null ? null : (PublicationVO) currentRow.getItem();
                            if (currentPub.localProperty().get())
                                setStyle("-fx-text-fill: #c71d32;-fx-wrap-text: true;");
                            else
                                setStyle("-fx-text-fill: #0d48a3;-fx-wrap-text: true;");
//                            setWrapText(true);
                        } else {
                            setGraphic(null);
                            setText(null);
                        }
                    }
                };
                return cell;
            }
        });

        tableView = new TableView();
        tableView.setItems(data);
        tableView.setEditable(true);
        tableView.getColumns().addAll(invitedCol, titleColumn);

        ContextMenu menu = new ContextMenu();
        MenuItem item = new MenuItem("Open PDF");
        item.setOnAction(event -> {
            PublicationVO selectedItem = (PublicationVO) tableView.getSelectionModel().getSelectedItem();
            System.out.println("selectedItem = " + selectedItem.getPublication());
            if (selectedItem.getPublication().getLocalPath() != null) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().open(Paths.get(selectedItem.getPublication().getLocalPath()).toFile());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        menu.getItems().add(item);

        MenuItem downloadItem = new MenuItem("Download");
        downloadItem.setOnAction(event -> {
            System.out.println("you've clicked the menu item");
        });
        menu.getItems().add(downloadItem);

        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableView.setContextMenu(menu);

        return tableView;
    }


    public Parent createDownloadTable() {
        downloadTable = new TableView<>();
        Random rng = new Random();
        for (int i = 0; i < 10; i++) {
            downloadTable.getItems().add(new TestTask(rng.nextInt(3000) + 2000, rng.nextInt(30) + 20));
        }

        TableColumn<TestTask, String> statusCol = new TableColumn("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("message"));
        statusCol.setPrefWidth(100);

        TableColumn<TestTask, Double> progressCol = new TableColumn("Progress");
        progressCol.setCellValueFactory(new PropertyValueFactory<>("progress"));
        progressCol.setCellFactory(ProgressIndicatorTableCell.<TestTask>forTableColumn());
        progressCol.setPrefWidth(150);

        downloadTable.getColumns().addAll(statusCol, progressCol);

        BorderPane root = new BorderPane();
        root.setTop(new TextField("dummy barcode"));
        root.setCenter(downloadTable);

        ExecutorService executor = Executors.newFixedThreadPool(5, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        });

        for (TestTask task : downloadTable.getItems()) {
            executor.execute(task);
        }

        return root;
    }

    static class TestTask extends Task<Void> {
        private final int waitTime; // milliseconds
        private final int pauseTime; // milliseconds
        public static final int NUM_ITERATIONS = 100;

        TestTask(int waitTime, int pauseTime) {
            this.waitTime = waitTime;
            this.pauseTime = pauseTime;
            this.updateProgress(0.0, 1);
            this.updateMessage("Waiting...");
        }

        @Override
        protected Void call() throws Exception {
            this.updateProgress(0.0, 1);
            this.updateMessage("Waiting...");
            Thread.sleep(waitTime);
            this.updateMessage("Running...");
            for (int i = 0; i < NUM_ITERATIONS; i++) {
                updateProgress((1.0 * i) / NUM_ITERATIONS, 1);
                Thread.sleep(pauseTime);
            }
            this.updateMessage("Done");
            this.updateProgress(1, 1);
            return null;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}