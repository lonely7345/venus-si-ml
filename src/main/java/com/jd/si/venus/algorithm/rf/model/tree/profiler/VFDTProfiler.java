package com.jd.si.venus.algorithm.rf.model.tree.profiler;


import com.jd.si.venus.algorithm.rf.model.tree.VFDT;

import java.io.Serializable;

/**
 * A profiler for VFDT
 */
public class VFDTProfiler extends SDTProfiler implements Serializable {
    protected ProfileMeasurement training;
    protected ProfileMeasurement checkNodeSplit;
    protected VFDT vfdt;
    protected int numTotalSplit;
    protected int numTieSplit;
    protected int numPreprune;

    public VFDTProfiler(VFDT vfdt) {
        this.training = new ProfileMeasurement("training");
        this.checkNodeSplit = new ProfileMeasurement("checkNodeSplit");
        this.vfdt = vfdt;
        this.numTotalSplit = 0;
        this.numTieSplit = 0;
        this.numPreprune = 0;
    }

    public void addPreprune() {
        numPreprune++;
    }

    public int getNumPreprune() {
        return numPreprune;
    }

    public void addTieSplit() {
        numTieSplit++;
    }

    public void addTotalSplit() {
        numTotalSplit++;
    }

    public int getNumTieSplit() {
        return numTieSplit;
    }

    public int getNumTotalSplit() {
        return numTotalSplit;
    }

    public void startTraining() {
        training.start();
    }

    public void stopTraining() {
        training.stop();
    }

    public long getTrainingCount() {
        return training.getCount();
    }

    public double getTrainingCost() {
        return training.getCost();
    }

    public void startCheckNodeSplit() {
        checkNodeSplit.start();
    }

    public void stopCheckNodeSplit() {
        checkNodeSplit.stop();
    }

    public int getTreeSize() {
        return vfdt.getRoot().getTreeSize();
    }

    public long getCheckNodeSplitCount() {
        return checkNodeSplit.getCount();
    }

    public double getCheckNodeSplitCost() {
        return checkNodeSplit.getCost();
    }


    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder(super.toString());
        ret.append(training.toString()).append("\n");
        ret.append(checkNodeSplit.toString()).append("\n");
        ret.append("# of total split is ").append(getNumTotalSplit()).append("\n");
        ret.append("# of tie split is ").append(getNumTieSplit()).append("\n");
        ret.append("# of preprune is ").append(numPreprune).append("\n");
        return ret.toString();
    }

    /*
    @Override
    public ProfileResult getProfileResult() {
        return new VFDTProfileResult();
    }

    public class VFDTProfileResult extends SDTProfileResult implements Serializable {
        protected long numTraining;
        protected double costTraining;
        protected long numCheckNodeSplit;
        protected double costCheckNodeSplit;
        protected int numTreeSize;

        public VFDTProfileResult() {
            super();
            numTraining = training.getCount();
            costTraining = training.getCost();
            numCheckNodeSplit = checkNodeSplit.getCount();
            costCheckNodeSplit = checkNodeSplit.getCost();
            numTreeSize = vfdt.getRoot().getTreeSize();
        }
    }
    */
}
