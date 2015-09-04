package com.jd.si.ml.tree.split;

import java.io.Serializable;

/**
 * Numeric Test Statistics.
 */
public class NumTestStats extends TestStats implements Serializable {
    protected double squaredSum;

    public NumTestStats() {
        super();
        squaredSum = 0d;
    }

    @Override
    public void reset() {
        totalCount = 0;
        squaredSum = 0d;
        isNew = false;
    }

    @Override
    public void collect(double actualValue, double predictValue) {
        totalCount++;
        squaredSum += Math.pow(actualValue - predictValue, 2);
    }

    /**
     * Mean squared error
     * @return
     */
    @Override
    public double getError() {
        if (totalCount == 0) {
            return 0d;
        }

        return squaredSum / totalCount;
    }
}
