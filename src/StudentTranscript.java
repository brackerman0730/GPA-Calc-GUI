import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentTranscript {

    private final Map<String, Course> courses = new HashMap<>();
    private final List<PriorRecord> priorRecords = new ArrayList<>();

    public void addCourse(Course course) {
        courses.put(course.courseName().toUpperCase(), course);
    }

    public void removeCourse(Course course) {
        if (course != null) {
            courses.remove(course.courseName().toUpperCase());
        }
    }

    Course findCourse(String name) {
        return courses.get(name.trim().toUpperCase());
    }

    public Iterable<Course> courses() {
        return this.courses.values();
    }

    public void clearCourses() {
        courses.clear();
    }

    public void addPriorRecord(PriorRecord record) {
        priorRecords.add(record);
    }

    public void removePriorRecord(PriorRecord record) {
        priorRecords.remove(record);
    }

    public List<PriorRecord> priorRecords() {
        return priorRecords;
    }

    public void clearPriorRecords() {
        priorRecords.clear();
    }

    double calculateCurrentSemesterGPA() {
        double totalPointsTimesCredits = 0.0;
        double totalCredits = 0.0;

        for (Course course : courses.values()) {
            double percent = course.calculateCurrentPercent();
            double points = percentToGradePoints(percent);
            double creds = course.credits();
            totalPointsTimesCredits += points * creds;
            totalCredits += creds;
        }

        if (totalCredits == 0.0) return 0.0;
        return totalPointsTimesCredits / totalCredits;
    }

    double calculateOverallGPA() {
        double totalQualityPoints = 0.0;
        double totalCredits = 0.0;

        for (Course course : courses.values()) {
            double percent = course.calculateCurrentPercent();
            double points = percentToGradePoints(percent);
            double creds = course.credits();
            totalQualityPoints += points * creds;
            totalCredits += creds;
        }

        for (PriorRecord record : priorRecords) {
            totalQualityPoints += record.getQualityPoints();
            totalCredits += record.getCredits();
        }

        if (totalCredits == 0.0) return 0.0;
        return totalQualityPoints / totalCredits;
    }

    double currentSemesterCredits() {
        double total = 0.0;
        for (Course c : courses.values()) total += c.credits();
        return total;
    }

    double priorCredits() {
        double total = 0.0;
        for (PriorRecord p : priorRecords) total += p.getCredits();
        return total;
    }

    private double percentToGradePoints(double pct) {
        if (pct >= 93.0) return 4.0;
        if (pct >= 90.0) return 3.7;
        if (pct >= 87.0) return 3.3;
        if (pct >= 83.0) return 3.0;
        if (pct >= 80.0) return 2.7;
        if (pct >= 77.0) return 2.3;
        if (pct >= 73.0) return 2.0;
        if (pct >= 70.0) return 1.7;
        if (pct >= 60.0) return 1.0;
        return 0.0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Transcript:\n");

        if (!priorRecords.isEmpty()) {
            sb.append("Prior GPA Records:\n");
            for (PriorRecord p : priorRecords) {
                sb.append("  - ").append(p).append("\n");
            }
            sb.append("\n");
        }

        for (Course c : courses.values()) {
            sb.append(c.toString()).append("\n");
        }

        sb.append(String.format("Current Semester GPA: %.2f\n", calculateCurrentSemesterGPA()));
        sb.append(String.format("Overall GPA: %.2f\n", calculateOverallGPA()));
        return sb.toString();
    }
}