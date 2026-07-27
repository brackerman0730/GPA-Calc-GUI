import java.util.ArrayList;
import java.util.List;

public class Course {
    private final String name;
    private final List<GradeComponent> components;
    private final int credits;

    public Course(String name, List<GradeComponent> components, int credits) {
        this.name = name.trim();
        this.components = new ArrayList<>(components);
        this.credits = credits;
    }

    // Calculate current course percentage by ignoring nulls and re-normalizing weights
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

        if (completedWeight == 0.0) return 0.0; // no data yet
        // Scale to 100: divide by completedWeight% and multiply by 100
        return (weightedSum / (completedWeight / 100.0));
    }

    // Return list of components (internal use)
    List<GradeComponent> internalComponents() {
        return components;
    }

    // Find a component by name (case-insensitive match)
    GradeComponent findComponentByName(String compName) {
        for (GradeComponent c : components) {
            if (c.nameForMatch().equalsIgnoreCase(compName.trim())) return c;
        }
        return null;
    }

    // For prediction: compute required value x for a chosen missing component to reach desiredPercent
    // We assume only the chosen component is filled (others remain null), and we re-normalize weights
    Double requiredForComponent(String componentName, double desiredPercent) {
        GradeComponent target = findComponentByName(componentName);
        if (target == null) return null;
        if (target.hasGrade()) return null; // already has a grade

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
            // no other data; needed grade is simply desiredPercent (no scaling)
            return desiredPercent;
        }

        // desiredPercent = (completedWeightedSum + x * (targetWeight/100)) / (denom/100)
        // solve for x:
        double needed = (desiredPercent * (denom / 100.0) - completedWeightedSum) / (targetWeight / 100.0);
        // needed may be below 0 or above 100, caller can interpret
        return needed;
    }

    double credits() {
        return credits;
    }

    String courseName() {
        return name;
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