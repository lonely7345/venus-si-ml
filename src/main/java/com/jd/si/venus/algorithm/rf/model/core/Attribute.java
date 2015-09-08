package com.jd.si.venus.algorithm.rf.model.core;

import java.io.Serializable;
import java.util.*;

/**
 * Attribute meta
 */
public class Attribute implements Serializable {
    private int index;
    private String name;
    private boolean isCont;
    private List<String> distValList;
    private Map<String, Integer> val2Idxs;
    private boolean isTarget;
    private String distDefault = "";
    private double numDefault = 0.0;
    
    /**
     * Init default value for Discrete Attribute.
     * 
     * @param index
     * @param colName
     * @param isCont
     * @param distVals
     * @param targetIdx
     * @param distDefault
     */
    public Attribute(int index, String colName, boolean isCont, String[] distVals, int targetIdx, String distDefault) {
    	this(index, colName, isCont, distVals, targetIdx);
    	this.distDefault = distDefault;
    }
    
    /**
     * Init default value for Numeric Attribute.
     * 
     * @param index
     * @param colName
     * @param isCont
     * @param distVals
     * @param targetIdx
     * @param numDefault
     */
    public Attribute(int index, String colName, boolean isCont, String[] distVals, int targetIdx, double numDefault) {
    	this(index, colName, isCont, distVals, targetIdx);
    	this.numDefault = numDefault;
    }

    public Attribute(int index, String colName, boolean isCont, String[] distVals, int targetIdx) {
        this.index = index;
        this.name = colName;
        this.isCont = isCont;

        if (!isCont) {
            distValList = new ArrayList<String>();
            for (int i = 0; i < distVals.length; i++) {
                distValList.add(distVals[i]);
            }
            Collections.sort(distValList);
            val2Idxs = new HashMap<String, Integer>();
            for (int i = 0; i < distValList.size(); i++) {
                val2Idxs.put(distValList.get(i), i);
            }
        }
        this.isTarget = (index == targetIdx);
    }

    public int index() {
        return index;
    }

    public boolean isNumeric() {
        return isCont;
    }

    public String name() {
        return name;
    }
    
    public String getDistDefault() {
    	return distDefault;
    }
    
    public double getNumDefault() {
    	return numDefault;
    }
        
    public String value(int index) {
        if (isCont) {
            throw new RuntimeException("Can not get discrete value for a numeric attribute.");
        }
        return distValList.get(index);
    }

    public int index(String value) {
        if (isCont) {
            throw new RuntimeException("Can not get indexed id for a numeric attribute.");
        }
        return val2Idxs.get(value);
    }

    public double value(Object o) {
    	// Deal with null values
    	if (o == null) {
    		if (isCont) {
    			return getNumDefault();
    		} else {
    			return index(getDistDefault());
    		}
    	} else {
		    if (isCont) {
		        return (Double) o;
		    } else {
		        return index((String) o);
		    }
    	}
    }

    /**
     * For continuous feature, it return 2;
     * For descrete feature, it return the number of distinct values;
     * @return
     */
    public int numValues() {
        if (isCont) {
            return 2;
        } else {
            return distValList.size();
        }
    }

    @Override
    public String toString() {
        return "Attribute{" +
                "index=" + index +
                ", name='" + name + '\'' +
                ", isCont=" + isCont +
                ", distValList=" + distValList +
                ", val2Idxs=" + val2Idxs +
                ", isTarget=" + isTarget +
                '}';
    }
}
