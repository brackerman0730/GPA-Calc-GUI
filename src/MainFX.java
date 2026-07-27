// MainFX.java
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * JavaFX GUI front-end for the transcript/course utility.
 * Uses your model classes: Course, GradeComponent, StudentTranscript.
 */
public class MainFX extends Application {

    private StudentTranscript transcript = new StudentTranscript();
    private final ObservableList<Course> courseObservable = FXCollections.observableArrayList();
    private final List<String> changeLog = new ArrayList<>();

    private ListView<Course> courseListView;
    private TextArea rightText;
    private Label statusLabel;

    private static final String[] TARGET_LABELS = { "B-", "B", "B+", "A-", "A" };
    private static final double[] TARGET_PERCENTS = { 80.0, 83.0, 87.0, 90.0, 93.0 };

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Transcript Helper - JavaFX");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(8));

        // Left: Course list
        courseListView = new ListView<>(courseObservable);
        courseListView.setCellFactory(lv -> new ListCell<Course>() {
            @Override
            protected void updateItem(Course item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.courseName());
            }
        });
        courseListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) showCourseDetails(newV);
        });

        // Right: text area for transcript / details
        rightText = new TextArea();
        rightText.setEditable(false);
        rightText.setWrapText(false);

        // Top: toolbar (buttons)
        ToolBar toolbar = new ToolBar();
        Button loadBtn = new Button("Load File...");
        Button showTranscriptBtn = new Button("Show Transcript");
        Button showCourseBtn = new Button("Show Course Details");
        Button fillSingleBtn = new Button("Fill missing (single)");
        Button fillAssignBtn = new Button("Fill missing (assignments)");
        Button predictBtn = new Button("Predict required");
        Button gpaBtn = new Button("Show GPA");
        Button logBtn = new Button("Change Log");
        Button exitBtn = new Button("Exit");

        toolbar.getItems().addAll(loadBtn, new Separator(), showTranscriptBtn, showCourseBtn,
                new Separator(), fillSingleBtn, fillAssignBtn, predictBtn,
                new Separator(), gpaBtn, logBtn, new Separator(), exitBtn);

        // Bottom: status
        statusLabel = new Label("Ready.");

        // Layout
        VBox leftBox = new VBox(new Label("Courses:"), courseListView);
        leftBox.setSpacing(6);
        leftBox.setPrefWidth(260);

        HBox centerBox = new HBox();
        centerBox.setSpacing(6);
        centerBox.getChildren().addAll(rightText);
        HBox.setHgrow(rightText, Priority.ALWAYS);

        root.setTop(toolbar);
        root.setLeft(leftBox);
        root.setCenter(centerBox);
        root.setBottom(statusLabel);
        BorderPane.setMargin(leftBox, new Insets(6));
        BorderPane.setMargin(centerBox, new Insets(6));

        // Button actions
        loadBtn.setOnAction(e -> loadFileDialog(primaryStage));
        showTranscriptBtn.setOnAction(e -> showTranscript());
        showCourseBtn.setOnAction(e -> {
            Course c = courseListView.getSelectionModel().getSelectedItem();
            if (c == null) {
                showAlert(AlertType.INFORMATION, "Select Course", "Please select a course from the left panel.");
            } else {
                showCourseDetails(c);
            }
        });
        fillSingleBtn.setOnAction(e -> handleFillSingle());
        fillAssignBtn.setOnAction(e -> handleFillAssignments());
        predictBtn.setOnAction(e -> handlePredict());
        gpaBtn.setOnAction(e -> showGPA());
        logBtn.setOnAction(e -> showChangeLog());
        exitBtn.setOnAction(e -> {
            onExit();
            Platform.exit();
        });

        // Keybinding: Delete key to clear right text
        Scene scene = new Scene(root, 1000, 600);
        scene.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.F5) {
                showTranscript();
            }
        });

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadFileDialog(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open transcript file");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files", "*.txt", "*.csv", "*.*"));
        File f = chooser.showOpenDialog(stage);
        if (f == null) return;
        try {
            loadFromFile(f.getAbsolutePath(), transcript);
            // refresh list
            courseObservable.clear();
            for (Course c : transcript.courses()) courseObservable.add(c);
            statusLabel.setText("Loaded " + courseObservable.size() + " courses from " + f.getName());
            showTranscript();
        } catch (FileNotFoundException ex) {
            showAlert(AlertType.ERROR, "File not found", "Couldn't find file: " + f.getAbsolutePath());
        } catch (Exception ex) {
            showAlert(AlertType.ERROR, "Error parsing file", ex.getMessage());
        }
    }

    private void showTranscript() {
        StringBuilder sb = new StringBuilder();
        sb.append(transcript.toString());
        rightText.setText(sb.toString());
    }

    private void showCourseDetails(Course c) {
        rightText.setText(c.toString());
    }

    private void handleFillSingle() {
        Course c = courseListView.getSelectionModel().getSelectedItem();
        if (c == null) {
            showAlert(AlertType.INFORMATION, "Select Course", "Please select a course to edit.");
            return;
        }
        // list components and pick one
        List<GradeComponent> comps = c.internalComponents();
        ChoiceDialog<GradeComponent> dlg = new ChoiceDialog<>(comps.get(0), comps);
        dlg.setTitle("Select Component");
        dlg.setHeaderText("Choose component to fill (single numeric grade)");
        dlg.setContentText("Component:");
        // show names in dialog by toString -> override converter

        Optional<GradeComponent> res = dlg.showAndWait();
        if (!res.isPresent()) return;
        GradeComponent comp = res.get();
        if (comp.hasGrade()) {
            boolean ok = showConfirm("Component already has a grade", "This component already has a grade. Overwrite?");
            if (!ok) return;
        }
        TextInputDialog input = new TextInputDialog();
        input.setTitle("Enter grade");
        input.setHeaderText("Enter numeric grade percent for '" + comp.nameForMatch() + "' (0-100)");
        input.setContentText("Grade:");
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
            showCourseDetails(c);
        } catch (NumberFormatException ex) {
            showAlert(AlertType.ERROR, "Invalid number", "Please enter a valid number between 0 and 100.");
        }
    }

    private void handleFillAssignments() {
        Course c = courseListView.getSelectionModel().getSelectedItem();
        if (c == null) {
            showAlert(AlertType.INFORMATION, "Select Course", "Please select a course to edit.");
            return;
        }
        List<GradeComponent> comps = c.internalComponents();
        ChoiceDialog<GradeComponent> dlg = new ChoiceDialog<>(comps.get(0), comps);
        dlg.setTitle("Select Component");
        dlg.setHeaderText("Choose component to fill from assignments");
        dlg.setContentText("Component:");

        Optional<GradeComponent> chosen = dlg.showAndWait();
        if (!chosen.isPresent()) return;
        GradeComponent comp = chosen.get();

        // Ask whether assignments have different max points
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
            input.setTitle("Enter assignment grades");
            input.setHeaderText("Enter assignment grades separated by spaces (e.g. 100 90 85)");
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
                statusLabel.setText("Updated assignments average for " + comp.nameForMatch() + " -> " + now);
                showCourseDetails(c);
            } catch (NumberFormatException ex) {
                showAlert(AlertType.ERROR, "Invalid list", "Ensure all are numbers between 0 and 100.");
                return;
            }

        } else {
            // fraction mode
            TextInputDialog input = new TextInputDialog();
            input.setTitle("Enter assignment fractions");
            input.setHeaderText("Enter assignments as fractions separated by spaces (e.g. 24/30 61/70)");
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
                    String token = p.trim();
                    if (token.isEmpty()) continue;
                    String[] frac = token.split("/");
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
                statusLabel.setText("Updated component " + comp.nameForMatch() + " -> " + now);
                showCourseDetails(c);
            } catch (NumberFormatException ex) {
                showAlert(AlertType.ERROR, "Invalid fractions", "Use format like 24/30, denominators must be positive.");
                return;
            }
        }
    }

    private void handlePredict() {
        Course c = courseListView.getSelectionModel().getSelectedItem();
        if (c == null) {
            showAlert(AlertType.INFORMATION, "Select Course", "Please select a course to predict for.");
            return;
        }
        List<GradeComponent> missing = new ArrayList<>();
        for (GradeComponent gc : c.internalComponents()) {
            if (!gc.hasGrade()) missing.add(gc);
        }
        if (missing.isEmpty()) {
            showAlert(AlertType.INFORMATION, "Nothing to predict", "No missing components to predict for (all filled).");
            return;
        }
        ChoiceDialog<GradeComponent> chooseComp = new ChoiceDialog<>(missing.get(0), missing);
        chooseComp.setTitle("Choose missing component");
        chooseComp.setHeaderText("Select which missing component to compute required score for");

        Optional<GradeComponent> sel = chooseComp.showAndWait();
        if (!sel.isPresent()) return;
        GradeComponent target = sel.get();

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
            String msg = String.format("To reach %s (%.1f%%) in %s, you need %.2f%% in '%s'.",
                    label, desired, c.courseName(), needed, target.nameForMatch());
            if (needed > 100) msg += "\nNote: needed > 100% (not achievable unless extra credit).";
            if (needed < 0) msg += "\nNote: needed < 0% (you already have enough).";
            showAlert(AlertType.INFORMATION, "Required Score", msg);
        }
    }

    private void showGPA() {
        double gpa = transcript.calculateGPA();
        showAlert(AlertType.INFORMATION, "GPA", String.format("GPA (current): %.2f", gpa));
    }

    private void showChangeLog() {
        if (changeLog.isEmpty()) {
            showAlert(AlertType.INFORMATION, "Change log", "No changes made this session.");
            return;
        }
        // one-line and detailed
        String oneLine = String.join(" | ", changeLog);
        StringBuilder details = new StringBuilder();
        for (String s : changeLog) details.append(" - ").append(s).append("\n");
        TextArea ta = new TextArea("One-line:\n" + oneLine + "\n\nDetailed:\n" + details.toString());
        ta.setEditable(false);
        ta.setWrapText(false);
        ta.setPrefWidth(600);
        ta.setPrefHeight(400);
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
            System.out.println("One-line summary of changes (copy/paste to save):");
            System.out.println(oneLine);
            System.out.println("\nDetailed change log:");
            for (String s : changeLog) System.out.println(" - " + s);
        }
    }

    // Utility dialogs
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

    /**
     * Load from file using same parsing logic as the console version.
     */
    private static void loadFromFile(String filename, StudentTranscript transcript) throws FileNotFoundException {
        Scanner fileScanner = new Scanner(new File(filename));
        int lineNo = 0;
        while (fileScanner.hasNextLine()) {
            String line = fileScanner.nextLine().trim();
            lineNo++;
            if (line.isEmpty()) continue;
            // Expected: name;categories;grades;credits
            String[] parts = line.split(";");
            if (parts.length < 4) {
                System.out.println("Skipping malformed line " + lineNo + ": " + line);
                continue;
            }
            String courseName = parts[0].trim();
            String categoriesPart = parts[1].trim();
            String gradesPart = parts[2].trim();
            String creditsPart = parts[3].trim();

            // Parse categories: items separated by ','; each item "Name: XX%"
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

            // Parse grades: comma-separated; use 'null' for missing
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

            // Make components: align by position; if mismatch, fill with nulls or extra categories ignored
            List<GradeComponent> components = new ArrayList<>();
            int n = Math.max(catNames.size(), parsedGrades.size());
            for (int i = 0; i < n; i++) {
                String cname = (i < catNames.size()) ? catNames.get(i) : ("Extra" + (i+1));
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
