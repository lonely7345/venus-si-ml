package com.jd.si.ml.tree.split;


import com.jd.si.ml.core.Attribute;

import java.io.Serializable;
import java.util.*;

/**
 * Numeric Attribute Statistics
 */
public class NumAttrStat extends AttrStat implements Serializable {
    protected int histogramSize = 10;

    protected Attribute classAttr;
    //protected TreeMap<Double, ClassStat> counts;
    //protected Histogram histogram;

    protected NumAttrData data;

    public NumAttrStat(Attribute attribute, Attribute classAttr) {
        super(attribute);
        this.data = new Raw();
        //this.data = new Histogram(10);
        this.classAttr = classAttr;
    }

    @Override
    public void adjustCount(double attrValue, double classValue, int amount) {
        data.update(attrValue, classValue, amount);
    }

    @Override
    public int getCount(double attrValue, double classValue) {
        //ClassStat classStat = counts.get(attrValue);
        ClassStat classStat = data.getData().get(attrValue);

        if (classStat == null) {
            return 0;
        } else {
            return classStat.getCount(classValue);
        }
    }

    @Override
    public int getCount(double attrValue) {
        //ClassStat classStat = counts.get(attrValue);
        ClassStat classStat = data.getData().get(attrValue);

        if (classStat == null) {
            return 0;
        } else {
            return classStat.getCount();
        }
    }

    public TreeMap<Double, ClassStat> getCounts() {
        return data.getData();
        //return histogram.pointsMap;
    }

    public abstract class NumAttrData implements Serializable {
        public abstract void update(double attrValue, double classValue, int amount);
        public abstract TreeMap<Double, ClassStat> getData();
    }

    public class Raw extends NumAttrData implements Serializable {
        protected TreeMap<Double, ClassStat> data;

        public Raw() {
            data = new TreeMap<Double, ClassStat>();
        }

        @Override
        public void update(double attrValue, double classValue, int amount) {
            ClassStat classStat = data.get(attrValue);
            if (classStat == null) {
                classStat = ClassStat.getClassStat(classAttr);
                data.put(attrValue, classStat);
            }
            classStat.adjustCount(classValue, amount);

            if (amount < 0 && classStat.getCount() == 0) {
                data.remove(attrValue);
            }
        }

        @Override
        public TreeMap<Double, ClassStat> getData() {
            return data;
        }
    }

    public class Histogram extends NumAttrData implements Serializable {
        protected TreeMap<Double, ClassStat> pointsMap;
        protected ClassStat[] pointsArr;
        protected HashMap<ClassStat, Short> points2Idx;
        protected short pointsIdx;
        protected int size;
        //protected int windowIdx = 0;
        //protected List<Short> pointsList;
        //protected short[] inst2hist;
        protected List<Short> inst2hist;

        public Histogram(int size) {
            this.pointsMap = new TreeMap<Double, ClassStat>();
            this.size = size;
            this.pointsArr = new ClassStat[size];
            this.points2Idx = new HashMap<ClassStat, Short>();
            this.pointsIdx = 0;
            //pointsList = new ArrayList<Short>();
            //this.inst2hist = new short[WINDOW_SIZE];
            this.inst2hist = new LinkedList<Short>();//new ArrayList<Short>();
        }

        /**
         * Replace the bins (qi, ki), (qi+1, ki+1) by the bin
         *   qi * ki + qi+1ki+1
         * ( ------------------, ki + ki+1)
         *      ki + ki+1
         * @param attrValue
         * @param classValue
         * @param amount
         */
        @Override
        public void update(double attrValue, double classValue, int amount) {
            ClassStat stat = null;

            if (amount > 0) {
                if (pointsMap.size() < size) {
                    if (!pointsMap.containsKey(attrValue)) {
                        assert (amount > 0);
                        stat = ClassStat.getClassStat(classAttr);
                        pointsMap.put(attrValue, stat);
                        points2Idx.put(stat, pointsIdx);
                        pointsArr[pointsIdx++] = stat;
                    } else {
                        stat = pointsMap.get(attrValue);
                    }
                } else {
                    if (pointsMap.containsKey(attrValue)) {
                        stat = pointsMap.get(attrValue);
                    } else {
                        assert (amount > 0);
                        Map.Entry<Double, ClassStat> nearestPoint = getNearestPoint(attrValue);
                        stat = nearestPoint.getValue();
                        double newAttrValue = (nearestPoint.getKey() * stat.getCount() + attrValue * amount) /
                                (double) (stat.getCount() + amount);
                        pointsMap.remove(nearestPoint.getKey());
                        pointsMap.put(newAttrValue, stat);
                    }
                }

                //windowIdx++;
                inst2hist.add(points2Idx.get(stat));
            } else {
                assert(inst2hist.size() > 0);
                short pointIdx = inst2hist.remove(0);
                stat = pointsArr[pointIdx];
            }

            stat.adjustCount(classValue, amount);
            // TODO
            /*
            if (stat.getCount() == 0) {

            }
            */
        }

        @Override
        public TreeMap<Double, ClassStat> getData() {
            return pointsMap;
        }

        public Map.Entry<Double, ClassStat> getNearestPoint(double attrValue) {
            Map.Entry<Double, ClassStat> higher = pointsMap.higherEntry(attrValue);
            Map.Entry<Double, ClassStat> lower = pointsMap.lowerEntry(attrValue);

            assert(higher != null || lower != null);

            if (higher == null) {
                return lower;
            } else if (lower == null) {
                return higher;
            } else { // higher != null && lower != null
                double diffH = Math.abs(higher.getKey() - attrValue);
                double diffL = Math.abs(lower.getKey() - attrValue);
                if (diffH < diffL) {
                    return higher;
                } else if (diffH > diffL) {
                    return lower;
                } else { // diffH == diffL
                    return higher;
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();

        /*
        if (histogram != null) {
            ret.append("Histogram data: ");
            for (Map.Entry<Double, ClassStat> entry : histogram.pointsMap.entrySet()) {
                ret.append(entry.getKey()).append(" -> ").append(entry.getValue().getCount()).append("\n");
            }
        }

        ret.append("Actual data: ");
        ret.append("counts size = " + counts.size());
        for (Map.Entry<Double, ClassStat> entry : counts.entrySet()) {
            ret.append(entry.getKey()).append(" -> ").append(entry.getValue().getCount()).append("\n");
        }*/

        return ret.toString();
    }
}
