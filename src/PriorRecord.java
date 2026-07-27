public class PriorRecord {
    private int credits;
    private double gpa;

    public PriorRecord(int credits, double gpa) {
        this.credits = credits;
        this.gpa = gpa;
    }

    int getCredits() {
        return credits;
    }

    void setCredits(int credits) {
        this.credits = credits;
    }

    double getGpa() {
        return gpa;
    }

    void setGpa(double gpa) {
        this.gpa = gpa;
    }

    double getQualityPoints() {
        return credits * gpa;
    }

    @Override
    public String toString() {
        return String.format("%d credits at %.2f GPA", credits, gpa);
    }
}