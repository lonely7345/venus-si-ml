package com.jd.si.venus.algorithm.rf.model.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Instance
 */
public class Instance implements Serializable {
    //private Attribute[] attributes;
    protected List<Attribute> attributes = new ArrayList<Attribute>();

    private Attribute classAttr;
    private double[] values;

    public Instance(List<Attribute> attributes, Object[] oValues, Attribute classAttr) {
        this.attributes = attributes;
        this.classAttr = classAttr;

        this.values = new double[attributes.size()];

        // training case
        if (attributes.size() == oValues.length) {
            for (int i = 0; i < attributes.size(); i++) {
                this.values[i] = attributes.get(i).value(oValues[i]);
            }
        }
        // scoring case
        else if (attributes.size() - 1 == oValues.length) {
            for (int i = 0, j = 0; i < oValues.length; i++, j++) {
                if (i == classAttr.index()) {
                    continue;
                }

                this.values[j] = attributes.get(j).value(oValues[j]);
            }
        } else {
            StringBuilder ret = new StringBuilder();
            for (Object o : oValues) {
                ret.append(o).append(",");
            }
            throw new RuntimeException("Can not recognize instance " + ret.toString() +
                    ", attributes length " + attributes.size() + ", given instance length " + oValues.length);
        }
    }

    public boolean hasMissingValue() {
        return false;
    }

    public int numAttributes() {
        return attributes.size();
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public Attribute attribute(int index) {
        return attributes.get(index);
    }

    public double value(int index) {
        return value(index);
    }

    public double value(Attribute attr) {
        return values[attr.index()];
    }

    public double classValue() {
        return values[classAttr.index()];
    }

    public int numClasses() {
        return classAttr.numValues();
    }

    @Override
    public String toString() {
        return "Instance{" +
                "attributes=" + Arrays.toString(attributes.toArray()) +
                ", classAttr=" + classAttr +
                ", values=" + Arrays.toString(values) +
                '}';
    }
}
