import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class MainFX extends Application {

    private StudentTranscript transcript = new StudentTranscript();
    private final ObservableList<Course> courseObservable = FXCollections.observableArrayList();
    private final ObservableList<GradeComponent> componentObservable = FXCollections.observableArrayList();
    private final List<String> changeLog = new ArrayList<>();

    private ListView<Course> courseListView;
    private TableView<GradeComponent> componentTable;
    private TextArea transcriptArea;

    private Label titleLabel;
    private Label creditsLabel;
    private Label currentPercentLabel;
    private Label gpaLabel;
    private Label statusLabel;

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

        // Left panel
        Label coursesHeader = new Label("Courses");
        coursesHeader.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

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
            if (newV != null) {
                showCourseDetails(newV);
            } else {
                clearCourseDetails();
            }
        });

        Button loadBtn = new Button("Load File...");
        Button loadDefaultBtn = new Button("Load src/Grades.txt");
        loadBtn.setMaxWidth(Double.MAX_VALUE);
        loadDefaultBtn.setMaxWidth(Double.MAX_VALUE);

        VBox leftPanel = new VBox(10, coursesHeader, courseListView, loadBtn, loadDefaultBtn);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setPrefWidth(280);
        leftPanel.setStyle("-fx-background-color: #f7f7f7; -fx-border-color: #dddddd; -fx-border-radius: 6; -fx-background-radius: 6;");

        // Center top summary
        titleLabel = new Label("No course selected");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        creditsLabel = new Label("Credits: -");
        currentPercentLabel = new Label("Current Percent: -");
        gpaLabel = new Label(String.format("Current GPA: %.2f", transcript.calculateGPA()));

        HBox summaryRow = new HBox(20, creditsLabel, currentPercentLabel, gpaLabel);
        summaryRow.setAlignment(Pos.CENTER_LEFT);

        VBox summaryPanel = new VBox(8, titleLabel, summaryRow);
        summaryPanel.setPadding(new Insets(12));
        summaryPanel.setStyle("-fx-background-color: #eef4ff; -fx-border-color: #cfd9ee; -fx-border-radius: 6; -fx-background-radius: 6;");

        // Center table
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

        Label tableHeader = new Label("Grade Components");
        tableHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        VBox tablePanel = new VBox(8, tableHeader, componentTable);
        VBox.setVgrow(componentTable, Priority.ALWAYS);

        // Transcript view
        transcriptArea = new TextArea();
        transcriptArea.setEditable(false);
        transcriptArea.setWrapText(false);
        transcriptArea.setPrefRowCount(10);
        transcriptArea.setStyle("-fx-font-family: Consolas, monospace;");

        Label transcriptHeader = new Label("Transcript / Output");
        transcriptHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        VBox transcriptPanel = new VBox(8, transcriptHeader, transcriptArea);
        VBox.setVgrow(transcriptArea, Priority.ALWAYS);

        VBox centerPanel = new VBox(12, summaryPanel, tablePanel, transcriptPanel);
        centerPanel.setPadding(new Insets(0, 10, 0, 10));
        VBox.setVgrow(tablePanel, Priority.ALWAYS);
        VBox.setVgrow(transcriptPanel, Priority.ALWAYS);

        // Right panel actions
        Label actionsHeader = new Label("Actions");
        actionsHeader.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label editHeader = new Label("Edit");
        editHeader.setStyle("-fx-font-weight: bold;");
        Button setGradeBtn = new Button("Set Selected Grade");
        Button setAssignmentsBtn = new Button("Set From Assignments");

        Label analysisHeader = new Label("Analysis");
        analysisHeader.setStyle("-fx-font-weight: bold;");
        Button predictBtn = new Button("Predict Required");
        Button showTranscriptBtn = new Button("Show Transcript");
        Button showGpaBtn = new Button("Show GPA");

        Label miscHeader = new Label("Other");
        miscHeader.setStyle("-fx-font-weight: bold;");
        Button changeLogBtn = new Button("Show Change Log");
        Button clearOutputBtn = new Button("Clear Output");
        Button exitBtn = new Button("Exit");

        for (Button b : Arrays.asList(
                setGradeBtn, setAssignmentsBtn, predictBtn, showTranscriptBtn,
                showGpaBtn, changeLogBtn, clearOutputBtn, exitBtn
        )) {
            b.setMaxWidth(Double.MAX_VALUE);
        }

        VBox rightPanel = new VBox(
                10,
                actionsHeader,
                new Separator(),
                editHeader, setGradeBtn, setAssignmentsBtn,
                new Separator(),
                analysisHeader, predictBtn, showTranscriptBtn, showGpaBtn,
                new Separator(),
                miscHeader, changeLogBtn, clearOutputBtn, exitBtn
        );
        rightPanel.setPadding(new Insets(10));
        rightPanel.setPrefWidth(220);
        rightPanel.setStyle("-fx-background-color: #f7f7f7; -fx-border-color: #dddddd; -fx-border-radius: 6; -fx-background-radius: 6;");

        // Bottom status bar
        statusLabel = new Label("Ready.");
        HBox statusBar = new HBox(statusLabel);
        statusBar.setPadding(new Insets(8, 4, 4, 4));
        statusBar.setStyle("-fx-border-color: #dddddd transparent transparent transparent;");

        // Layout with split pane
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(leftPanel, centerPanel, rightPanel);
        splitPane.setDividerPositions(0.22, 0.78);

        root.setCenter(splitPane);
        root.setBottom(statusBar);

        // Button actions
        loadBtn.setOnAction(e -> loadFileDialog(primaryStage));
        loadDefaultBtn.setOnAction(e -> loadDefaultFile());
        setGradeBtn.setOnAction(e -> handleSetSelectedGrade());
        setAssignmentsBtn.setOnAction(e -> handleSetAssignmentsForSelected());
        predictBtn.setOnAction(e -> handlePredictForSelected());
        showTranscriptBtn.setOnAction(e -> showTranscript());
        showGpaBtn.setOnAction(e -> showGPA());
        changeLogBtn.setOnAction(e -> showChangeLog());
        clearOutputBtn.setOnAction(e -> transcriptArea.clear());
        exitBtn.setOnAction(e -> {
            onExit();
            Platform.exit();
        });

        Scene scene = new Scene(root, 1350, 760);
        scene.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.F5) {
                showTranscript();
            }
        });

        primaryStage.setScene(scene);
        primaryStage.show();

        loadDefaultFile();
    }

    private void loadDefaultFile() {
        File f = new File("src/Grades.txt");
        if (!f.exists()) {
            statusLabel.setText("Default file not found: src/Grades.txt");
            return;
        }
        try {
            transcript = new StudentTranscript();
            loadFromFile(f.getAbsolutePath(), transcript);
            refreshCourseList();
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
            transcript = new StudentTranscript();
            loadFromFile(f.getAbsolutePath(), transcript);
            refreshCourseList();
            statusLabel.setText("Loaded " + courseObservable.size() + " courses from " + f.getName());
            showTranscript();
        } catch (FileNotFoundException ex) {
            showAlert(AlertType.ERROR, "File not found", "Couldn't find file: " + f.getAbsolutePath());
        } catch (Exception ex) {
            showAlert(AlertType.ERROR, "Error parsing file", ex.getMessage());
        }
    }

    private void refreshCourseList() {
        courseObservable.clear();
        for (Course c : transcript.courses()) {
            courseObservable.add(c);
        }
        gpaLabel.setText(String.format("Current GPA: %.2f", transcript.calculateGPA()));

        if (!courseObservable.isEmpty()) {
            courseListView.getSelectionModel().selectFirst();
        } else {
            clearCourseDetails();
        }
    }

    private void clearCourseDetails() {
        titleLabel.setText("No course selected");
        creditsLabel.setText("Credits: -");
        currentPercentLabel.setText("Current Percent: -");
        componentObservable.clear();
    }

    private Course getSelectedCourse() {
        return courseListView.getSelectionModel().getSelectedItem();
    }

    private GradeComponent getSelectedComponent() {
        return componentTable.getSelectionModel().getSelectedItem();
    }

    private void showTranscript() {
        transcriptArea.setText(transcript.toString());
        gpaLabel.setText(String.format("Current GPA: %.2f", transcript.calculateGPA()));
    }

    private void showCourseDetails(Course c) {
        titleLabel.setText(c.courseName());
        creditsLabel.setText(String.format("Credits: %.0f", c.credits()));
        currentPercentLabel.setText(String.format("Current Percent: %.2f%%", c.calculateCurrentPercent()));

        componentObservable.clear();
        componentObservable.addAll(c.internalComponents());

        gpaLabel.setText(String.format("Current GPA: %.2f", transcript.calculateGPA()));
        transcriptArea.setText(c.toString());
    }

    private void refreshSelectedCourseView() {
        Course c = getSelectedCourse();
        refreshCourseList();
        if (c != null) {
            for (Course course : courseObservable) {
                if (course.courseName().equalsIgnoreCase(c.courseName())) {
                    courseListView.getSelectionModel().select(course);
                    showCourseDetails(course);
                    break;
                }
            }
        }
    }

    private void handleSetSelectedGrade() {
        Course c = getSelectedCourse();
        if (c == null) {
            showAlert(AlertType.INFORMATION, "Select Course", "Please select a course first.");
            return;
        }

        GradeComponent comp = getSelectedComponent();
        if (comp == null) {
            showAlert(AlertType.INFORMATION, "Select Component", "Please select a component in the table.");
            return;
        }

        if (comp.hasGrade()) {
            boolean ok = showConfirm("Component already has a grade", "This component already has a grade. Overwrite?");
            if (!ok) return;
        }

        TextInputDialog input = new TextInputDialog(comp.hasGrade() ? String.format("%.2f", comp.getGradeOrZero()) : "");
        input.setTitle("Set Grade");
        input.setHeaderText("Enter numeric grade percent for '" + comp.nameForMatch() + "'");
        input.setContentText("Grade (0-100):");

        Optional<String> val = input.showAndWait();
        if (!val.isPresent()) return;

        try {
            double d = Double.parseDouble(val.get().trim());
            if (d < 0 || d > 100) throw new NumberFormatException();

            String old = comp.hasGrade() ? String.format("%.2f%%", comp.getGradeOrZero()) : "null";
            comp.setGrade(d);
            String now = String.format("%.2f%%", comp.getGradeOrZero());

            changeLog.add(String.format("%s;%s;%s->%s", c.courseName(), comp.nameForMatch(), old, now));
            statusLabel.setText("Updated " + comp.nameForMatch() + " to " + now + " in " + c.courseName());
            refreshSelectedCourseView();
        } catch (NumberFormatException ex) {
            showAlert(AlertType.ERROR, "Invalid number", "Please enter a valid number between 0 and 100.");
        }
    }

    private void handleSetAssignmentsForSelected() {
        Course c = getSelectedCourse();
        if (c == null) {
            showAlert(AlertType.INFORMATION, "Select Course", "Please select a course first.");
            return;
        }

        GradeComponent comp = getSelectedComponent();
        if (comp == null) {
            showAlert(AlertType.INFORMATION, "Select Component", "Please select a component in the table.");
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
                showAlert(AlertType.ERROR, "No input", "No assignments entered.");
                return;
            }

            String[] parts = text.split("\\s+");
            double[] arr = new double[parts.length];

            try {
                for (int i = 0; i < parts.length; i++) {
                    double g = Double.parseDouble(parts[i]);
                    if (g < 0 || g > 100) throw new NumberFormatException();
                    arr[i] = g;
                }

                String old = comp.hasGrade() ? String.format("%.2f%%", comp.getGradeOrZero()) : "null";
                comp.setGradeFromAssignments(arr);
                String now = String.format("%.2f%%", comp.getGradeOrZero());

                changeLog.add(String.format("%s;%s;%s->%s", c.courseName(), comp.nameForMatch(), old, now));
                statusLabel.setText("Updated " + comp.nameForMatch() + " from assignment average -> " + now);
                refreshSelectedCourseView();
            } catch (NumberFormatException ex) {
                showAlert(AlertType.ERROR, "Invalid list", "Ensure all grades are numbers between 0 and 100.");
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
                showAlert(AlertType.ERROR, "No input", "No assignments entered.");
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
                    if (den <= 0) throw new NumberFormatException();

                    totalNum += num;
                    totalDen += den;
                }

                if (totalDen <= 0) {
                    showAlert(AlertType.ERROR, "Bad fractions", "Denominators must be positive.");
                    return;
                }

                double percent = (totalNum / totalDen) * 100.0;
                String old = comp.hasGrade() ? String.format("%.2f%%", comp.getGradeOrZero()) : "null";
                comp.setGrade(percent);
                String now = String.format("%.2f%%", comp.getGradeOrZero());

                changeLog.add(String.format("%s;%s;%s->%s", c.courseName(), comp.nameForMatch(), old, now));
                statusLabel.setText("Updated " + comp.nameForMatch() + " -> " + now);
                refreshSelectedCourseView();
            } catch (NumberFormatException ex) {
                showAlert(AlertType.ERROR, "Invalid fractions", "Use format like 24/30 and positive denominators.");
            }
        }
    }

    private void handlePredictForSelected() {
        Course c = getSelectedCourse();
        if (c == null) {
            showAlert(AlertType.INFORMATION, "Select Course", "Please select a course first.");
            return;
        }

        GradeComponent target = getSelectedComponent();
        if (target == null) {
            showAlert(AlertType.INFORMATION, "Select Component", "Please select a missing component in the table.");
            return;
        }

        if (target.hasGrade()) {
            showAlert(AlertType.INFORMATION, "Already filled", "Selected component already has a grade.");
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
            showAlert(AlertType.ERROR, "Cannot compute", "Could not compute requirement for that component.");
        } else {
            String msg = String.format(
                    "To reach %s (%.1f%%) in %s,\nyou need %.2f%% in '%s'.",
                    label, desired, c.courseName(), needed, target.nameForMatch()
            );
            if (needed > 100) msg += "\n\nNote: needed > 100% (not achievable unless extra credit).";
            if (needed < 0) msg += "\n\nNote: needed < 0% (you already have enough).";

            showAlert(AlertType.INFORMATION, "Required Score", msg);
        }
    }

    private void showGPA() {
        double gpa = transcript.calculateGPA();
        showAlert(AlertType.INFORMATION, "GPA", String.format("Current GPA: %.2f", gpa));
        gpaLabel.setText(String.format("Current GPA: %.2f", gpa));
    }

    private void showChangeLog() {
        if (changeLog.isEmpty()) {
            showAlert(AlertType.INFORMATION, "Change Log", "No changes made this session.");
            return;
        }

        String oneLine = String.join(" | ", changeLog);
        StringBuilder details = new StringBuilder();
        for (String s : changeLog) {
            details.append("• ").append(s).append("\n");
        }

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
            for (String s : changeLog) {
                System.out.println(" - " + s);
            }
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
}