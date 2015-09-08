package com.jd.si.venus.algorithm.rf.model.tree.split;

import java.io.Serializable;

/**
 * Discrete Class Statistics
 */
public class DistClassStat extends ClassStat implements Serializable {
    private int[] counts;
    private int totalCount;

    public DistClassStat(int numClasses) {
        counts = new int[numClasses];
        for (int i = 0; i < numClasses; i++) {
            counts[i] = 0;
        }

        totalCount = 0;
    }

    @Override
    public void adjustCount(double classIndex, int amount) {
        counts[(int) classIndex] += amount;
        totalCount += amount;
    }

    @Override
    public int getCount(double classIndex) {
        return counts[(int) classIndex];
    }

    @Override
    public int getCount() {
        return totalCount;
    }

    @Override
    public int[] getCounts() {
        return counts;
    }
    
	@Override
	public void setCounts(int[] counts) {
		if (this.counts.length != counts.length) {
			throw new RuntimeException("It is not the same of parameter counts with this.counts.");
		} else {
			for (int i = 0; i < counts.length; i++) {
				this.counts[i] = counts[i];
			}
		}
	}

    @Override
    public double getSum() {
        throw new RuntimeException("It is not supported to get sum for a discrete class (Decision Tree)");
    }

    @Override
    public double getSquaredSum() {
        throw new RuntimeException("It is not supported to get squared sum fro a discrete class (Decision Tree)");
    }

	@Override
	public void setSum(double sum) {
		 throw new RuntimeException("It is not supported to set sum for a discrete class (Decision Tree)");
		
	}

	@Override
	public void setSquaredSum(double squaredSum) {
        throw new RuntimeException("It is not supported to set squared sum fro a discrete class (Decision Tree)");		
	}

	@Override
	public void setCount(int count) {
        throw new RuntimeException("It is not supported to set count fro a discrete class (Decision Tree)");		
	}

}
