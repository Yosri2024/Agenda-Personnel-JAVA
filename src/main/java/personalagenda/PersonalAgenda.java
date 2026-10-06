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
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class PersonalAgenda extends Application {

    private final EventDAO eventDAO = new EventDAO();
    private final ObservableList<Event> eventList = FXCollections.observableArrayList();
    private final ObservableList<Event> filteredList = FXCollections.observableArrayList();
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
    private Label listCountLabel;
    private final Map<String, Label> filterCountLabels = new HashMap<>();

    private Label toastLabel;
    private Timeline toastTimeline;

    private Scene scene;
    private CheckMenuItem themeMenuItem;
    private Button themeToggleBtn;
    private boolean darkMode = false;
    private String lightCss;
    private String darkCss;

    private String currentFilter = "Tous";

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("\uD83D\uDCC5 Agenda Personnel - Gestionnaire d'Événements");
        primaryStage.setMaximized(true);
        primaryStage.setMinWidth(1024);
        primaryStage.setMinHeight(768);

        lightCss = getClass().getResource("/styles/AgendaStyle.css").toExternalForm();
        darkCss = getClass().getResource("/styles/AgendaDark.css").toExternalForm();

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

        // Toast : créé ici (overlay), pas dans la barre de statut
        toastLabel = new Label();
        toastLabel.getStyleClass().addAll("toast", "toast-info");
        toastLabel.setVisible(false);
        toastLabel.setMouseTransparent(true);

        VBox root = new VBox();
        root.getChildren().addAll(menuBar, header, mainContent, statusBar);
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        StackPane overlayRoot = new StackPane();
        overlayRoot.getChildren().addAll(root, toastLabel);
        StackPane.setAlignment(toastLabel, Pos.BOTTOM_CENTER);
        StackPane.setMargin(toastLabel, new Insets(0, 0, 30, 0));

        scene = new Scene(overlayRoot);
        applyTheme(false);

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
            showToast("\uD83D\uDD04 Agenda actualisé", "toast-info");
        }));
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();
    }

    /** Applique le thème clair ou sombre. Le CSS sombre surcharge uniquement les tokens. */
    private void applyTheme(boolean dark) {
        this.darkMode = dark;
        scene.getStylesheets().clear();
        scene.getStylesheets().add(lightCss);
        if (dark) {
            scene.getStylesheets().add(darkCss);
        }
        if (themeMenuItem != null) {
            themeMenuItem.setSelected(dark);
        }
        if (themeToggleBtn != null) {
            themeToggleBtn.setText(dark ? "☀️ Mode clair" : "🌙 Mode nuit");
        }
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        Menu fileMenu = new Menu("\uD83D\uDCC1 Fichier");
        MenuItem newEventItem = new MenuItem("Nouvel événement");
        newEventItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        newEventItem.setOnAction(e -> openAddEventDialog());

        MenuItem importItem = new MenuItem("Importer (ICS)");
        importItem.setOnAction(e -> showToast("Import ICS non disponible dans cette version", "toast-info"));
        MenuItem exportItem = new MenuItem("Exporter (CSV)");
        exportItem.setOnAction(e -> showToast("Export CSV non disponible dans cette version", "toast-info"));
        SeparatorMenuItem sep1 = new SeparatorMenuItem();
        MenuItem exitItem = new MenuItem("Quitter");
        exitItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));
        exitItem.setOnAction(e -> confirmExit((Stage) menuBar.getScene().getWindow()));

        fileMenu.getItems().addAll(newEventItem, importItem, exportItem, sep1, exitItem);

        Menu viewMenu = new Menu("\uD83D\uDC41 Affichage");
        MenuItem refreshItem = new MenuItem("Actualiser");
        refreshItem.setAccelerator(new KeyCodeCombination(KeyCode.F5));
        refreshItem.setOnAction(e -> loadAllEvents());

        CheckMenuItem fullScreenItem = new CheckMenuItem("Plein écran");
        fullScreenItem.setAccelerator(new KeyCodeCombination(KeyCode.F11));
        fullScreenItem.setOnAction(e -> {
            Stage stage = (Stage) menuBar.getScene().getWindow();
            stage.setFullScreen(fullScreenItem.isSelected());
        });

        themeMenuItem = new CheckMenuItem("Thème sombre");
        themeMenuItem.setSelected(false);
        themeMenuItem.setOnAction(e -> applyTheme(themeMenuItem.isSelected()));

        viewMenu.getItems().addAll(refreshItem, new SeparatorMenuItem(), fullScreenItem, themeMenuItem);

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
        header.getStyleClass().add("app-header");

        Label titleLabel = new Label("\uD83D\uDCC5 Agenda Personnel");
        titleLabel.getStyleClass().add("header-title");

        Label subtitleLabel = new Label("Gérez vos événements efficacement");
        subtitleLabel.getStyleClass().add("header-subtitle");

        Label timeLabel = new Label();
        timeLabel.getStyleClass().add("header-time");
        Label dateLabel = new Label();
        dateLabel.getStyleClass().add("header-date");

        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            LocalDate now = LocalDate.now();
            dateLabel.setText(now.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)));
            timeLabel.setText(java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
        // Affichage immédiat sans attendre la 1re seconde
        dateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)));
        timeLabel.setText(java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        VBox leftBox = new VBox(2, titleLabel, subtitleLabel);
        leftBox.setAlignment(Pos.CENTER_LEFT);
        VBox dateTimeBox = new VBox(2, timeLabel, dateLabel);
        dateTimeBox.setAlignment(Pos.CENTER_RIGHT);

        // Bouton mode nuit bien visible dans l'en-tête (synchronisé avec le menu Affichage)
        themeToggleBtn = new Button("🌙 Mode nuit");
        themeToggleBtn.getStyleClass().add("theme-toggle");
        themeToggleBtn.setFocusTraversable(false);
        themeToggleBtn.setTooltip(new Tooltip("Basculer entre thème clair et sombre"));
        themeToggleBtn.setOnAction(e -> applyTheme(!darkMode));

        HBox rightBox = new HBox(14, themeToggleBtn, dateTimeBox);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        BorderPane headerPane = new BorderPane();
        headerPane.setLeft(leftBox);
        headerPane.setRight(rightBox);
        BorderPane.setAlignment(leftBox, Pos.CENTER_LEFT);
        BorderPane.setAlignment(rightBox, Pos.CENTER_RIGHT);

        header.getChildren().add(headerPane);
        return header;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("panel");
        sidebar.setPrefWidth(280);
        sidebar.setSpacing(10);

        Label sectionTitle = new Label("\uD83D\uDCCA STATISTIQUES");
        sectionTitle.getStyleClass().add("section-title");

        VBox totalCard = createStatCard("stat-total", "\uD83D\uDCCB", "Total");
        VBox todayCard = createStatCard("stat-today", "\uD83D\uDCC5", "Aujourd'hui");
        VBox weekCard = createStatCard("stat-week", "\uD83D\uDCC6", "Semaine");
        VBox importantCard = createStatCard("stat-important", "⭐", "Importants");

        statTotalLabel = (Label) totalCard.getChildren().get(1);
        statTodayLabel = (Label) todayCard.getChildren().get(1);
        statWeekLabel = (Label) weekCard.getChildren().get(1);
        statImportantLabel = (Label) importantCard.getChildren().get(1);

        HBox statsRow1 = new HBox(12, totalCard, todayCard);
        HBox statsRow2 = new HBox(12, weekCard, importantCard);
        statsRow1.setAlignment(Pos.CENTER);
        statsRow2.setAlignment(Pos.CENTER);
        HBox.setHgrow(totalCard, Priority.ALWAYS);
        HBox.setHgrow(todayCard, Priority.ALWAYS);
        HBox.setHgrow(weekCard, Priority.ALWAYS);
        HBox.setHgrow(importantCard, Priority.ALWAYS);

        VBox statsBox = new VBox(12, statsRow1, statsRow2);
        statsBox.setAlignment(Pos.CENTER);

        Label filtersTitle = new Label("\uD83D\uDD0D FILTRES RAPIDES");
        filtersTitle.getStyleClass().add("section-title");
        filtersTitle.setPadding(new Insets(10, 0, 5, 0));

        VBox filtersBox = new VBox(8);
        filterGroup = new ToggleGroup();

        String[] filters = {"Tous", "Aujourd'hui", "Cette semaine", "Importants", "À venir"};
        for (String filter : filters) {
            ToggleButton filterBtn = new ToggleButton();
            filterBtn.setToggleGroup(filterGroup);
            filterBtn.getStyleClass().add("side-btn");
            filterBtn.setMaxWidth(Double.MAX_VALUE);

            Label nameLabel = new Label(filter);
            nameLabel.getStyleClass().add("side-btn-text");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label countLabel = new Label("0");
            countLabel.getStyleClass().add("side-count");
            filterCountLabels.put(filter, countLabel);

            HBox graphic = new HBox(8, nameLabel, spacer, countLabel);
            graphic.setAlignment(Pos.CENTER_LEFT);
            filterBtn.setGraphic(graphic);

            filterBtn.setSelected(filter.equals("Tous"));
            final String f = filter;
            filterBtn.setOnAction(e -> applyQuickFilter(f));
            filtersBox.getChildren().add(filterBtn);
        }

        Label categoriesTitle = new Label("\uD83D\uDCF0  CATÉGORIES");
        categoriesTitle.getStyleClass().add("section-title");
        categoriesTitle.setPadding(new Insets(15, 0, 5, 0));

        VBox categoriesBox = new VBox(5);

        String[][] categories = {
            {"\uD83D\uDCBC Travail", "Travail", "hyperlink-category-work"},
            {"\uD83D\uDC64 Personnel", "Personnel", "hyperlink-category-personal"},
            {"\uD83D\uDCDA Étude", "Étude", "hyperlink-category-study"},
            {"\uD83C\uDFE5 Santé", "Santé", "hyperlink-category-health"},
            {"👪 Famille", "Famille", "hyperlink-category-family"},
            {"\uD83C\uDFAE Loisirs", "Loisirs", "hyperlink-category-leisure"},
            {"\uD83D\uDED2 Courses", "Courses", "hyperlink-category-courses"},
            {"⚙️ Autre", "Autre", "hyperlink-category-other"}
        };

        for (String[] cat : categories) {
            Hyperlink catLink = new Hyperlink(cat[0]);
            catLink.getStyleClass().add(cat[2]);
            catLink.setMaxWidth(Double.MAX_VALUE);
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

    private VBox createStatCard(String variant, String emoji, String name) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().addAll("stat-card", variant);
        card.setMinWidth(110);
        card.setPrefWidth(110);

        Label emojiLabel = new Label(emoji);
        emojiLabel.setStyle("-fx-font-size: 24px;");

        Label valueLabel = new Label("0");
        valueLabel.getStyleClass().add("stat-number");

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("stat-name");
        nameLabel.setWrapText(true);

        card.getChildren().addAll(emojiLabel, valueLabel, nameLabel);
        return card;
    }

    private VBox createCenterContent() {
        VBox center = new VBox(15);

        HBox searchBox = createSearchBox();

        HBox listHeader = new HBox(10);
        listHeader.setAlignment(Pos.CENTER_LEFT);
        Label listTitle = new Label("\uD83D\uDCCB LISTE DES ÉVÉNEMENTS");
        listTitle.getStyleClass().add("list-title");
        listCountLabel = new Label("0");
        listCountLabel.getStyleClass().add("list-count");
        listHeader.getChildren().addAll(listTitle, listCountLabel);

        listView = new ListView<>(filteredList);
        listView.getStyleClass().add("event-list");
        listView.setPrefHeight(400);
        listView.setCellFactory(lv -> new EventCell());
        Label emptyLabel = new Label("Aucun événement — ajoutez votre premier événement ci-dessous.");
        emptyLabel.getStyleClass().add("empty-label");
        listView.setPlaceholder(emptyLabel);

        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Event selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openEditEventDialog(selected);
                }
            }
        });

        TitledPane addPane = createAddEventPane();
        addPane.setAnimated(true);
        addPane.setExpanded(false);
        VBox.setVgrow(listView, Priority.ALWAYS);

        center.getChildren().addAll(searchBox, listHeader, listView, addPane);
        return center;
    }

    private HBox createSearchBox() {
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(0, 0, 10, 0));

        searchField = new TextField();
        searchField.setPromptText("Rechercher un événement...");
        searchField.setPrefWidth(300);

        searchField.textProperty().addListener((obs, old, val) -> {
            if (searchField.isVisible() && !"Par Date".equals(searchTypeCombo.getValue())) {
                searchEvents();
            }
        });

        searchTypeCombo = new ComboBox<>();
        searchTypeCombo.getItems().addAll("Par Titre", "Par Catégorie", "Par Date");
        searchTypeCombo.setValue("Par Titre");
        searchTypeCombo.setPrefWidth(130);

        searchDatePicker = new DatePicker();
        searchDatePicker.setPromptText("Choisir une date");
        searchDatePicker.setVisible(false);
        searchDatePicker.setManaged(false);
        searchDatePicker.setOnAction(e -> searchEvents());

        searchTypeCombo.setOnAction(e -> {
            boolean isDateSearch = "Par Date".equals(searchTypeCombo.getValue());
            searchDatePicker.setVisible(isDateSearch);
            searchDatePicker.setManaged(isDateSearch);
            searchField.setVisible(!isDateSearch);
            searchField.setManaged(!isDateSearch);
            if (isDateSearch && searchDatePicker.getValue() != null) {
                searchEvents();
            }
        });

        Button searchBtn = new Button("\uD83D\uDD0D Rechercher");
        searchBtn.getStyleClass().add("btn-primary");
        searchBtn.setOnAction(e -> searchEvents());

        Button resetBtn = new Button("Réinitialiser");
        resetBtn.getStyleClass().add("btn-secondary");
        resetBtn.setOnAction(e -> {
            searchField.clear();
            searchDatePicker.setValue(null);
            searchTypeCombo.setValue("Par Titre");
            loadAllEvents();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addQuickBtn = new Button("+ Nouvel événement");
        addQuickBtn.getStyleClass().add("btn-success");
        addQuickBtn.setOnAction(e -> openAddEventDialog());

        searchBox.getChildren().addAll(searchField, searchDatePicker, searchTypeCombo, searchBtn, resetBtn, spacer, addQuickBtn);
        return searchBox;
    }

    private TitledPane createAddEventPane() {
        GridPane formPane = new GridPane();
        formPane.setHgap(15);
        formPane.setVgap(12);
        formPane.setPadding(new Insets(15));
        formPane.getStyleClass().addAll("panel", "quick-add");

        TextField titleField = new TextField();
        titleField.setPromptText("Ex: Réunion importante");

        DatePicker datePicker = new DatePicker(LocalDate.now());

        TextField timeField = new TextField();
        timeField.setPromptText("HH:MM (optionnel)");

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Travail", "Personnel", "Étude", "Santé", "Famille", "Loisirs", "Courses", "Autre");
        typeCombo.setValue("Personnel");

        TextArea descArea = new TextArea();
        descArea.setPromptText("Description détaillée...");
        descArea.setPrefRowCount(2);

        Slider importanceSlider = new Slider(1, 5, 3);
        importanceSlider.setShowTickLabels(true);
        importanceSlider.setShowTickMarks(true);
        importanceSlider.setMajorTickUnit(1);
        importanceSlider.setMinorTickCount(0);
        importanceSlider.setSnapToTicks(true);

        Label importanceLabel = new Label("⭐⭐⭐ (3/5)");
        importanceLabel.getStyleClass().add("field-label");
        importanceSlider.valueProperty().addListener((obs, old, val) -> {
            int v = val.intValue();
            importanceLabel.setText("⭐".repeat(v) + " (" + v + "/5)");
        });

        Label formError = new Label();
        formError.getStyleClass().add("form-error");
        formError.setVisible(false);

        Button addBtn = new Button("➕ AJOUTER L'ÉVÉNEMENT");
        addBtn.getStyleClass().add("btn-success");
        addBtn.setMaxWidth(Double.MAX_VALUE);

        addBtn.setOnAction(e -> {
            if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
                formError.setText("Le titre est obligatoire.");
                formError.setVisible(true);
                showToast("⚠️ Le titre est obligatoire", "toast-error");
                return;
            }
            formError.setVisible(false);
            try {
                Event event = new Event();
                event.setTitle(titleField.getText().trim());
                event.setDate(datePicker.getValue());
                if (timeField.getText() != null && !timeField.getText().trim().isEmpty()) {
                    String timeStr = timeField.getText().trim();
                    if (timeStr.matches("\\d{1,2}:\\d{2}")) {
                        if (timeStr.length() == 4) timeStr = "0" + timeStr;
                        event.setTime(LocalTime.parse(timeStr));
                    } else {
                        formError.setText("Format d'heure invalide (attendu HH:MM).");
                        formError.setVisible(true);
                        showToast("⚠️ Format d'heure invalide", "toast-error");
                        return;
                    }
                }
                event.setDescription(descArea.getText());
                event.setType(typeCombo.getValue());
                event.setImportanceLevel((int) importanceSlider.getValue());

                eventDAO.addEvent(event);
                showToast("✅ Événement ajouté : " + event.getTitle(), "toast-success");

                titleField.clear();
                timeField.clear();
                descArea.clear();
                importanceSlider.setValue(3);
                datePicker.setValue(LocalDate.now());
                typeCombo.setValue("Personnel");

                loadAllEvents();
                updateStatistics();
            } catch (SQLException ex) {
                showToast("❌ Erreur d'ajout : " + ex.getMessage(), "toast-error");
            }
        });

        Label l1 = new Label("Titre *"); l1.getStyleClass().add("field-label");
        Label l2 = new Label("Date"); l2.getStyleClass().add("field-label");
        Label l3 = new Label("Heure"); l3.getStyleClass().add("field-label");
        Label l4 = new Label("Catégorie"); l4.getStyleClass().add("field-label");
        Label l5 = new Label("Description"); l5.getStyleClass().add("field-label");
        Label l6 = new Label("Importance"); l6.getStyleClass().add("field-label");
        formPane.add(l1, 0, 0);
        formPane.add(titleField, 1, 0);
        formPane.add(l2, 0, 1);
        formPane.add(datePicker, 1, 1);
        formPane.add(l3, 0, 2);
        formPane.add(timeField, 1, 2);
        formPane.add(l4, 0, 3);
        formPane.add(typeCombo, 1, 3);
        formPane.add(l5, 0, 4);
        formPane.add(descArea, 1, 4);
        formPane.add(l6, 0, 5);
        formPane.add(importanceSlider, 1, 5);
        formPane.add(importanceLabel, 1, 6);
        formPane.add(formError, 1, 7);
        formPane.add(addBtn, 1, 8);

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setMinWidth(110);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        formPane.getColumnConstraints().addAll(c0, c1);

        TitledPane titledPane = new TitledPane("➕ AJOUT RAPIDE D'ÉVÉNEMENT", formPane);
        return titledPane;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox();
        statusBar.getStyleClass().add("status-bar");
        statusBar.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label("✅ Prêt");
        statusLabel.getStyleClass().add("status-text");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label versionLabel = new Label("v2.0 | © 2026");
        versionLabel.getStyleClass().add("status-version");

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
                event.setTitle(titleField.getText() == null ? "" : titleField.getText().trim());
                event.setDate(datePicker.getValue());
                if (timeField.getText() != null && !timeField.getText().trim().isEmpty()) {
                    try { event.setTime(LocalTime.parse(timeField.getText().trim())); } catch (Exception ex) { event.setTime(null); }
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
            if (event.getTitle() == null || event.getTitle().isEmpty()) {
                showToast("⚠️ Le titre est obligatoire", "toast-error");
                return;
            }
            try {
                eventDAO.addEvent(event);
                showToast("✅ " + event.getTitle() + " ajouté", "toast-success");
                loadAllEvents();
                updateStatistics();
            } catch (SQLException e) {
                showToast("❌ Erreur d'ajout", "toast-error");
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
        int importance = event.getImportanceLevel() < 1 ? 1 : Math.min(event.getImportanceLevel(), 5);
        Slider importanceSlider = new Slider(1, 5, importance);
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
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setOnAction(e -> confirmDeleteEvent(event, dialog::close));

        dialog.getDialogPane().setContent(grid);
        // Ajoute la suppression comme zone d'actions personnalisée sous le formulaire
        VBox content = new VBox(10, grid, deleteBtn);
        content.setAlignment(Pos.CENTER_RIGHT);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                event.setTitle(titleField.getText());
                event.setDate(datePicker.getValue());
                if (timeField.getText() != null && !timeField.getText().isEmpty()) {
                    try { event.setTime(LocalTime.parse(timeField.getText().trim())); } catch (Exception ex) { event.setTime(null); }
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
                showToast("✏️ " + e.getTitle() + " modifié", "toast-success");
                loadAllEvents();
                updateStatistics();
            } catch (SQLException ex) {
                showToast("❌ Erreur modification", "toast-error");
            }
        });
    }

    /** Suppression avec confirmation, réutilisée par le dialogue et le bouton de chaque carte. */
    private void confirmDeleteEvent(Event event, Runnable onDeleted) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'événement ?");
        confirm.setContentText("Voulez-vous vraiment supprimer : " + event.getTitle() + " ?");

        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                eventDAO.deleteEvent(event.getId());
                showToast("🗑️ " + event.getTitle() + " supprimé", "toast-success");
                loadAllEvents();
                updateStatistics();
                if (onDeleted != null) onDeleted.run();
            } catch (SQLException ex) {
                showToast("❌ Erreur suppression", "toast-error");
            }
        }
    }

    private void loadAllEvents() {
        try {
            eventList.clear();
            eventList.addAll(eventDAO.getAllEvents());
            applyQuickFilter(currentFilter);
            statusLabel.setText("\uD83D\uDCCB " + eventList.size() + " événement(s)");
            statusLabel.getStyleClass().remove("status-error");
            if (!statusLabel.getStyleClass().contains("status-text")) {
                statusLabel.getStyleClass().add("status-text");
            }
        } catch (SQLException e) {
            statusLabel.setText("❌ Erreur: " + e.getMessage());
            if (!statusLabel.getStyleClass().contains("status-error")) {
                statusLabel.getStyleClass().add("status-error");
            }
        }
    }

    private void applyQuickFilter(String filter) {
        currentFilter = filter;
        // Synchronise le ToggleGroup si l'appel vient d'ailleurs que du bouton
        if (filterGroup != null) {
            for (Toggle t : filterGroup.getToggles()) {
                ToggleButton btn = (ToggleButton) t;
                HBox g = (HBox) btn.getGraphic();
                Label name = (Label) g.getChildren().get(0);
                if (filter.equals(name.getText())) {
                    filterGroup.selectToggle(btn);
                    break;
                }
            }
        }
        filteredList.clear();
        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(7);
        for (Event event : eventList) {
            if (event.getDate() == null) {
                if ("Tous".equals(filter)) filteredList.add(event);
                continue;
            }
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
        if (listCountLabel != null) {
            listCountLabel.setText(String.valueOf(filteredList.size()));
        }
        updateFilterCounts();
    }

    private void updateFilterCounts() {
        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(7);
        int cToday = 0, cWeek = 0, cImp = 0, cComing = 0;
        for (Event e : eventList) {
            if (e.getDate() == null) continue;
            if (e.getDate().equals(today)) cToday++;
            if (!e.getDate().isBefore(today) && !e.getDate().isAfter(weekEnd)) cWeek++;
            if (e.isImportant()) cImp++;
            if (!e.getDate().isBefore(today)) cComing++;
        }
        setFilterCount("Tous", eventList.size());
        setFilterCount("Aujourd'hui", cToday);
        setFilterCount("Cette semaine", cWeek);
        setFilterCount("Importants", cImp);
        setFilterCount("À venir", cComing);
    }

    private void setFilterCount(String filter, int value) {
        Label l = filterCountLabels.get(filter);
        if (l != null) l.setText(String.valueOf(value));
    }

    private void updateStatistics() {
        try {
            // Une seule lecture DB, calculs en mémoire
            java.util.List<Event> all = eventDAO.getAllEvents();
            LocalDate todayDate = LocalDate.now();
            int today = 0, important = 0, week = 0;
            for (Event e : all) {
                if (e.getDate() != null && e.getDate().equals(todayDate)) today++;
                if (e.isImportant()) important++;
                if (e.getDate() != null && !e.getDate().isBefore(todayDate) && !e.getDate().isAfter(todayDate.plusDays(7))) week++;
            }
            statTotalLabel.setText(String.valueOf(all.size()));
            statTodayLabel.setText(String.valueOf(today));
            statWeekLabel.setText(String.valueOf(week));
            statImportantLabel.setText(String.valueOf(important));
            updateFilterCounts();
        } catch (SQLException e) {
            statTotalLabel.setText("?");
            statTodayLabel.setText("?");
            statWeekLabel.setText("?");
            statImportantLabel.setText("?");
        }
    }

    private void searchEvents() {
        String searchType = searchTypeCombo.getValue();
        try {
            filteredList.clear();
            if ("Par Date".equals(searchType)) {
                LocalDate date = searchDatePicker.getValue();
                if (date == null) { loadAllEvents(); return; }
                filteredList.addAll(eventDAO.searchByDate(date));
                statusLabel.setText("\uD83D\uDD0D " + filteredList.size() + " résultat(s)");
            } else {
                String keyword = searchField.getText() == null ? "" : searchField.getText().trim();
                if (keyword.isEmpty()) { loadAllEvents(); return; }
                if ("Par Titre".equals(searchType)) {
                    filteredList.addAll(eventDAO.searchByTitle(keyword));
                } else {
                    filteredList.addAll(eventDAO.searchByCategory(keyword));
                }
                statusLabel.setText("\uD83D\uDD0D " + filteredList.size() + " résultat(s)");
            }
            if (listCountLabel != null) listCountLabel.setText(String.valueOf(filteredList.size()));
        } catch (SQLException e) {
            showToast("❌ Erreur recherche", "toast-error");
        }
    }

    private void showToast(String message, String toastStyle) {
        toastLabel.setText(message);
        toastLabel.getStyleClass().removeAll("toast-success", "toast-error", "toast-info");
        if (toastStyle == null || toastStyle.isEmpty()) toastStyle = "toast-info";
        // Compat : anciens appels avec couleur hex -> style correspondant
        if (toastStyle.startsWith("#")) {
            if (toastStyle.equalsIgnoreCase("#10B981") || toastStyle.equalsIgnoreCase("#059669")) toastStyle = "toast-success";
            else if (toastStyle.equalsIgnoreCase("#EF4444") || toastStyle.equalsIgnoreCase("#DC2626")) toastStyle = "toast-error";
            else toastStyle = "toast-info";
        }
        toastLabel.getStyleClass().add(toastStyle);
        toastLabel.setVisible(true);
        toastLabel.setOpacity(1.0);
        if (toastTimeline != null) toastTimeline.stop();
        toastTimeline = new Timeline(
            new KeyFrame(Duration.seconds(2.6), new KeyValue(toastLabel.opacityProperty(), 1.0)),
            new KeyFrame(Duration.seconds(3.0), new KeyValue(toastLabel.opacityProperty(), 0.0))
        );
        toastTimeline.setOnFinished(e -> toastLabel.setVisible(false));
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
        about.setHeaderText("\uD83D\uDCC5 Agenda Personnel v2.0");
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

    /**
     * Cellule riche pilotée par AgendaStyle.css (.event-card, .date-badge, .tag...).
     * Seules la pastille d'accent et le tag gardent un style inline dynamique
     * (couleur par catégorie), tout le reste vient du CSS.
     */
    private class EventCell extends ListCell<Event> {
        @Override
        protected void updateItem(Event event, boolean empty) {
            super.updateItem(event, empty);
            if (empty || event == null) {
                setText(null);
                setGraphic(null);
                getStyleClass().removeAll("event-card-today", "event-card-past");
            } else {
                LocalDate today = LocalDate.now();
                boolean isToday = today.equals(event.getDate());
                boolean isPast = event.getDate() != null && event.getDate().isBefore(today);

                HBox card = new HBox(12);
                card.getStyleClass().add("event-card");
                if (isToday) card.getStyleClass().add("event-card-today");
                if (isPast) card.getStyleClass().add("event-card-past");
                card.setAlignment(Pos.CENTER_LEFT);

                Region accent = new Region();
                accent.getStyleClass().add("event-accent");
                accent.setPrefWidth(5);
                accent.setMinWidth(5);
                accent.setMinHeight(56);
                accent.setStyle("-fx-background-color: " + event.getAccentColor() + ";");

                VBox dateBox = new VBox(0);
                dateBox.getStyleClass().add("date-badge");
                dateBox.setAlignment(Pos.CENTER);
                Label dayLabel = new Label(event.getDate() != null ? String.valueOf(event.getDate().getDayOfMonth()) : "—");
                dayLabel.getStyleClass().add("date-day");
                Label monthLabel = new Label(event.getDate() != null
                        ? event.getDate().getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH).toUpperCase(Locale.FRENCH)
                        : "");
                monthLabel.getStyleClass().add("date-month");
                dateBox.getChildren().addAll(dayLabel, monthLabel);

                VBox centerBox = new VBox(4);
                centerBox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(centerBox, Priority.ALWAYS);

                Label titleLabel = new Label((event.isImportant() ? "⭐ " : "") + (event.getTitle() == null ? "(sans titre)" : event.getTitle()));
                titleLabel.getStyleClass().add("event-title");
                titleLabel.setWrapText(true);

                String time = event.getTime() != null ? event.getTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "—";
                String dateStr = event.getDate() != null ? event.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "sans date";
                Label metaLabel = new Label(dateStr + " • " + time + " • " + (event.getType() == null ? "Autre" : event.getType()));
                metaLabel.getStyleClass().add("event-meta");

                centerBox.getChildren().addAll(titleLabel, metaLabel);
                if (event.getDescription() != null && !event.getDescription().trim().isEmpty()) {
                    Label descLabel = new Label(event.getDescription().trim().length() > 90
                            ? event.getDescription().trim().substring(0, 90) + "…" : event.getDescription().trim());
                    descLabel.getStyleClass().add("event-desc");
                    descLabel.setWrapText(true);
                    centerBox.getChildren().add(descLabel);
                }

                VBox rightBox = new VBox(6);
                rightBox.setAlignment(Pos.CENTER_RIGHT);
                Label tagLabel = new Label(event.getType() == null ? "Autre" : event.getType());
                tagLabel.getStyleClass().add("tag");
                // Fond du tag = couleur catégorie (pastel), texte = couleur contraste
                tagLabel.setStyle("-fx-background-color: " + event.getCategoryColor()
                        + "; -fx-text-fill: " + event.getTextColor() + ";");
                int level = Math.min(5, Math.max(1, event.getImportanceLevel()));
                HBox starsBox = new HBox(0);
                starsBox.setAlignment(Pos.CENTER_RIGHT);
                Label starsOn = new Label("★".repeat(level));
                starsOn.getStyleClass().add("stars-on");
                Label starsOff = new Label("☆".repeat(5 - level));
                starsOff.getStyleClass().add("stars-off");
                starsBox.getChildren().addAll(starsOn, starsOff);
                Tooltip.install(starsBox, new Tooltip("Importance : " + level + "/5"));
                Button delBtn = new Button("🗑️");
                delBtn.getStyleClass().add("event-delete");
                delBtn.setFocusTraversable(false);
                delBtn.setTooltip(new Tooltip("Supprimer cet événement"));
                delBtn.setOnAction(e -> confirmDeleteEvent(event, null));
                rightBox.getChildren().addAll(tagLabel, starsBox, delBtn);

                card.getChildren().addAll(accent, dateBox, centerBox, rightBox);

                setText(null);
                setGraphic(card);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
