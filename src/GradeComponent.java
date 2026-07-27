public class GradeComponent {
    private String name;
    private double weightPercent;
    private Double gradePercent;

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

    void clearGrade() {
        this.gradePercent = null;
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

    void setWeight(double weightPercent) {
        this.weightPercent = weightPercent;
    }

    double getGradeOrZero() {
        return (gradePercent == null) ? 0.0 : gradePercent;
    }

    Double getRawGrade() {
        return gradePercent;
    }

    String nameForMatch() {
        return name;
    }

    void setName(String name) {
        this.name = name.trim();
    }

    @Override
    public String toString() {
        String g = hasGrade() ? String.format("%.2f%%", gradePercent) : "null";
        return name + " (" + weightPercent + "%): " + g;
    }
}