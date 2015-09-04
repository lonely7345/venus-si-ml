package com.jd.si.ml.tree.split;


import com.jd.si.ml.core.Attribute;

import java.io.Serializable;

/**
 * Discrete Attribute Statistics
 */
public class DistAttrStat extends AttrStat implements Serializable {
    private ClassStat[] counts;

    public DistAttrStat(Attribute attribute, Attribute classAttr) {
        super(attribute);
        counts = new ClassStat[attribute.numValues()];
        for (int i = 0; i < counts.length; i++) {
            counts[i] = ClassStat.getClassStat(classAttr);
        }
    }

    @Override
    public void adjustCount(double attrValueIndex, double classValue, int amount) {
        counts[(int) attrValueIndex].adjustCount(classValue, amount);
    }

    @Override
    public int getCount(double attrValueIndex, double classValue) {
        return counts[(int) attrValueIndex].getCount(classValue);
    }

    @Override
    public int getCount(double attrValueIndex) {
        return counts[(int) attrValueIndex].getCount();
    }

    public ClassStat[] getCounts() {
        return counts;
    }
    
    public void setCounts(int index, ClassStat classStat) {
    	counts[index] = classStat;
    }
    
}
