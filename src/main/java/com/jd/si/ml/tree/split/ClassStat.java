package com.jd.si.ml.tree.split;


import com.jd.si.ml.core.Attribute;

import java.io.Serializable;

/**
 * Abstract class Statistics
 */
public abstract class ClassStat implements Serializable {
    public abstract void adjustCount(double classValue, int amount);
    public abstract int getCount(double classValue);
    public abstract int getCount();
    public abstract int[] getCounts();
    public abstract void setCounts(int[] counts);
    public abstract double getSum();
    public abstract double getSquaredSum();
    public abstract void setSum(double sum);
    public abstract void setSquaredSum(double squaredSum);
    public abstract void setCount(int count);
    

    public static ClassStat getClassStat(Attribute classAttr) {
        if (classAttr.isNumeric()) {
            return new NumClassStat();
        } else {
            return new DistClassStat(classAttr.numValues());
        }
    }
}
