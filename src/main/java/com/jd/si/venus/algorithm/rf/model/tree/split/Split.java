package com.jd.si.venus.algorithm.rf.model.tree.split;


import com.jd.si.venus.algorithm.rf.model.core.Attribute;

/**
 * Split information
 */
public class Split {
    protected Attribute attr;
    protected double scv;
    protected double splitValue;

    public Split(Attribute attr, double scv, double splitValue) {
        this.attr = attr;
        this.scv = scv;
        this.splitValue = splitValue;
    }

    public Attribute getAttr() {
        return attr;
    }

    public void setAttr(Attribute attr) {
        this.attr = attr;
    }

    public double getScv() {
        return scv;
    }

    public void setScv(double scv) {
        this.scv = scv;
    }

    public double getSplitValue() {
        return splitValue;
    }

    public void setSplitValue(double splitValue) {
        this.splitValue = splitValue;
    }
}
