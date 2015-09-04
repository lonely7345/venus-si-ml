package com.jd.si.ml.tree.profiler;

import java.io.Serializable;

/**
 * A tree profiler.
 */
public abstract class AbstractProfiler implements Serializable {
    //public abstract ProfileResult getProfileResult();

    public static String getMemoryUsage() {
        StringBuilder usage = new StringBuilder("Memory usage: ");

        Runtime runtime = Runtime.getRuntime();
        final double _1M = 1024d * 1024d;
        usage.append("used: ").append((runtime.totalMemory() - runtime.freeMemory()) / _1M).
                append("M, total: ").append(runtime.totalMemory() / _1M).
                append("M, max: ").append(runtime.maxMemory() / _1M).append("M\n");

        return usage.toString();
    }

    /*
    public abstract class ProfileResult implements Serializable {

    }
    */

    public class ProfileMeasurement implements Serializable {
        private static final double NANOS_PER_MILL = 1000d * 1000d;
        private String measurementName;
        private long count;
        private long nanos;
        transient private boolean start;
        transient private long nanoStart;

        public ProfileMeasurement(String name) {
            measurementName = name;
            count = 0l;
            nanos = 0l;
            start = false;
            nanoStart = System.nanoTime();
        }

        public void start() {
            assert(!start);

            nanoStart = System.nanoTime();
            count++;
            start = true;
        }

        public void stop() {
            assert(start);

            nanos += (System.nanoTime() - nanoStart);
            start = false;
        }

        public long getCount() {
            return count;
        }

        public double getCost() {
            return nanos / NANOS_PER_MILL;
        }

        @Override
        public String toString() {
            StringBuilder ret = new StringBuilder();
            ret.append("# of ").append(measurementName).append(" is ").
                    append(count).append(", taking ").append(nanos / NANOS_PER_MILL).
                    append(" mill seconds.");

            return ret.toString();
        }
    }
}
