import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class MainFX extends Application {

    private StudentTranscript transcript = new StudentTranscript();
    private final ObservableList<Course> courseObservable = FXCollections.observableArrayList();
    private final ObservableList<GradeComponent> componentObservable = FXCollections.observableArrayList();
    private final ObservableList<PriorRecord> priorObservable = FXCollections.observableArrayList();
    private final List<String> changeLog = new ArrayList<>();

    private ListView<Course> courseListView;
    private TableView<GradeComponent> componentTable;
    private TableView<PriorRecord> priorTable;
    private TextArea transcriptArea;

    private Label titleLabel;
    private Label creditsLabel;
    private Label currentPercentLabel;
    private Label semesterGpaLabel;
    private Label overallGpaLabel;
    private Label statusLabel;

    private File currentFile;

    private static final String[] TARGET_LABELS = { "B-", "B", "B+", "A-", "A" };
    private static final double[] TARGET_PERCENTS = { 80.0, 83.0, 87.0, 90.0, 93.0 };

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Transcript Helper");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        courseListView = new ListView<>(courseObservable);
        courseListView.setPrefWidth(260);
        courseListView.setCellFactory(lv -> new ListCell<Course>() {
            @Override
            protected void updateItem(Course item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label name = new Label(item.courseName());
                    name.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                    Label sub = new Label(String.format("Current: %.2f%%   Credits: %.0f",
                            item.calculateCurrentPercent(), item.credits()));
                    sub.setStyle("-fx-text-fill: #555555;");
                    VBox box = new VBox(name, sub);
                    box.setSpacing(2);
                    setGraphic(box);
                }
            }
        });

        courseListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) showCourseDetails(newV);
            else clearCourseDetails();
        });

        Label coursesHeader = new Label("Courses");
        coursesHeader.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button loadBtn = new Button("Load File...");
        Button loadDefaultBtn = new Button("Load src/Grades.txt");
        Button saveBtn = new Button("Save");
        Button saveAsBtn = new Button("Save As...");
        Button addCourseBtn = new Button("Add Course");
        Button editCourseBtn = new Button("Edit Course");
        Button deleteCourseBtn = new Button("Delete Course");

        for (Button b : Arrays.asList(loadBtn, loadDefaultBtn, saveBtn, saveAsBtn, addCourseBtn, editCourseBtn, deleteCourseBtn)) {
            b.setMaxWidth(Double.MAX_VALUE);
        }

        VBox leftPanel = new VBox(10,
                coursesHeader,
                courseListView,
                new Separator(),
                loadBtn,
                loadDefaultBtn,
                saveBtn,
                saveAsBtn,
                new Separator(),
                addCourseBtn,
                editCourseBtn,
                deleteCourseBtn
        );
        leftPanel.setPadding(new Insets(10));
        leftPanel.setPrefWidth(280);
        leftPanel.setStyle("-fx-background-color: #f7f7f7; -fx-border-color: #dddddd; -fx-border-radius: 6; -fx-background-radius: 6;");
        VBox.setVgrow(courseListView, Priority.ALWAYS);

        titleLabel = new Label("No course selected");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        creditsLabel = new Label("Credits: -");
        currentPercentLabel = new Label("Current Percent: -");
        semesterGpaLabel = new Label("Semester GPA: 0.00");
        overallGpaLabel = new Label("Overall GPA: 0.00");

        HBox summaryRow = new HBox(20, creditsLabel, currentPercentLabel, semesterGpaLabel, overallGpaLabel);
        summaryRow.setAlignment(Pos.CENTER_LEFT);

        VBox summaryPanel = new VBox(8, titleLabel, summaryRow);
        summaryPanel.setPadding(new Insets(12));
        summaryPanel.setStyle("-fx-background-color: #eef4ff; -fx-border-color: #cfd9ee; -fx-border-radius: 6; -fx-background-radius: 6;");

        componentTable = new TableView<>(componentObservable);
        componentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<GradeComponent, String> nameCol = new TableColumn<>("Component");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().nameForMatch()));

        TableColumn<GradeComponent, String> weightCol = new TableColumn<>("Weight");
        weightCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f%%", data.getValue().getWeight())));

        TableColumn<GradeComponent, String> gradeCol = new TableColumn<>("Grade");
        gradeCol.setCellValueFactory(data -> {
            GradeComponent gc = data.getValue();
            String text = gc.hasGrade() ? String.format("%.2f%%", gc.getGradeOrZero()) : "Missing";
            return new SimpleStringProperty(text);
        });

        componentTable.getColumns().addAll(nameCol, weightCol, gradeCol);

        Button setGradeBtn = new Button("Set Grade");
        Button clearGradeBtn = new Button("Clear Grade");
        Button fromAssignmentsBtn = new Button("From Assignments");
        Button editComponentBtn = new Button("Edit Component");
        Button addComponentBtn = new Button("Add Component");
        Button deleteComponentBtn = new Button("Delete Component");
        Button predictBtn = new Button("Predict Required");

        FlowPane componentButtons = new FlowPane();
        componentButtons.setHgap(8);
        componentButtons.setVgap(8);
        componentButtons.getChildren().addAll(
                setGradeBtn,
                clearGradeBtn,
                fromAssignmentsBtn,
                editComponentBtn,
                addComponentBtn,
                deleteComponentBtn,
                predictBtn
);

        Label componentHeader = new Label("Grade Components");
        componentHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        VBox componentPanel = new VBox(8, componentHeader, componentTable, componentButtons);
        VBox.setVgrow(componentTable, Priority.ALWAYS);

        priorTable = new TableView<>(priorObservable);
        priorTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<PriorRecord, String> priorCreditsCol = new TableColumn<>("Prior Credits");
        priorCreditsCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getCredits())));

        TableColumn<PriorRecord, String> priorGpaCol = new TableColumn<>("Prior GPA");
        priorGpaCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f", data.getValue().getGpa())));

        TableColumn<PriorRecord, String> priorQpCol = new TableColumn<>("Quality Points");
        priorQpCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f", data.getValue().getQualityPoints())));

        priorTable.getColumns().addAll(priorCreditsCol, priorGpaCol, priorQpCol);
        priorTable.setPrefHeight(160);

        Button addPriorBtn = new Button("Add Prior GPA");
        Button editPriorBtn = new Button("Edit Prior GPA");
        Button deletePriorBtn = new Button("Delete Prior GPA");

        HBox priorButtons = new HBox(8, addPriorBtn, editPriorBtn, deletePriorBtn);

        Label priorHeader = new Label("Old / Prior GPA Records");
        priorHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        VBox priorPanel = new VBox(8, priorHeader, priorTable, priorButtons);

        transcriptArea = new TextArea();
        transcriptArea.setEditable(false);
        transcriptArea.setWrapText(false);
        transcriptArea.setStyle("-fx-font-family: Consolas, monospace;");
        Label transcriptHeader = new Label("Transcript / Output");
        transcriptHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        VBox transcriptPanel = new VBox(8, transcriptHeader, transcriptArea);
        VBox.setVgrow(transcriptArea, Priority.ALWAYS);

        VBox centerPanel = new VBox(12, summaryPanel, componentPanel, priorPanel, transcriptPanel);
        centerPanel.setPadding(new Insets(0, 10, 0, 10));
        VBox.setVgrow(componentPanel, Priority.ALWAYS);
        VBox.setVgrow(transcriptPanel, Priority.ALWAYS);

        Label actionsHeader = new Label("Actions");
        actionsHeader.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button showTranscriptBtn = new Button("Show Transcript");
        Button showSemesterGpaBtn = new Button("Show Semester GPA");
        Button showOverallGpaBtn = new Button("Show Overall GPA");
        Button changeLogBtn = new Button("Show Change Log");
        Button clearOutputBtn = new Button("Clear Output");
        Button exitBtn = new Button("Exit");

        for (Button b : Arrays.asList(showTranscriptBtn, showSemesterGpaBtn, showOverallGpaBtn, changeLogBtn, clearOutputBtn, exitBtn)) {
            b.setMaxWidth(Double.MAX_VALUE);
        }

        VBox rightPanel = new VBox(
                10,
                actionsHeader,
                new Separator(),
                showTranscriptBtn,
                showSemesterGpaBtn,
                showOverallGpaBtn,
                changeLogBtn,
                clearOutputBtn,
                new Separator(),
                exitBtn
        );
        rightPanel.setPadding(new Insets(10));
        rightPanel.setPrefWidth(220);
        rightPanel.setStyle("-fx-background-color: #f7f7f7; -fx-border-color: #dddddd; -fx-border-radius: 6; -fx-background-radius: 6;");

        statusLabel = new Label("Ready.");
        HBox statusBar = new HBox(statusLabel);
        statusBar.setPadding(new Insets(8, 4, 4, 4));
        statusBar.setStyle("-fx-border-color: #dddddd transparent transparent transparent;");

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(leftPanel, centerPanel, rightPanel);
        splitPane.setDividerPositions(0.20, 0.84);

        root.setCenter(splitPane);
        root.setBottom(statusBar);

        loadBtn.setOnAction(e -> loadFileDialog(primaryStage));
        loadDefaultBtn.setOnAction(e -> loadDefaultFile());
        saveBtn.setOnAction(e -> saveCurrentFile(primaryStage));
        saveAsBtn.setOnAction(e -> saveAsFile(primaryStage));

        addCourseBtn.setOnAction(e -> handleAddCourse());
        editCourseBtn.setOnAction(e -> handleEditCourse());
        deleteCourseBtn.setOnAction(e -> handleDeleteCourse());

        setGradeBtn.setOnAction(e -> handleSetSelectedGrade());
        clearGradeBtn.setOnAction(e -> handleClearSelectedGrade());
        fromAssignmentsBtn.setOnAction(e -> handleSetAssignmentsForSelected());
        editComponentBtn.setOnAction(e -> handleEditSelectedComponent());
        addComponentBtn.setOnAction(e -> handleAddComponent());
        deleteComponentBtn.setOnAction(e -> handleDeleteComponent());
        predictBtn.setOnAction(e -> handlePredictForSelected());

        addPriorBtn.setOnAction(e -> handleAddPriorRecord());
        editPriorBtn.setOnAction(e -> handleEditPriorRecord());
        deletePriorBtn.setOnAction(e -> handleDeletePriorRecord());

        showTranscriptBtn.setOnAction(e -> showTranscript());
        showSemesterGpaBtn.setOnAction(e -> showSemesterGPA());
        showOverallGpaBtn.setOnAction(e -> showOverallGPA());
        changeLogBtn.setOnAction(e -> showChangeLog());
        clearOutputBtn.setOnAction(e -> transcriptArea.clear());
        exitBtn.setOnAction(e -> {
            onExit();
            Platform.exit();
        });

        Scene scene = new Scene(root, 1500, 850);
        scene.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.F5) showTranscript();
        });

        primaryStage.setScene(scene);
        primaryStage.show();

        loadDefaultFile();
    }

    private void loadDefaultFile() {
        File f = new File("src/Grades.txt");
        if (!f.exists()) {
            currentFile = null;
            statusLabel.setText("Default file not found: src/Grades.txt");
            refreshAllViews();
            return;
        }
        try {
            currentFile = f;
            transcript = new StudentTranscript();
            loadFromFile(f.getAbsolutePath(), transcript);
            refreshAllViews();
            statusLabel.setText("Loaded default file: " + f.getName());
            showTranscript();
        } catch (Exception ex) {
            showAlert(AlertType.ERROR, "Load Error", ex.getMessage());
        }
    }

    private void loadFileDialog(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open transcript file");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files", "*.txt", "*.csv", "*.*"));
        File f = chooser.showOpenDialog(stage);
        if (f == null) return;

        try {
            currentFile = f;
            transcript = new StudentTranscript();
            loadFromFile(f.getAbsolutePath(), transcript);
            refreshAllViews();
            statusLabel.setText("Loaded " + courseObservable.size() + " courses from " + f.getName());
            showTranscript();
        } catch (FileNotFoundException ex) {
            showAlert(AlertType.ERROR, "File not found", "Couldn't find file: " + f.getAbsolutePath());
        } catch (Exception ex) {
            showAlert(AlertType.ERROR, "Error parsing file", ex.getMessage());
        }
    }

    private void saveCurrentFile(Stage stage) {
        if (currentFile == null) {
            saveAsFile(stage);
            return;
        }
        try {
            saveToFile(currentFile, transcript);
            statusLabel.setText("Saved to " + currentFile.getName());
            showTranscript();
        } catch (Exception ex) {
            showAlert(AlertType.ERROR, "Save Error", ex.getMessage());
        }
    }

    private void saveAsFile(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save transcript file");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files", "*.txt", "*.csv", "*.*"));
        File f = chooser.showSaveDialog(stage);
        if (f == null) return;

        try {
            saveToFile(f, transcript);
            currentFile = f;
            statusLabel.setText("Saved to " + f.getName());
            showTranscript();
        } catch (Exception ex) {
            showAlert(AlertType.ERROR, "Save Error", ex.getMessage());
        }
    }

    private void refreshAllViews() {
        Course selected = getSelectedCourse();

        courseObservable.clear();
        for (Course c : transcript.courses()) {
            courseObservable.add(c);
        }

        priorObservable.clear();
        priorObservable.addAll(transcript.priorRecords());

        semesterGpaLabel.setText(String.format("Semester GPA: %.2f", transcript.calculateCurrentSemesterGPA()));
        overallGpaLabel.setText(String.format("Overall GPA: %.2f", transcript.calculateOverallGPA()));

        if (!courseObservable.isEmpty()) {
            if (selected != null) {
                boolean found = false;
                for (Course c : courseObservable) {
                    if (c.courseName().equalsIgnoreCase(selected.courseName())) {
                        courseListView.getSelectionModel().select(c);
                        showCourseDetails(c);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    courseListView.getSelectionModel().selectFirst();
                    showCourseDetails(courseListView.getSelectionModel().getSelectedItem());
                }
            } else {
                courseListView.getSelectionModel().selectFirst();
                showCourseDetails(courseListView.getSelectionModel().getSelectedItem());
            }
        } else {
            clearCourseDetails();
        }
    }

    private void clearCourseDetails() {
        titleLabel.setText("No course selected");
        creditsLabel.setText("Credits: -");
        currentPercentLabel.setText("Current Percent: -");
        componentObservable.clear();
        semesterGpaLabel.setText(String.format("Semester GPA: %.2f", transcript.calculateCurrentSemesterGPA()));
        overallGpaLabel.setText(String.format("Overall GPA: %.2f", transcript.calculateOverallGPA()));
    }

    private Course getSelectedCourse() {
        return courseListView.getSelectionModel().getSelectedItem();
    }

    private GradeComponent getSelectedComponent() {
        return componentTable.getSelectionModel().getSelectedItem();
    }

    private PriorRecord getSelectedPriorRecord() {
        return priorTable.getSelectionModel().getSelectedItem();
    }

    private void showTranscript() {
        transcriptArea.setText(transcript.toString());
        semesterGpaLabel.setText(String.format("Semester GPA: %.2f", transcript.calculateCurrentSemesterGPA()));
        overallGpaLabel.setText(String.format("Overall GPA: %.2f", transcript.calculateOverallGPA()));
    }

    private void showCourseDetails(Course c) {
        if (c == null) {
            clearCourseDetails();
            return;
        }
        titleLabel.setText(c.courseName());
        creditsLabel.setText(String.format("Credits: %.0f", c.credits()));
        currentPercentLabel.setText(String.format("Current Percent: %.2f%%", c.calculateCurrentPercent()));

        componentObservable.clear();
        componentObservable.addAll(c.internalComponents());

        semesterGpaLabel.setText(String.format("Semester GPA: %.2f", transcript.calculateCurrentSemesterGPA()));
        overallGpaLabel.setText(String.format("Overall GPA: %.2f", transcript.calculateOverallGPA()));
        transcriptArea.setText(c.toString());
    }

    private void handleAddCourse() {
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("Add Course");
        nameDialog.setHeaderText("Enter course name");
        nameDialog.setContentText("Course name:");
        Optional<String> nameRes = nameDialog.showAndWait();
        if (!nameRes.isPresent()) return;

        String name = nameRes.get().trim();
        if (name.isEmpty()) {
            showAlert(AlertType.ERROR, "Invalid Name", "Course name cannot be empty.");
            return;
        }

        TextInputDialog creditsDialog = new TextInputDialog("3");
        creditsDialog.setTitle("Add Course");
        creditsDialog.setHeaderText("Enter course credits");
        creditsDialog.setContentText("Credits:");
        Optional<String> creditsRes = creditsDialog.showAndWait();
        if (!creditsRes.isPresent()) return;

        try {
            int credits = Integer.parseInt(creditsRes.get().trim());
            if (credits < 0) throw new NumberFormatException();

            Course c = new Course(name, new ArrayList<>(), credits);
            transcript.addCourse(c);
            changeLog.add("Added course " + name + " (" + credits + " credits)");
            refreshAllViews();
            courseListView.getSelectionModel().select(c);
            statusLabel.setText("Added course " + name);
        } catch (NumberFormatException ex) {
            showAlert(AlertType.ERROR, "Invalid Credits", "Please enter a valid non-negative integer.");
        }
    }

    private void handleEditCourse() {
        Course c = getSelectedCourse();
        if (c == null) {
            showAlert(AlertType.INFORMATION, "Select Course", "Please select a course first.");
            return;
        }

        String oldName = c.courseName();
        int oldCredits = (int) c.credits();

        TextInputDialog nameDialog = new TextInputDialog(c.courseName());
        nameDialog.setTitle("Edit Course");
        nameDialog.setHeaderText("Edit course name");
        nameDialog.setContentText("Course name:");
        Optional<String> nameRes = nameDialog.showAndWait();
        if (!nameRes.isPresent()) return;

        String newName = nameRes.get().trim();
        if (newName.isEmpty()) {
            showAlert(AlertType.ERROR, "Invalid Name", "Course name cannot be empty.");
            return;
        }

        TextInputDialog creditsDialog = new TextInputDialog(String.valueOf((int) c.credits()));
        creditsDialog.setTitle("Edit Course");
        creditsDialog.setHeaderText("Edit course credits");
        creditsDialog.setContentText("Credits:");
        Optional<String> creditsRes = creditsDialog.showAndWait();
        if (!creditsRes.isPresent()) return;

        try {
            int credits = Integer.parseInt(creditsRes.get().trim());
            if (credits < 0) throw new NumberFormatException();

            transcript.removeCourse(c);
            c.setCourseName(newName);
            c.setCredits(credits);
            transcript.addCourse(c);

            changeLog.add(String.format("Edited course %s/%d -> %s/%d", oldName, oldCredits, newName, credits));
            refreshAllViews();
            statusLabel.setText("Edited course " + newName);
        } catch (NumberFormatException ex) {
            showAlert(AlertType.ERROR, "Invalid Credits", "Please enter a valid non-negative integer.");
        }
    }

    private void handleDeleteCourse() {
        Course c = getSelectedCourse();
        if (c == null) {
            showAlert(AlertType.INFORMATION, "Select Course", "Please select a course first.");
            return;
        }

        boolean ok = showConfirm("Delete Course", "Delete course '" + c.courseName() + "'?");
        if (!ok) return;

        transcript.removeCourse(c);
        changeLog.add("Deleted course " + c.courseName());
        refreshAllViews();
        statusLabel.setText("Deleted course.");
    }

    private void handleAddComponent() {
        Course c = getSelectedCourse();
        if (c == null) {
            showAlert(AlertType.INFORMATION, "Select Course", "Please select a course first.");
            return;
        }

        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("Add Component");
        nameDialog.setHeaderText("Enter component name");
        nameDialog.setContentText("Name:");
        Optional<String> nameRes = nameDialog.showAndWait();
        if (!nameRes.isPresent()) return;

        String name = nameRes.get().trim();
        if (name.isEmpty()) {
            showAlert(AlertType.ERROR, "Invalid Name", "Component name cannot be empty.");
            return;
        }

        TextInputDialog weightDialog = new TextInputDialog("0");
        weightDialog.setTitle("Add Component");
        weightDialog.setHeaderText("Enter component weight percent");
        weightDialog.setContentText("Weight:");
        Optional<String> weightRes = weightDialog.showAndWait();
        if (!weightRes.isPresent()) return;

        try {
            double weight = Double.parseDouble(weightRes.get().trim());
            if (weight < 0) throw new NumberFormatException();

            GradeComponent gc = new GradeComponent(name, weight, null);
            c.internalComponents().add(gc);
            changeLog.add(String.format("Added component %s to %s", name, c.courseName()));
            refreshAllViews();
            showCourseDetails(c);
            statusLabel.setText("Added component " + name);
        } catch (NumberFormatException ex) {
            showAlert(AlertType.ERROR, "Invalid Weight", "Please enter a valid non-negative number.");
        }
    }

    private void handleEditSelectedComponent() {
        Course c = getSelectedCourse();
        GradeComponent gc = getSelectedComponent();

        if (c == null) {
            showAlert(AlertType.INFORMATION, "Select Course", "Please select a course first.");
            return;
        }
        if (gc == null) {
            showAlert(AlertType.INFORMATION, "Select Component", "Please select a component first.");
            return;
        }

        String oldName = gc.nameForMatch();
        double oldWeight = gc.getWeight();
        Double oldGrade = gc.getRawGrade();

        TextInputDialog nameDialog = new TextInputDialog(gc.nameForMatch());
        nameDialog.setTitle("Edit Component");
        nameDialog.setHeaderText("Edit component name");
        nameDialog.setContentText("Name:");
        Optional<String> nameRes = nameDialog.showAndWait();
        if (!nameRes.isPresent()) return;

        String name = nameRes.get().trim();
        if (name.isEmpty()) {
            showAlert(AlertType.ERROR, "Invalid Name", "Component name cannot be empty.");
            return;
        }

        TextInputDialog weightDialog = new TextInputDialog(String.valueOf(gc.getWeight()));
        weightDialog.setTitle("Edit Component");
        weightDialog.setHeaderText("Edit component weight");
        weightDialog.setContentText("Weight:");
        Optional<String> weightRes = weightDialog.showAndWait();
        if (!weightRes.isPresent()) return;

        try {
            double weight = Double.parseDouble(weightRes.get().trim());
            if (weight < 0) throw new NumberFormatException();

            TextInputDialog gradeDialog = new TextInputDialog(gc.hasGrade() ? String.valueOf(gc.getGradeOrZero()) : "");
            gradeDialog.setTitle("Edit Component");
            gradeDialog.setHeaderText("Edit component grade (leave blank for missing)");
            gradeDialog.setContentText("Grade:");
            Optional<String> gradeRes = gradeDialog.showAndWait();
            if (!gradeRes.isPresent()) return;

            String gradeText = gradeRes.get().trim();
            Double grade = null;
            if (!gradeText.isEmpty()) {
                grade = Double.parseDouble(gradeText);
                if (grade < 0) throw new NumberFormatException();
            }

            gc.setName(name);
            gc.setWeight(weight);
            if (grade == null) gc.clearGrade();
            else gc.setGrade(grade);

            String oldGradeText = oldGrade == null ? "null" : String.format("%.2f", oldGrade);
            String newGradeText = grade == null ? "null" : String.format("%.2f", grade);
            changeLog.add(String.format("Edited component in %s: %s/%.2f/%s -> %s/%.2f/%s",
                    c.courseName(), oldName, oldWeight, oldGradeText, name, weight, newGradeText));

            refreshAllViews();
            showCourseDetails(c);
            statusLabel.setText("Edited component " + name);
        } catch (NumberFormatException ex) {
            showAlert(AlertType.ERROR, "Invalid Input", "Please enter valid non-negative numbers.");
        }
    }

    private void handleDeleteComponent() {
        Course c = getSelectedCourse();
        GradeComponent gc = getSelectedComponent();

        if (c == null) {
            showAlert(AlertType.INFORMATION, "Select Course", "Please select a course first.");
            return;
        }
        if (gc == null) {
            showAlert(AlertType.INFORMATION, "Select Component", "Please select a component first.");
            return;
        }

        boolean ok = showConfirm("Delete Component", "Delete component '" + gc.nameForMatch() + "'?");
        if (!ok) return;

        c.internalComponents().remove(gc);
        changeLog.add(String.format("Deleted component %s from %s", gc.nameForMatch(), c.courseName()));
        refreshAllViews();
        showCourseDetails(c);
        statusLabel.setText("Deleted component.");
    }

    private void handleSetSelectedGrade() {
        Course c = getSelectedCourse();
        GradeComponent comp = getSelectedComponent();

        if (c == null) {
            showAlert(AlertType.INFORMATION, "Select Course", "Please select a course first.");
            return;
        }
        if (comp == null) {
            showAlert(AlertType.INFORMATION, "Select Component", "Please select a component first.");
            return;
        }

        TextInputDialog input = new TextInputDialog(comp.hasGrade() ? String.format("%.2f", comp.getGradeOrZero()) : "");
        input.setTitle("Set Grade");
        input.setHeaderText("Enter numeric grade percent for '" + comp.nameForMatch() + "'");
        input.setContentText("Grade:");

        Optional<String> val = input.showAndWait();
        if (!val.isPresent()) return;

        try {
            double d = Double.parseDouble(val.get().trim());
            if (d < 0) throw new NumberFormatException();

            String old = comp.hasGrade() ? String.format("%.2f%%", comp.getGradeOrZero()) : "null";
            comp.setGrade(d);
            String now = String.format("%.2f%%", comp.getGradeOrZero());

            changeLog.add(String.format("%s;%s;%s->%s", c.courseName(), comp.nameForMatch(), old, now));
            statusLabel.setText("Updated " + comp.nameForMatch() + " to " + now + " in " + c.courseName());
            refreshAllViews();
            showCourseDetails(c);
        } catch (NumberFormatException ex) {
            showAlert(AlertType.ERROR, "Invalid Number", "Please enter a valid non-negative number.");
        }
    }

    private void handleClearSelectedGrade() {
        Course c = getSelectedCourse();
        GradeComponent comp = getSelectedComponent();

        if (c == null || comp == null) {
            showAlert(AlertType.INFORMATION, "Select Item", "Please select a course and component first.");
            return;
        }

        comp.clearGrade();
        changeLog.add(String.format("Cleared grade for %s in %s", comp.nameForMatch(), c.courseName()));
        refreshAllViews();
        showCourseDetails(c);
        statusLabel.setText("Cleared grade.");
    }

    private void handleSetAssignmentsForSelected() {
        Course c = getSelectedCourse();
        GradeComponent comp = getSelectedComponent();

        if (c == null) {
            showAlert(AlertType.INFORMATION, "Select Course", "Please select a course first.");
            return;
        }
        if (comp == null) {
            showAlert(AlertType.INFORMATION, "Select Component", "Please select a component first.");
            return;
        }

        Alert q = new Alert(AlertType.CONFIRMATION);
        q.setTitle("Assignment mode");
        q.setHeaderText("Do these assignments have different max points?");
        q.setContentText("Yes = enter fractions like 24/30; No = enter plain percents (100 90 85)");
        ButtonType yes = new ButtonType("Yes");
        ButtonType no = new ButtonType("No");
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        q.getButtonTypes().setAll(yes, no, cancel);

        Optional<ButtonType> mode = q.showAndWait();
        if (!mode.isPresent() || mode.get() == cancel) return;

        if (mode.get() == no) {
            TextInputDialog input = new TextInputDialog();
            input.setTitle("Enter Assignment Grades");
            input.setHeaderText("Enter grades separated by spaces");
            input.setContentText("Grades:");

            Optional<String> ans = input.showAndWait();
            if (!ans.isPresent()) return;

            String text = ans.get().trim();
            if (text.isEmpty()) {
                showAlert(AlertType.ERROR, "No Input", "No assignments entered.");
                return;
            }

            String[] parts = text.split("\\s+");
            double[] arr = new double[parts.length];

            try {
                for (int i = 0; i < parts.length; i++) {
                    double g = Double.parseDouble(parts[i]);
                    if (g < 0) throw new NumberFormatException();
                    arr[i] = g;
                }

                String old = comp.hasGrade() ? String.format("%.2f%%", comp.getGradeOrZero()) : "null";
                comp.setGradeFromAssignments(arr);
                String now = String.format("%.2f%%", comp.getGradeOrZero());

                changeLog.add(String.format("%s;%s;%s->%s", c.courseName(), comp.nameForMatch(), old, now));
                statusLabel.setText("Updated " + comp.nameForMatch() + " from assignment average -> " + now);
                refreshAllViews();
                showCourseDetails(c);
            } catch (NumberFormatException ex) {
                showAlert(AlertType.ERROR, "Invalid List", "Ensure all grades are non-negative numbers.");
            }
        } else {
            TextInputDialog input = new TextInputDialog();
            input.setTitle("Enter Assignment Fractions");
            input.setHeaderText("Enter assignments as fractions separated by spaces");
            input.setContentText("Fractions:");

            Optional<String> ans = input.showAndWait();
            if (!ans.isPresent()) return;

            String text = ans.get().trim();
            if (text.isEmpty()) {
                showAlert(AlertType.ERROR, "No Input", "No assignments entered.");
                return;
            }

            String[] parts = text.split("\\s+");
            double totalNum = 0.0;
            double totalDen = 0.0;

            try {
                for (String p : parts) {
                    String[] frac = p.trim().split("/");
                    if (frac.length != 2) throw new NumberFormatException();

                    double num = Double.parseDouble(frac[0]);
                    double den = Double.parseDouble(frac[1]);
                    if (den <= 0 || num < 0) throw new NumberFormatException();

                    totalNum += num;
                    totalDen += den;
                }

                if (totalDen <= 0) {
                    showAlert(AlertType.ERROR, "Bad Fractions", "Denominators must be positive.");
                    return;
                }

                double percent = (totalNum / totalDen) * 100.0;
                String old = comp.hasGrade() ? String.format("%.2f%%", comp.getGradeOrZero()) : "null";
                comp.setGrade(percent);
                String now = String.format("%.2f%%", comp.getGradeOrZero());

                changeLog.add(String.format("%s;%s;%s->%s", c.courseName(), comp.nameForMatch(), old, now));
                statusLabel.setText("Updated " + comp.nameForMatch() + " -> " + now);
                refreshAllViews();
                showCourseDetails(c);
            } catch (NumberFormatException ex) {
                showAlert(AlertType.ERROR, "Invalid Fractions", "Use format like 24/30 with positive denominators.");
            }
        }
    }

    private void handlePredictForSelected() {
        Course c = getSelectedCourse();
        GradeComponent target = getSelectedComponent();

        if (c == null) {
            showAlert(AlertType.INFORMATION, "Select Course", "Please select a course first.");
            return;
        }
        if (target == null) {
            showAlert(AlertType.INFORMATION, "Select Component", "Please select a component first.");
            return;
        }
        if (target.hasGrade()) {
            showAlert(AlertType.INFORMATION, "Already Filled", "Selected component already has a grade.");
            return;
        }

        ChoiceDialog<String> letterDlg = new ChoiceDialog<>(TARGET_LABELS[0], TARGET_LABELS);
        letterDlg.setTitle("Target Letter");
        letterDlg.setHeaderText("Choose a target letter grade");

        Optional<String> letterSel = letterDlg.showAndWait();
        if (!letterSel.isPresent()) return;

        String label = letterSel.get();
        int idx = Arrays.asList(TARGET_LABELS).indexOf(label);
        double desired = TARGET_PERCENTS[idx];

        Double needed = c.requiredForComponent(target.nameForMatch(), desired);
        if (needed == null) {
            showAlert(AlertType.ERROR, "Cannot Compute", "Could not compute requirement for that component.");
        } else {
            String msg = String.format("To reach %s (%.1f%%) in %s,\nyou need %.2f%% in '%s'.",
                    label, desired, c.courseName(), needed, target.nameForMatch());
            if (needed > 100) msg += "\n\nNote: needed > 100% (not achievable unless extra credit).";
            if (needed < 0) msg += "\n\nNote: needed < 0% (you already have enough).";
            showAlert(AlertType.INFORMATION, "Required Score", msg);
        }
    }

    private void handleAddPriorRecord() {
        TextInputDialog creditsDialog = new TextInputDialog();
        creditsDialog.setTitle("Add Prior GPA Record");
        creditsDialog.setHeaderText("Enter prior completed credits");
        creditsDialog.setContentText("Credits:");
        Optional<String> creditsRes = creditsDialog.showAndWait();
        if (!creditsRes.isPresent()) return;

        TextInputDialog gpaDialog = new TextInputDialog();
        gpaDialog.setTitle("Add Prior GPA Record");
        gpaDialog.setHeaderText("Enter GPA for those credits");
        gpaDialog.setContentText("GPA:");
        Optional<String> gpaRes = gpaDialog.showAndWait();
        if (!gpaRes.isPresent()) return;

        try {
            int credits = Integer.parseInt(creditsRes.get().trim());
            double gpa = Double.parseDouble(gpaRes.get().trim());
            if (credits < 0 || gpa < 0) throw new NumberFormatException();

            PriorRecord pr = new PriorRecord(credits, gpa);
            transcript.addPriorRecord(pr);
            changeLog.add(String.format("Added prior record: %d credits at %.2f GPA", credits, gpa));
            refreshAllViews();
            statusLabel.setText("Added prior GPA record.");
        } catch (NumberFormatException ex) {
            showAlert(AlertType.ERROR, "Invalid Input", "Please enter valid non-negative numbers.");
        }
    }

    private void handleEditPriorRecord() {
        PriorRecord pr = getSelectedPriorRecord();
        if (pr == null) {
            showAlert(AlertType.INFORMATION, "Select Prior Record", "Please select a prior GPA record first.");
            return;
        }

        int oldCredits = pr.getCredits();
        double oldGpa = pr.getGpa();

        TextInputDialog creditsDialog = new TextInputDialog(String.valueOf(pr.getCredits()));
        creditsDialog.setTitle("Edit Prior GPA Record");
        creditsDialog.setHeaderText("Edit prior credits");
        creditsDialog.setContentText("Credits:");
        Optional<String> creditsRes = creditsDialog.showAndWait();
        if (!creditsRes.isPresent()) return;

        TextInputDialog gpaDialog = new TextInputDialog(String.valueOf(pr.getGpa()));
        gpaDialog.setTitle("Edit Prior GPA Record");
        gpaDialog.setHeaderText("Edit prior GPA");
        gpaDialog.setContentText("GPA:");
        Optional<String> gpaRes = gpaDialog.showAndWait();
        if (!gpaRes.isPresent()) return;

        try {
            int credits = Integer.parseInt(creditsRes.get().trim());
            double gpa = Double.parseDouble(gpaRes.get().trim());
            if (credits < 0 || gpa < 0) throw new NumberFormatException();

            pr.setCredits(credits);
            pr.setGpa(gpa);
            changeLog.add(String.format("Edited prior record: %d/%.2f -> %d/%.2f", oldCredits, oldGpa, credits, gpa));
            refreshAllViews();
            statusLabel.setText("Edited prior GPA record.");
        } catch (NumberFormatException ex) {
            showAlert(AlertType.ERROR, "Invalid Input", "Please enter valid non-negative numbers.");
        }
    }

    private void handleDeletePriorRecord() {
        PriorRecord pr = getSelectedPriorRecord();
        if (pr == null) {
            showAlert(AlertType.INFORMATION, "Select Prior Record", "Please select a prior GPA record first.");
            return;
        }

        boolean ok = showConfirm("Delete Prior Record", "Delete selected prior GPA record?");
        if (!ok) return;

        transcript.removePriorRecord(pr);
        changeLog.add(String.format("Deleted prior record: %d credits at %.2f GPA", pr.getCredits(), pr.getGpa()));
        refreshAllViews();
        statusLabel.setText("Deleted prior GPA record.");
    }

    private void showSemesterGPA() {
        double gpa = transcript.calculateCurrentSemesterGPA();
        showAlert(AlertType.INFORMATION, "Semester GPA", String.format("Current Semester GPA: %.2f", gpa));
        semesterGpaLabel.setText(String.format("Semester GPA: %.2f", gpa));
    }

    private void showOverallGPA() {
        double gpa = transcript.calculateOverallGPA();
        showAlert(AlertType.INFORMATION, "Overall GPA", String.format("Overall GPA: %.2f", gpa));
        overallGpaLabel.setText(String.format("Overall GPA: %.2f", gpa));
    }

    private void showChangeLog() {
        if (changeLog.isEmpty()) {
            showAlert(AlertType.INFORMATION, "Change Log", "No changes made this session.");
            return;
        }

        String oneLine = String.join(" | ", changeLog);
        StringBuilder details = new StringBuilder();
        for (String s : changeLog) details.append("• ").append(s).append("\n");

        TextArea ta = new TextArea("One-line summary:\n" + oneLine + "\n\nDetailed log:\n" + details);
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefWidth(700);
        ta.setPrefHeight(450);

        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Change Log");
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.getDialogPane().setContent(ta);
        dlg.showAndWait();
    }

    private void onExit() {
        if (changeLog.isEmpty()) {
            System.out.println("No changes made during this session.");
        } else {
            String oneLine = String.join(" | ", changeLog);
            System.out.println("One-line summary of changes:");
            System.out.println(oneLine);
            System.out.println("\nDetailed change log:");
            for (String s : changeLog) System.out.println(" - " + s);
        }
    }

    private void showAlert(AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private boolean showConfirm(String title, String msg) {
        Alert a = new Alert(AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setTitle(title);
        Optional<ButtonType> res = a.showAndWait();
        return res.isPresent() && res.get() == ButtonType.YES;
    }

    private static void loadFromFile(String filename, StudentTranscript transcript) throws FileNotFoundException {
        Scanner fileScanner = new Scanner(new File(filename));
        int lineNo = 0;

        while (fileScanner.hasNextLine()) {
            String line = fileScanner.nextLine().trim();
            lineNo++;
            if (line.isEmpty()) continue;

            if (line.startsWith("PRIOR;")) {
                String[] p = line.split(";");
                if (p.length >= 3) {
                    try {
                        int credits = Integer.parseInt(p[1].trim());
                        double gpa = Double.parseDouble(p[2].trim());
                        transcript.addPriorRecord(new PriorRecord(credits, gpa));
                    } catch (Exception e) {
                        System.out.println("Skipping bad PRIOR line " + lineNo + ": " + line);
                    }
                }
                continue;
            }

            String[] parts = line.split(";");
            if (parts.length < 4) {
                System.out.println("Skipping malformed line " + lineNo + ": " + line);
                continue;
            }

            String courseName = parts[0].trim();
            String categoriesPart = parts[1].trim();
            String gradesPart = parts[2].trim();
            String creditsPart = parts[3].trim();

            String[] catItems = categoriesPart.split(",");
            List<String> catNames = new ArrayList<>();
            List<Double> catWeights = new ArrayList<>();

            for (String item : catItems) {
                String it = item.trim();
                if (it.isEmpty()) continue;

                int colon = it.lastIndexOf(':');
                if (colon < 0) {
                    System.out.println("Warning parsing category in line " + lineNo + ": " + it);
                    continue;
                }

                String name = it.substring(0, colon).trim();
                String wt = it.substring(colon + 1).replace("%", "").trim();

                try {
                    double w = Double.parseDouble(wt);
                    catNames.add(name);
                    catWeights.add(w);
                } catch (Exception e) {
                    System.out.println("Warning bad weight at line " + lineNo + ": " + wt);
                }
            }

            String[] gradeItems = gradesPart.split(",");
            List<Double> parsedGrades = new ArrayList<>();

            for (String gstr : gradeItems) {
                String gg = gstr.trim();
                if (gg.equalsIgnoreCase("null") || gg.isEmpty()) {
                    parsedGrades.add(null);
                } else {
                    try {
                        parsedGrades.add(Double.parseDouble(gg));
                    } catch (Exception e) {
                        parsedGrades.add(null);
                    }
                }
            }

            List<GradeComponent> components = new ArrayList<>();
            int n = Math.max(catNames.size(), parsedGrades.size());

            for (int i = 0; i < n; i++) {
                String cname = (i < catNames.size()) ? catNames.get(i) : ("Extra" + (i + 1));
                double wt = (i < catWeights.size()) ? catWeights.get(i) : 0.0;
                Double gr = (i < parsedGrades.size()) ? parsedGrades.get(i) : null;
                components.add(new GradeComponent(cname, wt, gr));
            }

            int credits = 0;
            try {
                credits = Integer.parseInt(creditsPart.trim());
            } catch (Exception e) {
                System.out.println("Warning: bad credits at line " + lineNo + ", defaulting to 0");
            }

            Course course = new Course(courseName, components, credits);
            transcript.addCourse(course);
        }

        fileScanner.close();
    }

    private static void saveToFile(File file, StudentTranscript transcript) throws IOException {
        PrintWriter out = new PrintWriter(new FileWriter(file));

        for (PriorRecord pr : transcript.priorRecords()) {
            out.println("PRIOR;" + pr.getCredits() + ";" + pr.getGpa());
        }

        for (Course course : transcript.courses()) {
            StringBuilder cats = new StringBuilder();
            StringBuilder grades = new StringBuilder();

            List<GradeComponent> comps = course.internalComponents();
            for (int i = 0; i < comps.size(); i++) {
                GradeComponent gc = comps.get(i);

                if (i > 0) {
                    cats.append(", ");
                    grades.append(", ");
                }

                cats.append(gc.nameForMatch()).append(": ").append(gc.getWeight()).append("%");
                Double raw = gc.getRawGrade();
                grades.append(raw == null ? "null" : raw);
            }

            out.println(course.courseName() + ";" + cats + ";" + grades + ";" + (int) course.credits());
        }

        out.close();
    }
}