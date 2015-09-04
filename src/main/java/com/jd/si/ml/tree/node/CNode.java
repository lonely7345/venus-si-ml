package com.jd.si.ml.tree.node;



import com.jd.si.ml.core.Attribute;
import com.jd.si.ml.core.Instance;
import com.jd.si.ml.core.InstanceId;
import com.jd.si.ml.tree.CVFDT;
import com.jd.si.ml.tree.profiler.CVFDTProfiler;
import com.jd.si.ml.tree.split.AttrStat;
import com.jd.si.ml.tree.split.TestStats;

import java.util.*;

/**
 * Tree node of CVFDT
 */
public class CNode extends VNode
{
    private static final long serialVersionUID = 1L;

    /**
     * If the error difference decrease below this rate, prune this ALT node.
     */
    //private double minAltErrorDiff = 0.01;

    /**
     * A map of subtrees made from splitting on alternative Attributes (instead of
     * splitting on the Attribute specified in this.attribute).
     */
    protected Map<Attribute, CNode> altNodes = new LinkedHashMap<Attribute, CNode>();
    protected Map<Attribute, TestStats> altStats = new LinkedHashMap<Attribute, TestStats>();

    /**
     * @see InstanceId
     */
    protected int id;

    /**
     * Number of instances until entering/exiting next test phase.
     */
    protected int testCount = 0;

    /**
     * The number of correctly classified test instances.
     */
    //protected int testCorrectCount = 0;
    protected TestStats testStats;

    /**
     * If true, new data instances are not used to grow the tree. Instead, they are
     * used to compare the error rate of this VNode to that of its subtrees.
     */
    protected boolean testMode = false;

    public CNode(List<Attribute> attributes, Attribute classAttribute, int id, double parentClassValue,
                   int numSampledFeatures, int height) {
        //         int numSampledFeatures, double minAltErrorDiff) {
        super(attributes, classAttribute, parentClassValue, numSampledFeatures, height);
        this.id = id;
        this.testStats = TestStats.getTestStats(classAttribute);
        //this.minAltErrorDiff = minAltErrorDiff;
    }

    public CNode(Instance instance, Attribute classAttribute, int id, double parentClassValue,
                   int numSampledFeatures, int height) {
        //         int numSampledFeatures, double minAltErrorDiff) {
        super(instance.getAttributes(), classAttribute, parentClassValue, numSampledFeatures, height);
        this.id = id;
        this.testStats = TestStats.getTestStats(classAttribute);
        //this.minAltErrorDiff = minAltErrorDiff;
    }

    /**
     * Modified this VNode to look like the provided node. Does not deep copy fields.
     */
    @Override
    public void copyNode(VNode node) {
        super.copyNode(node);

        if (node instanceof CNode)
        {
            CNode cnode = (CNode) node;

            this.id = cnode.id;
            this.altNodes = cnode.altNodes;
            this.altStats = cnode.altStats;
            this.testStats = cnode.testStats;
            //this.bestScv = cnode.bestScv;
            //this.minAltErrorDiff = cnode.minAltErrorDiff;
        }
    }

    @Override
    public CNode getLeafNode(Instance instance)
    {
        return (CNode) getLeafNode(this, instance);
    }

    @Override
    public CNode getSuccessor(int value) {
        if (successors != null) {
            return (CNode) successors[value];
        }
        else {
            return null;
        }
    }

    public void setSampledFeatures(int[] sampledFeatures) {
        if (sampledFeatures != null) {
            this.sampledFeatures = sampledFeatures;
            this.numSampledFeatures = sampledFeatures.length;
            counts = new AttrStat[numTotalFeatures];
            if (numSampledFeatures > 0) {
                for (int i = 0; i < numSampledFeatures; i++) {
                    Attribute attribute = attributes.get(sampledFeatures[i]);
                    this.counts[sampledFeatures[i]] = AttrStat.getAttrStat(attribute, classAttribute);
                }
            } else {
                for (int i = 0; i < numTotalFeatures; i++) {
                    Attribute attribute = attributes.get(i);
                    this.counts[i] = AttrStat.getAttrStat(attribute, classAttribute);
                }
            }
        }
    }

    public Collection<CNode> getAlternativeTrees() {
        return altNodes.values();
    }

    /**
     * Determines the class prediction of the tree rooted at this node for the given instance.
     * Compares that prediction against the true class value, and if they are equal, increments
     * the correct test counter for this node.
     *
     * @param instance
     */
    public void testInstance(Instance instance) {
        // test this node
        double predicted = getLeafNode(instance).getScoreValue();
        double actual = instance.classValue();
        testStats.collect(actual, predicted);

        // test all alternative nodes
        Iterator<Attribute> iter = altNodes.keySet().iterator();
        while (iter.hasNext()) {
            Attribute attribute = iter.next();
            CNode alt = altNodes.get(attribute);
            TestStats stats = altStats.get(attribute);

            double altPredicted = alt.getLeafNode(instance).getScoreValue();
            stats.collect(actual, altPredicted);
        }
    }

    public void incrementTestCount(int testInterval, int testDuration, CVFDTProfiler profiler, double minAltErrorDiff) {
        // check whether we should enter or exit test mode
        this.testCount++;
        if (this.testMode) {
            if (this.testCount > testDuration) {
                endTest(profiler, minAltErrorDiff);
            }
        }
        else {
            if (this.testCount > testInterval) {
                startTest();
            }
        }
    }

    public boolean isTestMode() {
        return this.testMode;
    }

    public int getTestCount()
    {
        return this.testCount;
    }

    public double getTestError() {
        return testStats.getError();
    }

    public boolean doesAltNodeExist(int attributeIndex, double splitValue) {
        for (Attribute altAttribute : altNodes.keySet()) {
            if (altAttribute.index() == attributeIndex) {
                if ((getAttribute().isNumeric() && splitValue == getSplitValue()) ||
                        !getAttribute().isNumeric()) {
                    return true;
                }
            }
        }

        return false;
    }

    public void addAlternativeNode(Instance instance, Attribute attribute, int newId, double splitValue) {
        // create the alternative node and immediately split it on the new attribute
        //CNode node = new CNode(instance, classAttribute, newId, parentClassValue, numSampledFeatures, minAltErrorDiff);
        CNode node = new CNode(instance, classAttribute, newId, parentClassValue, numSampledFeatures, height);
        node.splitValue = splitValue;
        //node.bestScv = scv;
        node.split(attribute, instance, newId);
        node.setSampledFeatures(this.sampledFeatures);

        TestStats stats = TestStats.getTestStats(classAttribute);

        // the new alternative node should not be tested if this CNode is currently
        // in test mode (wait until the next test mode)
        if (isTestMode()) {
            stats.setNew(true);
        }

        altStats.put(attribute, stats);
        altNodes.put(attribute, node);
    }

    /**
     * Called when enough data instances have been seen that it is time to end test mode.
     */
    protected void endTest(CVFDTProfiler profiler, double minAltErrorDiff) {
        profiler.startTestALT();

        Attribute bestAttribute = null;
        CNode bestAlt = null;
        double bestErrorDiff = 0;
        double mainError = getTestError();
        Iterator<Attribute> iter = altNodes.keySet().iterator();
        while (iter.hasNext()) {
            Attribute attribute = iter.next();
            CNode alt = altNodes.get(attribute);
            TestStats stats = altStats.get(attribute);

            // if an alternative tree was created while we were in
            // test mode, it will not yet be in test mode and will
            // not have collected enough examples yet to evaluate it properly
            // so skip it until the next test mode
            if (!stats.isNew()) {
                double altError = stats.getError();
                double errorDiff = mainError - altError;
                //System.out.println("node " + id + ", errorDiff = " + errorDiff + ", bestErrorDiff = " + stats.getBestErrorDiff());
                // if the error difference improved, record the new improved value
                if (errorDiff > stats.getBestErrorDiff()) {
                    stats.setBestErrorDiff(errorDiff);
                }
                // if the error difference decreased by 'minAltErrorDiff' below the current
                // best, then drop the alternative node
                else if (errorDiff < stats.getBestErrorDiff() * (1 + minAltErrorDiff)) {
                    //TODO having two maps is awkward (it's only done because we sometimes
                    //     want lists of just CNodes)
                    //System.out.println("node " + id + " prune");
                    iter.remove();
                    altStats.remove(attribute);
                    profiler.pruneALT(alt);
                }

                // remember the alternative node with the best error
                if (bestErrorDiff < errorDiff) {
                    //System.out.println("node " + id + " activate");
                    bestAttribute = attribute;
                    bestErrorDiff = errorDiff;
                    bestAlt = alt;
                }
            }

            stats.reset();
        }

        // one of the alternative trees is better than the current tree!
        // replace this node with the alternative node
        if (bestAlt != null) {
            this.copyNode(bestAlt);
            // remove the alternative node which was promoted
            // from the list of alternative nodes
            this.altNodes.remove(bestAttribute);
            this.altStats.remove(bestAttribute);
            profiler.activateALT(bestAlt);
        }

        this.testStats.reset();
        this.testCount = 0;
        this.testMode = false;

        profiler.stopTestALT();
    }

    /**
     * Called when enough data instances have been seen that it is time to enter test mode.
     */
    protected void startTest()
    {
        //this.testCorrectCount = 0;
        this.testStats.reset();
        this.testCount = 0;

        // if there are no alternative nodes to test, don't enter test mode (wait another
        // testInterval instances then check again)
        this.testMode = !this.altNodes.isEmpty();
    }

    /**
     * Populate attribute statistics from window
     */
    public void populateStats(InstanceId instanceId) {
        if (attribute == null) {
            return;
        }

        // populate current node
        if (instanceId.getId() >= id) {
            incrementCounts(instanceId.getInstance(), CVFDT.StatMode.POPULATE);
        }

        // populate alt
        for (CNode alt : altNodes.values()) {
            alt.populateStats(instanceId);
        }

        // populate child node
        CNode child = null;
        if (attribute.isNumeric()) {
            if (instanceId.getInstance().value(attribute) <= splitValue) {
                child = getSuccessor(0);
            } else {
                child = getSuccessor(1);
            }
        } else {
            child = getSuccessor((int) instanceId.getInstance().value(attribute));
        }
        child.populateStats(instanceId);
    }

    public void clearCounts() {
        counts = null;
    }

    public void initCounts() {
        if (attribute == null) {
            return;
        }

        assert(counts == null);

        // init statistics
        counts = new AttrStat[numTotalFeatures];
        if (numSampledFeatures > 0) {
            for (int i = 0; i < numSampledFeatures; i++) {
                Attribute attribute = attributes.get(sampledFeatures[i]);
                this.counts[sampledFeatures[i]] = AttrStat.getAttrStat(attribute, classAttribute);
            }
        } else {
            for (int i = 0; i < numTotalFeatures; i++) {
                Attribute attribute = attributes.get(i);
                this.counts[i] = AttrStat.getAttrStat(attribute, classAttribute);
            }
        }

        // init alt nodes
        for (CNode alt : altNodes.values()) {
            alt.initCounts();
        }

        // init child nodes
        for (int i = 0; i < attribute.numValues(); i++) {
            getSuccessor(i).initCounts();
        }
    }

    public void split(Attribute attribute, Instance instance, int id) {
        split(attribute, instance, id, CVFDT.StatMode.NORM);
    }

    /**
     * Like {@code VNode#split(Attribute, Instance)}, but creates CNodes and
     * assigns the specified id to the VNode.
     */
    public void split(Attribute attribute, Instance instance, int id, CVFDT.StatMode statMode) {
        this.successors = new CNode[attribute.numValues()];
        this.attribute = attribute;

        for (int valueIndex = 0; valueIndex < attribute.numValues(); valueIndex++) {
            this.successors[valueIndex] = new CNode(instance, classAttribute, id, getScoreValue(),
                    numSampledFeatures, height + 1);
        }

        if (statMode == CVFDT.StatMode.LAZY) {
            counts = null;
        }
    }

    /**
     * @see InstanceId
     */
    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return toString(this, "", null, false);
    }

    @Override
    public String toString(boolean verbose) {
        return toString(this, "", null, verbose);
    }

    /**
     *
     * @param node
     * @param indent
     * @param altTestStats if altTestStats is not null, this node is a ALT node.
     * @param verbose
     * @return
     */
    protected String toString(CNode node, String indent, TestStats altTestStats, boolean verbose) {
        StringBuilder text = new StringBuilder();

        // current node
        text.append(node.getText(altTestStats, verbose)).append("\n");

        // child nodes
        if (node.getAttribute() != null) {
            for (int i = 0; i < node.successors.length; i++) {
                text.append(indent).append("|---");
                text.append(node.getText(i, verbose));
                CNode child = (CNode) node.successors[i];

                if (i == node.successors.length - 1 && (node.altNodes.size() == 0 || !verbose)) {
                    text.append(toString(child, indent + "    ", null, verbose));
                } else {
                    text.append(toString(child, indent + "|   ", null, verbose));
                }
            }
        }

        // alternative nodes
        if (verbose && node.altNodes.size() > 0) {
            int i = 0;
            for (Map.Entry<Attribute, CNode> entry : node.altNodes.entrySet()) {
                text.append(indent).append("|---");
                if (i++ == node.altNodes.size() - 1) {
                    text.append(toString(entry.getValue(), indent + "    ", node.altStats.get(entry.getKey()), verbose));
                } else {
                    text.append(toString(entry.getValue(), indent + "|   ", node.altStats.get(entry.getKey()), verbose));
                }
            }
        }

        return text.toString();
    }

    @Override
    protected String getText(boolean verbose) {
        return getText(null, verbose);
    }

    protected String getText(TestStats altTestStats, boolean verbose) {
        StringBuilder text = new StringBuilder();

        if (altTestStats != null) {
            text.append("[ALT: ");
        } else if (attribute != null) {
            text.append("[NODE: ");
        } else {
            text.append("[LEAF: ");
        }

        if (classAttribute.isNumeric()) {
            text.append(String.format("%.2f", getScoreValue()));
        } else {
            text.append(classAttribute.value((int) getScoreValue()));
        }

        text.append(", id: ").append(id);
        if (altNodes.size() > 0) {
            text.append(", alt: ").append(altNodes.size());
        }

        if (verbose) {
            text.append(", instances: ").append(totalCount);

            if (verbose) {
                if (!classAttribute.isNumeric()) {
                    text.append(", classes: ");
                    for (int classCount : getClassCounts()) {
                        text.append(classCount + "/");
                    }
                }
            }

            if (testMode) {
                text.append(", testMode: ").append(testMode);
                text.append(", testStats: ").append(testStats.getError());
            }
            if (altTestStats != null) {
                text.append(", ").append(altTestStats.toString());
            }

            if (sampledFeatures != null) {
                text.append(", features: <");
                for (int idx : sampledFeatures) {
                    text.append(idx + "-" + attributes.get(idx).name()).append(", ");
                }
                text.append(">");
            }
        }

        text.append("]");

        return text.toString();
    }

    @Override
    protected String getText(int i, boolean verbose) {
        StringBuilder text = new StringBuilder();

        text.append(attribute.name()).append(" ");
        if (attribute.isNumeric()) {
            if (i == 0) {
                text.append("<= ");
            } else {
                text.append("> ");
            }

            text.append(String.format("%.2f", splitValue));
        } else {
            text.append("= ").append(attribute.value(i));
        }

        text.append(": ");

        return text.toString();
    }

    public int getTotalSize() {
        int size = 1;

        for (CNode alt : altNodes.values()) {
            size += alt.getTotalSize();
        }

        if (successors != null) {
            for (VNode successor : successors) {
                size += ((CNode) successor).getTotalSize();
            }
        }

        return size;
    }
}

