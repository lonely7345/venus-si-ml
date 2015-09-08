package com.jd.si.venus.algorithm.rf.model.tree.profiler;



import com.jd.si.venus.algorithm.rf.model.tree.CVFDT;
import com.jd.si.venus.algorithm.rf.model.tree.node.CNode;

import java.io.Serializable;

/**
 * A profiler for CVFDT
 */
public class CVFDTProfiler extends VFDTProfiler implements Serializable {
    // recheck node split tries to generate ALT
    protected ProfileMeasurement recheckNodeSplit;
    protected ProfileMeasurement testALT;

    protected int numALTPruning;
    protected int numALTNodesPruning;
    protected int numALTActivate;
    protected int numALTNodesActivate;
    protected int numALTTotalSplit;
    protected int numALTTieSplit;

    public CVFDTProfiler(CVFDT cvfdt) {
        super(cvfdt);
        recheckNodeSplit = new ProfileMeasurement("recheckNodeSplit");
        testALT = new ProfileMeasurement("testALT");
        numALTPruning = 0;
        numALTActivate = 0;
        numALTNodesPruning = 0;
        numALTNodesActivate = 0;
        numALTTotalSplit = 0;
        numALTTieSplit = 0;
    }

    public void addNumALTTotalSplit() {
        numALTTotalSplit++;
    }

    public void addNumALTTieSplit() {
        numALTTieSplit++;
    }

    public int getNumALTTotalSplit() {
        return numALTTotalSplit;
    }

    public int getNumALTTieSplit() {
        return numALTTieSplit;
    }

    public void startRecheckNodeSplit() {
        recheckNodeSplit.start();
    }

    public void stopRecheckNodeSplit() {
        recheckNodeSplit.stop();
    }

    public long getRecheckNodeSplitCount() {
        return recheckNodeSplit.getCount();
    }

    public double getRecheckNodeSplitCost() {
        return recheckNodeSplit.getCost();
    }

    public void startTestALT() {
        testALT.start();
    }

    public void stopTestALT() {
        testALT.stop();
    }

    public long getTestALTCount() {
        return testALT.getCount();
    }

    public double getTestALTCost() {
        return testALT.getCost();
    }

    public void pruneALT(CNode alt) {
        numALTPruning++;
        numALTNodesPruning += alt.getTotalSize();
    }

    public int getNumALTPruning() {
        return numALTPruning;
    }

    public int getNumALTNodesPruning() {
        return numALTNodesPruning;
    }

    public void activateALT(CNode alt) {
        numALTActivate++;
        numALTNodesActivate += alt.getTotalSize();
    }

    public int getNumALTActivate() {
        return numALTActivate;
    }

    public int getNumALTNodesActivate() {
        return numALTNodesActivate;
    }

    @Override
    public int getTreeSize() {
        return ((CVFDT) vfdt).getRoot().getTreeSize();
    }

    public int getALTSize() {
        return ((CVFDT) vfdt).getALTSize();
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder(super.toString());

        ret.append(recheckNodeSplit.toString()).append("\n");
        ret.append(testALT.toString()).append("\n");
        ret.append("# of ALT pruning is ").append(numALTPruning).
                append(", # of nodes is ").append(numALTNodesPruning).append("\n");
        ret.append("# of ALT activate is  ").append(numALTActivate)
                .append(", # of nodes is ").append(numALTNodesActivate).append("\n");
        ret.append("# of ALT total split is ").append(numALTTotalSplit).append("\n");
        ret.append("# of ALT tie split is ").append(numALTTieSplit).append("\n");

        return ret.toString();
    }

    /*
    @Override
    public ProfileResult getProfileResult() {
        return new CVFDTProfileResult();
    }

    public class CVFDTProfileResult extends VFDTProfileResult implements Serializable {
        protected long numRecheckNodeSplit;
        protected double costRecheckNodeSplit;
        //protected long num
        protected int numALT;
        protected int numALTPruning;
        protected int numALTActivate;

        public CVFDTProfileResult() {
            super();
            this.numRecheckNodeSplit = recheckNodeSplit.getCount();
            this.costRecheckNodeSplit = recheckNodeSplit.getCost();
            CVFDT cvfdt = (CVFDT) vfdt;
            this.numALT = cvfdt.getRoot().getTotalSize();
        }
    }
    */
}
