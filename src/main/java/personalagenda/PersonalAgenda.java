package personalagenda;

import personalagenda.dao.EventDAO;
import personalagenda.model.Event;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.animation.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class PersonalAgenda extends Application {
    
    private EventDAO eventDAO = new EventDAO();
    private ObservableList<Event> eventList = FXCollections.observableArrayList();
    private ObservableList<Event> filteredList = FXCollections.observableArrayList();
    private ListView<Event> listView;
    
    private TextField searchField;
    private ComboBox<String> searchTypeCombo;
    private DatePicker searchDatePicker;
    private ToggleGroup filterGroup;
    
    private Label statTotalLabel;
    private Label statTodayLabel;
    private Label statWeekLabel;
    private Label statImportantLabel;
    private Label statusLabel;
    
    private Label toastLabel;
    private Timeline toastTimeline;
    
    private String currentFilter = "Tous";
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("📅 Agenda Personnel - Gestionnaire d'Événements");
        primaryStage.setMaximized(true);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(768);
        
        MenuBar menuBar = createMenuBar();
        VBox header = createHeader();
        
        HBox mainContent = new HBox();
        mainContent.setPadding(new Insets(15));
        mainContent.setSpacing(20);
        
        VBox sidebar = createSidebar();
        VBox centerContent = createCenterContent();
        
        mainContent.getChildren().addAll(sidebar, centerContent);
        HBox.setHgrow(centerContent, Priority.ALWAYS);
        
        HBox statusBar = createStatusBar();
        
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #F1F5F9;");
        root.getChildren().addAll(menuBar, header, mainContent, statusBar);
        
        StackPane overlayRoot = new StackPane();
        overlayRoot.getChildren().addAll(root, toastLabel);
        StackPane.setAlignment(toastLabel, Pos.BOTTOM_CENTER);
        StackPane.setMargin(toastLabel, new Insets(0, 0, 30, 0));
        
        Scene scene = new Scene(overlayRoot);
        
        scene.setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.N) {
                openAddEventDialog();
            } else if (event.isControlDown() && event.getCode() == KeyCode.F) {
                searchField.requestFocus();
            } else if (event.getCode() == KeyCode.F5) {
                loadAllEvents();
            } else if (event.isControlDown() && event.getCode() == KeyCode.Q) {
                confirmExit(primaryStage);
            } else if (event.getCode() == KeyCode.F11) {
                primaryStage.setFullScreen(!primaryStage.isFullScreen());
            }
        });
        
        primaryStage.setScene(scene);
        primaryStage.show();
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
        
        loadAllEvents();
        updateStatistics();
        
        Timeline autoRefresh = new Timeline(new KeyFrame(Duration.minutes(5), e -> {
            loadAllEvents();
            showToast("🔄 Agenda actualisé", "#6366F1");
        }));
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();
    }
    
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");
        
        Menu fileMenu = new Menu("📁 Fichier");
        MenuItem newEventItem = new MenuItem("Nouvel événement");
        newEventItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        newEventItem.setOnAction(e -> openAddEventDialog());
        
        MenuItem importItem = new MenuItem("Importer (ICS)");
        MenuItem exportItem = new MenuItem("Exporter (CSV)");
        SeparatorMenuItem sep1 = new SeparatorMenuItem();
        MenuItem exitItem = new MenuItem("Quitter");
        exitItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
        exitItem.setOnAction(e -> confirmExit((Stage) menuBar.getScene().getWindow()));
        
        fileMenu.getItems().addAll(newEventItem, importItem, exportItem, sep1, exitItem);
        
        Menu viewMenu = new Menu("👁 Affichage");
        MenuItem refreshItem = new MenuItem("Actualiser");
        refreshItem.setAccelerator(new KeyCodeCombination(KeyCode.F5));
        refreshItem.setOnAction(e -> loadAllEvents());
        
        CheckMenuItem fullScreenItem = new CheckMenuItem("Plein écran");
        fullScreenItem.setAccelerator(new KeyCodeCombination(KeyCode.F11));
        fullScreenItem.setOnAction(e -> {
            Stage stage = (Stage) menuBar.getScene().getWindow();
            stage.setFullScreen(fullScreenItem.isSelected());
        });
        
        viewMenu.getItems().addAll(refreshItem, new SeparatorMenuItem(), fullScreenItem);
        
        Menu helpMenu = new Menu("❓ Aide");
        MenuItem aboutItem = new MenuItem("À propos");
        aboutItem.setOnAction(e -> showAboutDialog());
        MenuItem shortcutsItem = new MenuItem("Raccourcis clavier");
        shortcutsItem.setOnAction(e -> showShortcutsDialog());
        
        helpMenu.getItems().addAll(aboutItem, shortcutsItem);
        
        menuBar.getMenus().addAll(fileMenu, viewMenu, helpMenu);
        return menuBar;
    }
    
    private VBox createHeader() {
        VBox header = new VBox();
        header.setStyle("-fx-background-color: #4F46E5; -fx-padding: 20 20;");
        
        Label titleLabel = new Label("📅 Agenda Personnel");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        Label subtitleLabel = new Label("Gérez vos événements efficacement");
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #C7D2FE;");
        
        Label dateTimeLabel = new Label();
        dateTimeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
        
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            dateTimeLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy"))
                    + " - " + java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
        
        BorderPane headerPane = new BorderPane();
        HBox leftBox = new HBox(20, titleLabel, subtitleLabel);
        headerPane.setLeft(leftBox);
        headerPane.setRight(dateTimeLabel);
        
        header.getChildren().add(headerPane);
        return header;
    }
    
    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);");
        sidebar.setPrefWidth(260);
        sidebar.setSpacing(10);
        
        Label sectionTitle = new Label("📊 STATISTIQUES");
        sectionTitle.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px; -fx-font-weight: bold;");
        
        VBox statsBox = new VBox(12);
        statsBox.setAlignment(Pos.CENTER);
        
        VBox totalCard = createStatCard("📋", "Total", "0", "#6366F1");
        VBox todayCard = createStatCard("📅", "Aujourd'hui", "0", "#10B981");
        VBox weekCard = createStatCard("📆", "Semaine", "0", "#F59E0B");
        VBox importantCard = createStatCard("⭐", "Importants", "0", "#EF4444");
        
        statTotalLabel = (Label) totalCard.getChildren().get(1);
        statTodayLabel = (Label) todayCard.getChildren().get(1);
        statWeekLabel = (Label) weekCard.getChildren().get(1);
        statImportantLabel = (Label) importantCard.getChildren().get(1);
        
        HBox statsRow1 = new HBox(12, totalCard, todayCard);
        HBox statsRow2 = new HBox(12, weekCard, importantCard);
        statsRow1.setAlignment(Pos.CENTER);
        statsRow2.setAlignment(Pos.CENTER);
        
        statsBox.getChildren().addAll(statsRow1, statsRow2);
        
        Label filtersTitle = new Label("🔍 FILTRES RAPIDES");
        filtersTitle.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px; -fx-font-weight: bold;");
        filtersTitle.setPadding(new Insets(10, 0, 5, 0));
        
        VBox filtersBox = new VBox(8);
        filterGroup = new ToggleGroup();
        
        String[] filters = {"Tous", "Aujourd'hui", "Cette semaine", "Importants", "À venir"};
        for (String filter : filters) {
            RadioButton filterBtn = new RadioButton(filter);
            filterBtn.setToggleGroup(filterGroup);
            filterBtn.setStyle("-fx-text-fill: #334155; -fx-font-size: 13px;");
            filterBtn.setSelected(filter.equals("Tous"));
            final String f = filter;
            filterBtn.setOnAction(e -> applyQuickFilter(f));
            filtersBox.getChildren().add(filterBtn);
        }
        
        Label categoriesTitle = new Label("📰  CATÉGORIES");
       
        categoriesTitle.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px; -fx-font-weight: bold;");
        categoriesTitle.setPadding(new Insets(15, 0, 5, 0));
        
        VBox categoriesBox = new VBox(5);
        
        String[][] categories = {
            {"💼 Travail", "Travail"}, {"👤 Personnel", "Personnel"},
            {"📚 Étude", "Étude"}, {"🏥 Santé", "Santé"},
            {"👨‍👩‍👧 Famille", "Famille"}, {"🎮 Loisirs", "Loisirs"},
            {"🛒 Courses", "Courses"}, {"⚙️ Autre", "Autre"}
        };
        
        for (String[] cat : categories) {
            Hyperlink catLink = new Hyperlink(cat[0]);
            catLink.setStyle("-fx-text-fill: #334155; -fx-underline: false; -fx-font-size: 13px; -fx-padding: 5 10;");
            catLink.setOnAction(e -> {
                searchField.setText(cat[1]);
                searchTypeCombo.setValue("Par Catégorie");
                searchEvents();
            });
            categoriesBox.getChildren().add(catLink);
        }
        
        sidebar.getChildren().addAll(sectionTitle, statsBox, new Separator(), filtersTitle, filtersBox, categoriesTitle, categoriesBox);
        return sidebar;
    }
    
    private VBox createStatCard(String emoji, String label, String value, String color) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12, 8, 12, 8));
        card.setMinWidth(100);
        card.setPrefWidth(100);
        card.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 14px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 6, 0, 0, 1);");
        
        Label emojiLabel = new Label(emoji);
        emojiLabel.setStyle("-fx-font-size: 24px;");
        
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        Label nameLabel = new Label(label);
        nameLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 600; -fx-text-fill: rgba(255,255,255,0.9);");
        nameLabel.setWrapText(true);
        
        card.getChildren().addAll(emojiLabel, valueLabel, nameLabel);
        
        card.setOnMouseEntered(e -> {
            card.setScaleX(1.03);
            card.setScaleY(1.03);
            card.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 14px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2);");
        });
        card.setOnMouseExited(e -> {
            card.setScaleX(1.0);
            card.setScaleY(1.0);
            card.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 14px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 6, 0, 0, 1);");
        });
        
        return card;
    }
    
    private VBox createCenterContent() {
        VBox center = new VBox(15);
        center.setPadding(new Insets(0));
        
        HBox searchBox = createSearchBox();
        
        listView = new ListView<>(filteredList);
        listView.setStyle("-fx-background-color: transparent;");
        listView.setPrefHeight(400);
        listView.setCellFactory(lv -> new EventCell());
        
        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Event selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openEditEventDialog(selected);
                }
            }
        });
        
        Label listTitle = new Label("📋 LISTE DES ÉVÉNEMENTS");
        listTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1E293B;");
        
        TitledPane addPane = createAddEventPane();
        addPane.setAnimated(true);
        
        center.getChildren().addAll(searchBox, listTitle, listView, addPane);
        return center;
    }
    
    private HBox createSearchBox() {
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(0, 0, 10, 0));
        
        searchField = new TextField();
        searchField.setPromptText("Rechercher un événement...");
        searchField.setPrefWidth(300);
        searchField.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 8 12;");
        
        searchField.textProperty().addListener((obs, old, val) -> {
            if (searchField.isVisible() && !searchTypeCombo.getValue().equals("Par Date")) {
                searchEvents();
            }
        });
        
        searchTypeCombo = new ComboBox<>();
        searchTypeCombo.getItems().addAll("Par Titre", "Par Catégorie", "Par Date");
        searchTypeCombo.setValue("Par Titre");
        searchTypeCombo.setPrefWidth(130);
        searchTypeCombo.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-radius: 10px;");
        
        searchDatePicker = new DatePicker();
        searchDatePicker.setPromptText("Choisir une date");
        searchDatePicker.setVisible(false);
        searchDatePicker.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-radius: 10px;");
        searchDatePicker.setOnAction(e -> searchEvents());
        
        searchTypeCombo.setOnAction(e -> {
            boolean isDateSearch = searchTypeCombo.getValue().equals("Par Date");
            searchDatePicker.setVisible(isDateSearch);
            searchField.setVisible(!isDateSearch);
            if (isDateSearch && searchDatePicker.getValue() != null) {
                searchEvents();
            }
        });
        
        Button searchBtn = new Button("🔍 Rechercher");
        searchBtn.setStyle("-fx-background-color: #6366F1; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10px; -fx-padding: 8 16;");
        searchBtn.setOnAction(e -> searchEvents());
        
        Button resetBtn = new Button("Réinitialiser");
        resetBtn.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #334155; -fx-background-radius: 10px; -fx-padding: 8 16;");
        resetBtn.setOnAction(e -> {
            searchField.clear();
            searchDatePicker.setValue(null);
            searchTypeCombo.setValue("Par Titre");
            loadAllEvents();
        });
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button addQuickBtn = new Button("+ Nouvel événement");
        addQuickBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10px; -fx-padding: 8 20;");
        addQuickBtn.setOnAction(e -> openAddEventDialog());
        
        searchBox.getChildren().addAll(searchField, searchDatePicker, searchTypeCombo, searchBtn, resetBtn, spacer, addQuickBtn);
        return searchBox;
    }
    
    private TitledPane createAddEventPane() {
        GridPane formPane = new GridPane();
        formPane.setHgap(15);
        formPane.setVgap(12);
        formPane.setPadding(new Insets(15));
        formPane.setStyle("-fx-background-color: white; -fx-background-radius: 12px;");
        
        TextField titleField = new TextField();
        titleField.setPromptText("Ex: Réunion importante");
        titleField.setStyle("-fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-padding: 8;");
        
        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setStyle("-fx-border-color: #E2E8F0; -fx-border-radius: 8px;");
        
        TextField timeField = new TextField();
        timeField.setPromptText("HH:MM (optionnel)");
        timeField.setStyle("-fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-padding: 8;");
        
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Travail", "Personnel", "Étude", "Santé", "Famille", "Loisirs", "Courses", "Autre");
        typeCombo.setValue("Personnel");
        typeCombo.setStyle("-fx-border-color: #E2E8F0; -fx-border-radius: 8px;");
        
        TextArea descArea = new TextArea();
        descArea.setPromptText("Description détaillée...");
        descArea.setPrefRowCount(2);
        descArea.setStyle("-fx-border-color: #E2E8F0; -fx-border-radius: 8px;");
        
        Slider importanceSlider = new Slider(1, 5, 3);
        importanceSlider.setShowTickLabels(true);
        importanceSlider.setShowTickMarks(true);
        importanceSlider.setMajorTickUnit(1);
        importanceSlider.setMinorTickCount(0);
        importanceSlider.setSnapToTicks(true);
        
        Label importanceLabel = new Label("⭐⭐⭐ (3/5)");
        importanceLabel.setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold;");
        importanceSlider.valueProperty().addListener((obs, old, val) -> {
            int v = val.intValue();
            importanceLabel.setText("⭐".repeat(v) + " (" + v + "/5)");
        });
        
        Button addBtn = new Button("➕ AJOUTER L'ÉVÉNEMENT");
        addBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10px; -fx-padding: 10;");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        
        addBtn.setOnAction(e -> {
            if (titleField.getText().trim().isEmpty()) {
                showToast("⚠️ Le titre est obligatoire", "#EF4444");
                return;
            }
            try {
                Event event = new Event();
                event.setTitle(titleField.getText().trim());
                event.setDate(datePicker.getValue());
                if (!timeField.getText().trim().isEmpty()) {
                    String timeStr = timeField.getText().trim();
                    if (timeStr.matches("\\d{1,2}:\\d{2}")) {
                        if (timeStr.length() == 4) timeStr = "0" + timeStr;
                        event.setTime(LocalTime.parse(timeStr));
                    } else {
                        showToast("⚠️ Format d'heure invalide", "#EF4444");
                        return;
                    }
                }
                event.setDescription(descArea.getText());
                event.setType(typeCombo.getValue());
                event.setImportanceLevel((int) importanceSlider.getValue());
                
                eventDAO.addEvent(event);
                showToast("✅ Événement ajouté : " + event.getTitle(), "#10B981");
                
                titleField.clear();
                timeField.clear();
                descArea.clear();
                importanceSlider.setValue(3);
                datePicker.setValue(LocalDate.now());
                typeCombo.setValue("Personnel");
                
                loadAllEvents();
                updateStatistics();
            } catch (SQLException ex) {
                showToast("❌ Erreur", "#EF4444");
            }
        });
        
        formPane.add(new Label("Titre *"), 0, 0);
        formPane.add(titleField, 1, 0);
        formPane.add(new Label("Date"), 0, 1);
        formPane.add(datePicker, 1, 1);
        formPane.add(new Label("Heure"), 0, 2);
        formPane.add(timeField, 1, 2);
        formPane.add(new Label("Catégorie"), 0, 3);
        formPane.add(typeCombo, 1, 3);
        formPane.add(new Label("Description"), 0, 4);
        formPane.add(descArea, 1, 4);
        formPane.add(new Label("Importance"), 0, 5);
        formPane.add(importanceSlider, 1, 5);
        formPane.add(importanceLabel, 1, 6);
        formPane.add(addBtn, 1, 7);
        
        TitledPane titledPane = new TitledPane("➕ AJOUT RAPIDE D'ÉVÉNEMENT", formPane);
        titledPane.setStyle("-fx-background-color: white; -fx-background-radius: 12px;");
        return titledPane;
    }
    
    private HBox createStatusBar() {
        HBox statusBar = new HBox();
        statusBar.setStyle("-fx-background-color: #1E293B; -fx-padding: 8 15;");
        statusBar.setAlignment(Pos.CENTER_LEFT);
        
        toastLabel = new Label();
        toastLabel.setStyle("-fx-background-color: #1E293B; -fx-text-fill: white; -fx-background-radius: 40px; -fx-padding: 8 20;");
        toastLabel.setVisible(false);
        
        statusLabel = new Label("✅ Prêt");
        statusLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 12px;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label versionLabel = new Label("v2.0 | © 2026");
        versionLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
        
        statusBar.getChildren().addAll(statusLabel, spacer, versionLabel);
        return statusBar;
    }
    
    private void openAddEventDialog() {
        Dialog<Event> dialog = new Dialog<>();
        dialog.setTitle("Nouvel événement");
        dialog.setHeaderText("Créer un nouvel événement");
        
        ButtonType addButtonType = new ButtonType("Ajouter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField titleField = new TextField();
        DatePicker datePicker = new DatePicker(LocalDate.now());
        TextField timeField = new TextField();
        timeField.setPromptText("HH:MM");
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Travail", "Personnel", "Étude", "Santé", "Famille", "Loisirs", "Courses", "Autre");
        typeCombo.setValue("Personnel");
        TextArea descArea = new TextArea();
        descArea.setPrefRowCount(3);
        Slider importanceSlider = new Slider(1, 5, 3);
        importanceSlider.setShowTickLabels(true);
        importanceSlider.setMajorTickUnit(1);
        importanceSlider.setSnapToTicks(true);
        
        grid.add(new Label("Titre:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Date:"), 0, 1);
        grid.add(datePicker, 1, 1);
        grid.add(new Label("Heure:"), 0, 2);
        grid.add(timeField, 1, 2);
        grid.add(new Label("Catégorie:"), 0, 3);
        grid.add(typeCombo, 1, 3);
        grid.add(new Label("Description:"), 0, 4);
        grid.add(descArea, 1, 4);
        grid.add(new Label("Importance:"), 0, 5);
        grid.add(importanceSlider, 1, 5);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                Event event = new Event();
                event.setTitle(titleField.getText());
                event.setDate(datePicker.getValue());
                if (!timeField.getText().isEmpty()) {
                    try { event.setTime(LocalTime.parse(timeField.getText())); } catch (Exception ex) {}
                }
                event.setDescription(descArea.getText());
                event.setType(typeCombo.getValue());
                event.setImportanceLevel((int) importanceSlider.getValue());
                return event;
            }
            return null;
        });
        
        Optional<Event> result = dialog.showAndWait();
        result.ifPresent(event -> {
            try {
                eventDAO.addEvent(event);
                showToast("✅ " + event.getTitle() + " ajouté", "#10B981");
                loadAllEvents();
                updateStatistics();
            } catch (SQLException e) {
                showToast("❌ Erreur", "#EF4444");
            }
        });
    }
    
    private void openEditEventDialog(Event event) {
        Dialog<Event> dialog = new Dialog<>();
        dialog.setTitle("Modifier l'événement");
        dialog.setHeaderText("Éditer : " + event.getTitle());
        
        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField titleField = new TextField(event.getTitle());
        DatePicker datePicker = new DatePicker(event.getDate());
        TextField timeField = new TextField(event.getTime() != null ? event.getTime().toString() : "");
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Travail", "Personnel", "Étude", "Santé", "Famille", "Loisirs", "Courses", "Autre");
        typeCombo.setValue(event.getType());
        TextArea descArea = new TextArea(event.getDescription());
        descArea.setPrefRowCount(3);
        Slider importanceSlider = new Slider(1, 5, event.getImportanceLevel());
        importanceSlider.setShowTickLabels(true);
        importanceSlider.setMajorTickUnit(1);
        importanceSlider.setSnapToTicks(true);
        
        grid.add(new Label("Titre:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Date:"), 0, 1);
        grid.add(datePicker, 1, 1);
        grid.add(new Label("Heure:"), 0, 2);
        grid.add(timeField, 1, 2);
        grid.add(new Label("Catégorie:"), 0, 3);
        grid.add(typeCombo, 1, 3);
        grid.add(new Label("Description:"), 0, 4);
        grid.add(descArea, 1, 4);
        grid.add(new Label("Importance:"), 0, 5);
        grid.add(importanceSlider, 1, 5);
        
        Button deleteBtn = new Button("🗑️ Supprimer");
        deleteBtn.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 8 16;");
        deleteBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText("Supprimer l'événement ?");
            confirm.setContentText("Voulez-vous vraiment supprimer : " + event.getTitle() + " ?");
            
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    eventDAO.deleteEvent(event.getId());
                    showToast("🗑️ " + event.getTitle() + " supprimé", "#EF4444");
                    loadAllEvents();
                    updateStatistics();
                    dialog.close();
                } catch (SQLException ex) {
                    showToast("❌ Erreur suppression", "#EF4444");
                }
            }
        });
        
        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #334155; -fx-background-radius: 8px; -fx-padding: 8 16;");
        cancelBtn.setOnAction(e -> dialog.close());
        
        Button saveBtn = new Button("💾 Enregistrer");
        saveBtn.setStyle("-fx-background-color: #6366F1; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 8 16;");
        saveBtn.setDefaultButton(true);
        
        HBox buttonBox = new HBox(10, deleteBtn, new Region(), cancelBtn, saveBtn);
        HBox.setHgrow(buttonBox.getChildren().get(1), Priority.ALWAYS);
        grid.add(buttonBox, 1, 6);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                event.setTitle(titleField.getText());
                event.setDate(datePicker.getValue());
                if (!timeField.getText().isEmpty()) {
                    try { event.setTime(LocalTime.parse(timeField.getText())); } catch (Exception ex) {}
                } else { event.setTime(null); }
                event.setDescription(descArea.getText());
                event.setType(typeCombo.getValue());
                event.setImportanceLevel((int) importanceSlider.getValue());
                return event;
            }
            return null;
        });
        
        Optional<Event> result = dialog.showAndWait();
        result.ifPresent(e -> {
            try {
                eventDAO.updateEvent(e);
                showToast("✏️ " + e.getTitle() + " modifié", "#6366F1");
                loadAllEvents();
                updateStatistics();
            } catch (SQLException ex) {
                showToast("❌ Erreur modification", "#EF4444");
            }
        });
    }
    
    private void loadAllEvents() {
        try {
            eventList.clear();
            eventList.addAll(eventDAO.getAllEvents());
            applyQuickFilter(currentFilter);
            statusLabel.setText("📋 " + eventList.size() + " événement(s)");
        } catch (SQLException e) {
            statusLabel.setText("❌ Erreur: " + e.getMessage());
        }
    }
    
    private void applyQuickFilter(String filter) {
        currentFilter = filter;
        filteredList.clear();
        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(7);
        for (Event event : eventList) {
            switch (filter) {
                case "Aujourd'hui":
                    if (event.getDate().equals(today)) filteredList.add(event);
                    break;
                case "Cette semaine":
                    if (!event.getDate().isBefore(today) && !event.getDate().isAfter(weekEnd)) filteredList.add(event);
                    break;
                case "Importants":
                    if (event.isImportant()) filteredList.add(event);
                    break;
                case "À venir":
                    if (!event.getDate().isBefore(today)) filteredList.add(event);
                    break;
                default: filteredList.add(event);
            }
        }
    }
    
    private void updateStatistics() {
        try {
            int total = eventDAO.getAllEvents().size();
            int today = eventDAO.searchByDate(LocalDate.now()).size();
            int important = eventDAO.getImportantEvents().size();
            int week = 0;
            LocalDate todayDate = LocalDate.now();
            for (Event e : eventDAO.getAllEvents()) {
                if (!e.getDate().isBefore(todayDate) && !e.getDate().isAfter(todayDate.plusDays(7))) week++;
            }
            statTotalLabel.setText(String.valueOf(total));
            statTodayLabel.setText(String.valueOf(today));
            statWeekLabel.setText(String.valueOf(week));
            statImportantLabel.setText(String.valueOf(important));
        } catch (SQLException e) {
            statTotalLabel.setText("?");
        }
    }
    
    private void searchEvents() {
        String searchType = searchTypeCombo.getValue();
        try {
            filteredList.clear();
            if (searchType.equals("Par Date")) {
                LocalDate date = searchDatePicker.getValue();
                if (date == null) { loadAllEvents(); return; }
                filteredList.addAll(eventDAO.searchByDate(date));
                statusLabel.setText("🔍 " + filteredList.size() + " résultat(s)");
            } else {
                String keyword = searchField.getText().trim();
                if (keyword.isEmpty()) { loadAllEvents(); return; }
                if (searchType.equals("Par Titre")) {
                    filteredList.addAll(eventDAO.searchByTitle(keyword));
                } else {
                    filteredList.addAll(eventDAO.searchByCategory(keyword));
                }
                statusLabel.setText("🔍 " + filteredList.size() + " résultat(s)");
            }
        } catch (SQLException e) {
            showToast("❌ Erreur recherche", "#EF4444");
        }
    }
    
    private void showToast(String message, String color) {
        toastLabel.setText(message);
        toastLabel.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 40px; -fx-padding: 10 24; -fx-font-size: 13px; -fx-font-weight: bold;");
        toastLabel.setVisible(true);
        if (toastTimeline != null) toastTimeline.stop();
        toastTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> toastLabel.setVisible(false)));
        toastTimeline.play();
    }
    
    private void confirmExit(Stage stage) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Quitter");
        confirm.setHeaderText("Voulez-vous vraiment quitter ?");
        confirm.setContentText("Toutes les données seront sauvegardées.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) stage.close();
    }
    
    private void showAboutDialog() {
        Alert about = new Alert(Alert.AlertType.INFORMATION);
        about.setTitle("À propos");
        about.setHeaderText("📅 Agenda Personnel v2.0");
        about.setContentText("Application de gestion d'agenda personnel.\nDéveloppée avec JavaFX et Oracle DB.\n\n© 2026");
        about.showAndWait();
    }
    
    private void showShortcutsDialog() {
        Alert shortcuts = new Alert(Alert.AlertType.INFORMATION);
        shortcuts.setTitle("Raccourcis clavier");
        shortcuts.setHeaderText("Raccourcis disponibles");
        shortcuts.setContentText("Ctrl + N : Nouvel événement\nCtrl + F : Rechercher\nF5 : Actualiser\nF11 : Plein écran\nCtrl + Q : Quitter");
        shortcuts.showAndWait();
    }
    
    private class EventCell extends ListCell<Event> {
        @Override
        protected void updateItem(Event event, boolean empty) {
            super.updateItem(event, empty);
            if (empty || event == null) {
                setText(null);
                setStyle("");
            } else {
                String star = event.isImportant() ? "⭐ " : "  ";
                String time = event.getTime() != null ? event.getTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "";
                setText(star + event.getTitle() + " - " + event.getDate() + " " + time + " [" + event.getType() + "]");
                String bgColor = event.getCategoryColor();
                String textColor = event.getTextColor();
                String style = "-fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor + "; -fx-padding: 12px 15px; -fx-background-radius: 12px;";
                if (event.isImportant()) style += "-fx-border-color: #F59E0B; -fx-border-width: 2px; -fx-border-radius: 12px;";
                setStyle(style);
            }
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}