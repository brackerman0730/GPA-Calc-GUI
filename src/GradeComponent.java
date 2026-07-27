public class GradeComponent {
    private final String name;
    private final double weightPercent; // e.g., 25.0 for 25%
    private Double gradePercent; // null means missing

    public GradeComponent(String name, double weightPercent, Double gradePercent) {
        this.name = name.trim();
        this.weightPercent = weightPercent;
        this.gradePercent = gradePercent;
    }

    boolean hasGrade() {
        return gradePercent != null;
    }

    void setGrade(double grade) {
        this.gradePercent = grade;
    }

    void setGradeFromAssignments(double[] assignments) {
        if (assignments == null || assignments.length == 0) return;
        double sum = 0.0;
        for (double a : assignments) sum += a;
        this.gradePercent = sum / assignments.length;
    }

    double getWeight() {
        return weightPercent;
    }

    double getGradeOrZero() {
        return (gradePercent == null) ? 0.0 : gradePercent;
    }

    String nameForMatch() {
        return name;
    }

    @Override
    public String toString() {
        String g = hasGrade() ? String.format("%.2f%%", gradePercent) : "null";
        return name + " (" + weightPercent + "%): " + g;
    }
}
