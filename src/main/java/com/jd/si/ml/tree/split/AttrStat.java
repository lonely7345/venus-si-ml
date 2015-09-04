package com.jd.si.ml.tree.split;


import com.jd.si.ml.core.Attribute;

import java.io.Serializable;

/**
 * Abstract Attribute Statistics
 */
public abstract class AttrStat implements Serializable {
    private Attribute attribute;

    public static AttrStat getAttrStat(Attribute attribute, Attribute classAttr) {
        if (attribute.isNumeric()) {
            return new NumAttrStat(attribute, classAttr);
        } else {
            return new DistAttrStat(attribute, classAttr);
        }
    }

    public AttrStat(Attribute attribute) {
        this.attribute = attribute;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public abstract void adjustCount(double attrValue, double classValue, int amount);
    public abstract int getCount(double attrValue, double classValue);
    public abstract int getCount(double attrValue);
}
