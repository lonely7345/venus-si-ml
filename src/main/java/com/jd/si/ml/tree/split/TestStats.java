package com.jd.si.ml.tree.split;


import com.jd.si.ml.core.Attribute;

import java.io.Serializable;

/**
 * Abstract Statistics information for Test mode
 */
public abstract class TestStats implements Serializable {
    protected int totalCount = 0;

    /**
     * Only valid for alternative nodes. Stores the previous best error difference
     * between the main tree and the alternative node. Used for pruning unpromising
     * alternative trees.
     */
    // start with infinitely bad error
    protected double bestErrorDiff = -Double.MAX_VALUE;

    /**
     * A flag set when a new alternative node is first created.
     * Necessary because the test interval is based on per-node counts and the
     * alternative node creation interval is based on the overall instance count.
     * Therefore, a new alternative node may get created in the middle of the test
     * phase for a node. This node shouldn't be included in the test (since it
     * doesn't have a full set of instances).
     */
    protected boolean isNew = false;

    /**
     * Reset
     */
    public abstract void reset();

    public abstract void collect(double actualValue, double predictValue);

    public abstract double getError();

    public static TestStats getTestStats(Attribute classAttr) {
        if (classAttr.isNumeric()) {
            return new NumTestStats();
        } else {
            return new DistTestStats();
        }
    }

    public TestStats() {

    }

    public double getBestErrorDiff()
    {
        return bestErrorDiff;
    }

    public void setBestErrorDiff(double bestError)
    {
        this.bestErrorDiff = bestError;
    }

    public boolean isNew()
    {
        return this.isNew;
    }

    public void setNew(boolean isNew)
    {
        this.isNew = isNew;
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder();

        text.append("testStats: ").append(getError()).append(", ");
        text.append("bestErrorDiff: ").append(bestErrorDiff);

        return text.toString();
    }
}
