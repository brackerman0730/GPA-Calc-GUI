import java.util.HashMap;
import java.util.Map;

public class StudentTranscript {

    private final Map<String, Course> courses = new HashMap<>();

    public void addCourse(Course course) {
        courses.put(course.courseName().toUpperCase(), course);
    }


    Course findCourse(String name) {
        return courses.get(name.trim().toUpperCase());
    }


    double calculateGPA() {
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

    public Iterable<Course> courses() {
        return this.courses.values();
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
        for (Course c : courses.values()) {
            sb.append(c.toString()).append("\n");
        }
        sb.append(String.format("GPA (current): %.2f\n", calculateGPA()));
        return sb.toString();
    }
}