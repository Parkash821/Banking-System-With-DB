// --- 8. util/LoanPriorityComparator.java ---
package util;

import model.LoanApplication;
import java.util.Comparator;

public class LoanPriorityComparator implements Comparator<LoanApplication> {
    @Override
    public int compare(LoanApplication a1, LoanApplication a2) {
        // Lower priorityScore means higher priority
        // If priorityScores are equal, prioritize older applications (smaller applicationDate)
        int scoreComparison = Integer.compare(a1.getPriorityScore(), a2.getPriorityScore());
        if (scoreComparison == 0) {
            return a1.getApplicationDate().compareTo(a2.getApplicationDate());
        }
        return scoreComparison;
    }
}
