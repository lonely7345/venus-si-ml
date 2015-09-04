package com.jd.si.ml.tree.split;

import java.io.Serializable;

/**
 * Discrete Test Statistics.
 */
public class DistTestStats extends TestStats implements Serializable {
    protected int correctCount;

    public DistTestStats() {
        super();
        correctCount = 0;
    }

    @Override
    public void reset() {
        totalCount = 0;
        correctCount = 0;
        isNew = false;
    }

    @Override
    public void collect(double actualValue, double predictValue) {
        totalCount++;

        if (actualValue == predictValue) {
            correctCount++;
        }
    }

    /**
     * Misclassification rate
     * @return
     */
    @Override
    public double getError() {
        if (totalCount == 0) {
            return 0d;
        }

        return 1.0 - (double) correctCount / (double) totalCount;
    }
}
