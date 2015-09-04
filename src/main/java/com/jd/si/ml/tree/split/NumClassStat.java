package com.jd.si.ml.tree.split;

import java.io.Serializable;

/**
 * Numeric Class Statistics
 */
public class NumClassStat extends ClassStat implements Serializable {
    private int count;
    private double squaredSum;
    private double sum;

    public NumClassStat() {
        super();

        count = 0;
        squaredSum = 0d;
        sum = 0d;
    }

    @Override
    public void adjustCount(double classValue, int amount) {
        count += amount;
        sum += classValue * amount;
        squaredSum += Math.pow(classValue, 2) * amount;
    }

    @Override
    public int getCount(double classValue) {
        throw new RuntimeException("It is not supported to get count for a numeric class (Regression Tree)");
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public int[] getCounts() {
        throw new RuntimeException("It is not supported to get classes counts for a numeric class (Regression Tree)");
    }
    
	@Override
	public void setCounts(int[] counts) {
        throw new RuntimeException("It is not supported to set classes counts for a numeric class (Regression Tree)");
	}

    @Override
    public double getSquaredSum() {
        return squaredSum;
    }

    @Override
    public double getSum() {
        return sum;
    }
    
    @Override
    public void setSquaredSum(double squaredSum) {
    	this.squaredSum = squaredSum;
    }
    
    @Override
    public void setSum(double sum) {
    	this.sum = sum;
    }
    
    @Override
    public void setCount(int count) {
    	this.count = count;
    }
}
