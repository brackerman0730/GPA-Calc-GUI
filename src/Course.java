import java.util.ArrayList;
import java.util.List;

public class Course {
    private String name;
    private final List<GradeComponent> components;
    private int credits;

    public Course(String name, List<GradeComponent> components, int credits) {
        this.name = name.trim();
        this.components = new ArrayList<>(components);
        this.credits = credits;
    }

    double calculateCurrentPercent() {
        double weightedSum = 0.0;
        double completedWeight = 0.0;

        for (GradeComponent c : components) {
            if (c.hasGrade()) {
                double w = c.getWeight();
                completedWeight += w;
                weightedSum += c.getGradeOrZero() * (w / 100.0);
            }
        }

        if (completedWeight == 0.0) return 0.0;
        return (weightedSum / (completedWeight / 100.0));
    }

    List<GradeComponent> internalComponents() {
        return components;
    }

    GradeComponent findComponentByName(String compName) {
        for (GradeComponent c : components) {
            if (c.nameForMatch().equalsIgnoreCase(compName.trim())) return c;
        }
        return null;
    }

    Double requiredForComponent(String componentName, double desiredPercent) {
        GradeComponent target = findComponentByName(componentName);
        if (target == null) return null;
        if (target.hasGrade()) return null;

        double completedWeightedSum = 0.0;
        double completedWeight = 0.0;
        double targetWeight = target.getWeight();

        for (GradeComponent c : components) {
            if (c == target) continue;
            if (c.hasGrade()) {
                completedWeight += c.getWeight();
                completedWeightedSum += c.getGradeOrZero() * (c.getWeight() / 100.0);
            }
        }

        double denom = (completedWeight + targetWeight);
        if (denom == 0.0) {
            return desiredPercent;
        }

        return (desiredPercent * (denom / 100.0) - completedWeightedSum) / (targetWeight / 100.0);
    }

    double credits() {
        return credits;
    }

    void setCredits(int credits) {
        this.credits = credits;
    }

    String courseName() {
        return name;
    }

    void setCourseName(String name) {
        this.name = name.trim();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" (").append(credits).append(" credits)\n");
        for (GradeComponent c : components) {
            sb.append("  - ").append(c.toString()).append("\n");
        }
        sb.append(String.format("  Current percent (ignoring nulls): %.2f%%\n", calculateCurrentPercent()));
        return sb.toString();
    }
}