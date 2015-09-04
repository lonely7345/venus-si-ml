package com.jd.si.ml.tree.profiler;

import java.io.Serializable;

/**
 * A profiler for SDT
 */
public class SDTProfiler extends AbstractProfiler implements Serializable {
    protected ProfileMeasurement scoring;

    public SDTProfiler() {
        super();

        scoring = new ProfileMeasurement("scoring");
    }

    public void startScoring() {
        scoring.start();
    }

    public void stopScoring() {
        scoring.stop();
    }

    public long getScoringCount() {
        return scoring.getCount();
    }

    public double getScoringCost() {
        return scoring.getCost();
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append(scoring.toString()).append("\n");
        return ret.toString();
    }

    /*
    @Override
    public ProfileResult getProfileResult() {
        return new SDTProfileResult();
    }

    public class SDTProfileResult extends ProfileResult implements Serializable {
        protected long numScoring;
        protected double costScoring;

        public SDTProfileResult() {
            this.numScoring = scoring.getCount();
            this.costScoring = scoring.getCost();
        }
    }
    */
}
