package com.jd.si.venus.algorithm.rf.model.tree;


import com.jd.si.venus.algorithm.rf.model.OnlineModel;
import com.jd.si.venus.algorithm.rf.model.core.Attribute;
import com.jd.si.venus.algorithm.rf.model.core.Instance;
import com.jd.si.venus.algorithm.rf.model.core.InstanceId;
import com.jd.si.venus.algorithm.rf.model.tree.profiler.AbstractProfiler;
import com.jd.si.venus.algorithm.rf.model.tree.profiler.CVFDTProfiler;
import com.jd.si.venus.algorithm.rf.model.tree.split.Split;
import com.jd.si.venus.algorithm.rf.model.tree.node.CNode;
import com.jd.si.venus.algorithm.rf.model.tree.node.VNode;

import java.util.*;
import java.util.logging.Logger;

/**
 * CVFDT
 */
public class CVFDT extends VFDT
{
    private static final Logger logger = Logger.getLogger(CVFDT.class.getName());

    private static final long serialVersionUID = 1L;

    /**
     * Linked list of instances currently inside the CVFDT learning window.
     */
    protected LinkedList<InstanceId> window = new LinkedList<InstanceId>();

    /**
     * The maximum size of the window list.
     */
    protected int windowSize = 200000;

    /**
     * The number of data instances between rechecks of the validity of all
     * alternative trees. This is a global count on instances (see
     * splitValidityCounter).
     */
    protected int splitRecheckInterval = 50000;

    /**
     * Every altTestModeInterval instances, CNodes enter a test state where they
     * use the next altTestModeDuration instances to evaluate whether or not
     * to discard the current tree in favor of one of the VNode's alternative trees.
     */
    protected int testInterval = 90000;
    protected int testDuration = 10000;

    /**
     * If the error difference decrease below this rate, prune this ALT node.
     */
    private double minAltErrorDiff = 0.01;

    protected int largestNodeId = 0;
    protected int splitValidityCounter = 0;

    //protected boolean lazyMode = false;
    public enum StatMode {NORM, LAZY, POPULATE};
    protected StatMode statMode = StatMode.NORM;

    protected CVFDT(int numCols, String[] colNames, boolean[] isCont, String[][] distVals, int targetIdx) {
        super(numCols, colNames, isCont, distVals, targetIdx);
    }

    public CVFDT(int numCols, String[] colNames, boolean[] isCont, String[][] distVals, int targetIdx, int numClasses) {
        super(numCols, colNames, isCont, distVals, targetIdx, numClasses);
    }

    public CVFDT(int numCols, String[] colNames, boolean[] isCont, String[][] distVals, int targetIdx, int numClasses, int numSampledFeatures) {
        super(numCols, colNames, isCont, distVals, targetIdx, numClasses, numSampledFeatures);
    }

    public CVFDT(int numCols, String[] colNames, boolean[] isCont, String[][] distVals, int targetIdx, double targetRange) {
        super(numCols, colNames, isCont, distVals, targetIdx, targetRange);
    }

    public CVFDT(int numCols, String[] colNames, boolean[] isCont, String[][] distVals, int targetIdx, double targetRange, int numSampledFeatures) {
        super(numCols, colNames, isCont, distVals, targetIdx, targetRange, numSampledFeatures);
    }

    public double getMinAltErrorDiff() {
        return minAltErrorDiff;
    }

    public void setMinAltErrorDiff(double minAltErrorDiff) {
        this.minAltErrorDiff = minAltErrorDiff;
    }

    public int getWindowSize()
    {
        return windowSize;
    }

    public void setWindowSize(int windowSize)
    {
        this.windowSize = windowSize;
    }

    public int getSplitRecheckInterval()
    {
        return splitRecheckInterval;
    }

    public void setSplitRecheckInterval(int splitRecheckInterval)
    {
        this.splitRecheckInterval = splitRecheckInterval;
    }

    public int getTestInterval()
    {
        return testInterval;
    }

    public void setTestInterval(int testInterval)
    {
        this.testInterval = testInterval;
    }

    public int getTestDuration()
    {
        return testDuration;
    }

    public void setTestDuration(int testDuration)
    {
        this.testDuration = testDuration;
    }

    public void setLazyMode(boolean lazyMode) {
        if (lazyMode) {
            statMode = StatMode.LAZY;
        } else {
            statMode = StatMode.NORM;
        }
    }

    @Override
    public CNode getRoot()
    {
        return (CNode) root;
    }

    @Override
    protected void init() {
        // create root node
        root = new CNode(attributes, classAttribute, largestNodeId, Double.NaN, numSampledFeatures, 0);
    }

    @Override
    public AbstractProfiler initProfiler() {
        return new CVFDTProfiler(this);
    }

    public CVFDTProfiler getProfiler() {
        return (CVFDTProfiler) treeProfiler;
    }

    @Override
    public void addInstance(Instance instance) {
        CVFDTProfiler profiler = getProfiler();
        profiler.startTraining();

        if (splitValidityCounter % 200000 == 0) {
            if (verbose) {
                System.out.println(debug());
                //generateRules(getRoot(), "");
            }
        }
        try {
            // update the counts associated with this instance
            // unlike VFDTProfiler, we start at the root because we will reach multiple
            // leaf nodes in the various alternative trees
            traverseAndIncrementCounts(instance, getRoot());

            // add the new instance to the window and remove old instance (if necessary)
            updateWindow(instance);

            // split nodes with attributes which have surpassed the hoeffding bound
            // and/or test the alternative subtrees for nodes in test mode
            traverseAndSplitOrTest(instance, getRoot());

            // check whether new alternative nodes should be created
            if (++splitValidityCounter % splitRecheckInterval == 0) {
                if (statMode == StatMode.LAZY) {
                    populateStats();
                }

                traverseAndCheckSplitValidity(instance, getRoot());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        profiler.stopTraining();
    }

    /**
     * If in a lazy mode, all internal nodes don't keep any statistics until we use it.
     */
    protected void populateStats() {
        CNode root = getRoot();

        root.initCounts();
        for (InstanceId instanceId : window) {
            root.populateStats(instanceId);
        }
    }

    /**
     * Traverse the entire tree and determine if new alternative trees should be created.
     */
    protected void traverseAndCheckSplitValidity(Instance instance, CNode node)
    {
        // only check the validity of split for non-leaf node (i.e. nodes with splits)
        if (node.getAttribute() != null) {
            // check the validity of the split on node.getAttribute() by
            // potentially creating a node with an alternative split
            CVFDTProfiler profiler = getProfiler();
            profiler.startRecheckNodeSplit();
            recheckNodeSplit(instance, node);
            profiler.stopRecheckNodeSplit();

            if (statMode == StatMode.LAZY) {
                node.clearCounts();
            }

            // traverse into all the alternative nodes
            for (CNode alt : node.getAlternativeTrees()) {
                traverseAndCheckSplitValidity(instance, alt);
            }

            // descend into all child nodes
            int numValues = node.getAttribute().numValues();
            for (int attributeValue = 0; attributeValue < numValues; attributeValue++) {
                CNode childNode = node.getSuccessor(attributeValue);
                traverseAndCheckSplitValidity(instance, childNode);
            }
        }
    }

    /**
     * In addition to incrementing the counts for the main tree,
     * increment the counts of any alternative trees being grown from this VNode.
     */
    protected void traverseAndIncrementCounts(Instance instance, CNode node) {
        // increment the counts for this node
        // (unlike VFDT, statistics are kept for each data instance
        // at every node in the tree in order to continuously monitor
        // the validity of previous decisions)
        node.incrementCounts(instance, statMode);

        // traverse into all the alternative nodes
        for (CNode alt : node.getAlternativeTrees()) {
            traverseAndIncrementCounts(instance, alt);
        }

        // if tree node is not a leaf node,
        // descend into the appropriate child node
        CNode childNode = getChild(instance, node);
        if (childNode != null) {
            traverseAndIncrementCounts(instance, childNode);
        }
    }

    protected CNode getChild(Instance instance, CNode node) {
        CNode childNode = null;
        Attribute attribute = node.getAttribute();
        if (attribute != null) {
            double attributeValue = instance.value(attribute);

            // for numeric attribute split, it has two branchs,
            if (attribute.isNumeric()) {
                if (attributeValue <= node.getSplitValue()) {
                    childNode = node.getSuccessor(0);
                } else {
                    childNode = node.getSuccessor(1);
                }
            } else {
                childNode = node.getSuccessor((int) attributeValue);
            }
        }

        return childNode;
    }

    /**
     * Called when an instance rolls off the window. Removes the instance
     * from the counts of each node
     */
    protected void traverseAndDecrementCounts(Instance instance, CNode node, int id)
    {
        // nodes with greater id than the instance id were created after the
        // instance arrived and do not have the instance data included in their counts
        if (node.getId() <= id) {
            node.decrementCounts(instance, statMode);
        }

        // traverse into all the alternative nodes
        for (CNode alt : node.getAlternativeTrees()) {
            traverseAndDecrementCounts(instance, alt, id);
        }

        // if the main tree node is not a leaf node,
        // descend into the appropriate child node
        CNode childNode = getChild(instance, node);
        if (childNode != null) {
            traverseAndDecrementCounts(instance, childNode, id);
        }
    }

    /**
     * <p>Traverses the main tree and alternative trees. If in test mode, classifies
     * the instance and increments the testCorrectCount if the classification
     * is correct.</p>
     *
     * <p>If not in test mode, does nothing unless this is a leaf node and nMin
     * instances have been reached. At that point it checks for potential new
     * splits of the node.</p>
     *
     * @param instance
     */
    protected void traverseAndSplitOrTest(Instance instance, CNode node)
    {
        node.incrementTestCount(testInterval, testDuration, getProfiler(), minAltErrorDiff);

        // If we're in test mode, instead of considering splits, evaluate
        // the predicted class of this instance and compare it to the correct
        // classification then store whether or not it matches. Perform this
        // calculation for this node and each alternative node.
        if (node.isTestMode()) {
            node.testInstance(instance);
        }

        // traverse into all the alternative nodes
        for (CNode alt : node.getAlternativeTrees()) {
            traverseAndSplitOrTest(instance, alt);
        }

        // if tree node is not a leaf node,
        // descend into the appropriate child node
        CNode childNode = getChild(instance, node);
        if (childNode != null) {
            traverseAndSplitOrTest(instance, childNode);
        }
        // if we are not in test mode and the node is a leaf node and
        // the count is a multiple of nMin,
        // check to see whether we should split the node
        else if (!node.isTestMode() && node.getCount() % nMin == 0) {
            CVFDTProfiler profiler = getProfiler();
            profiler.startCheckNodeSplit();
            checkNodeSplit(instance, node);
            profiler.stopCheckNodeSplit();
        }
    }

    protected void updateWindow(Instance instance) {
        //XXX CVFDT Table 2 uses the largest ID *among the nodes that instance
        //XXX passes through* I think using the overall largest ID has the
        //XXX same affect, but I'm not 100% sure

        // add the new instance to the window
        // tag it with the id of the largest currently existing node
        window.addLast(new InstanceId(instance, largestNodeId));

        // drop the oldest instance from the window
        if (window.size() > windowSize) {
            InstanceId oldInstanceId = window.removeFirst();
            int oldId = oldInstanceId.getId();

            // iterate through the tree (and all alternative trees) and decrement
            // counts if the node's id is less than or equal to oldId
            traverseAndDecrementCounts(instance, getRoot(), oldId);
        }
    }

    @Override
    protected void splitNode(VNode node, Attribute attribute, Instance instance) {
        ((CNode) node).split(attribute, instance, ++largestNodeId, statMode);
    }

    /**
     * Evaluates the attributes of an already split node to determine if
     * a new alternative tree should be created.
     *
     */
    protected void recheckNodeSplit(Instance instance, CNode node) {
        CVFDTProfiler profiler = getProfiler();

        if (preprune(node)) {
            profiler.addPreprune();
            return;
        }

        // determine based on Hoeffding Bound whether to split node
        int firstIndex = -1;
        double firstValue = Double.MAX_VALUE;
        double secondValue = Double.MAX_VALUE;
        double splitValue = 0d;

        int[] sampledFeautres = node.getSampledFeatures();
        int sampledIndex = 0;

        // loop through all the attributes, calculating information gains
        // and keeping the attributes with the two highest information gains
        for (int attrIndex = 0; attrIndex < instance.numAttributes(); attrIndex++) {
            // compute split for feature that chosen by this node.
            if (numSampledFeatures > 0)  {
                if (sampledIndex >= numSampledFeatures) {
                    break;
                }
                if (sampledFeautres[sampledIndex] != attrIndex) {
                    continue;
                } else {
                    sampledIndex++;
                }
            }

            // don't consider the class attribute
            if (attrIndex == classAttribute.index()) continue;
            // don't consider the current split discrete attribute
            if (attrIndex == node.getAttribute().index() && !node.getAttribute().isNumeric()) continue;

            Attribute attribute = instance.attribute(attrIndex);
            double value = 0d;
            Split bestSplit = null;
            if (classAttribute.isNumeric()) {
                bestSplit = computeResidualSum(node, attribute);
            } else {
                bestSplit = computeEntropySum(node, attribute);
            }
            value = bestSplit.getScv();

            if (value < firstValue) {
                secondValue = firstValue;
                firstValue = value;
                firstIndex = attrIndex;
                splitValue = bestSplit.getSplitValue();
            } else if (value < secondValue) {
                secondValue = value;
            }
        }

        assert(sampledIndex == numSampledFeatures);

        // if the difference between the information gain of the two best attributes
        // has exceeded the Hoeffding bound (which will continually shrink as more
        // attributes are added to the node) then split on the best attribute
        double hoeffdingBound = calculateHoeffdingBound(node);

        boolean alreadyExists = node.doesAltNodeExist(firstIndex, splitValue);
        // split if there is a large enough entropy difference between the first/second place attributes
        // and the new csv must be better than the old one for node split recheck
        boolean confident = secondValue - firstValue > hoeffdingBound;
        if (confident) {
            double mainScv = computeResidualSum(node, node.getAttribute(), node.getSplitValue());
            confident = confident && mainScv > firstValue;
        }

        // or if the first/second attributes are so close that the hoeffding bound has decreased below
        // the tie threshold (in this case it really doesn't matter which attribute is chosen
        boolean tie = tieConfidence > hoeffdingBound && secondValue - firstValue >= tieConfidence / 2.0;

        if (!alreadyExists && (tie || confident)) {
            Attribute attribute = instance.attribute(firstIndex);
            node.addAlternativeNode(instance, attribute, ++largestNodeId, splitValue);

            profiler.addNumALTTotalSplit();
            if (tie) {
                profiler.addNumALTTieSplit();
            }
        }
    }

    @Override
    public String toString() {
        return info();
    }

    @Override
    public String info() {
        if (root == null) {
            return "CVFDT: No model built yet.";
        }
        return "CVFDT\n" + root.toString();
    }

    @Override
    public String debug() {
        if (root == null) {
            return "CVFDT: No model built yet.";
        }

        StringBuilder ret = new StringBuilder("CVFDT\n");
        ret.append(root.toString(true)).append("\n");
        ret.append("Tree size: ").append(root.getTreeSize()).append("\n");
        ret.append("ALT size: ").append(getALTSize()).append("\n");
        ret.append("Total feature size: ").append(attributes.size()).append("\n");
        ret.append("Sampling feature size: ");
        if (numSampledFeatures == 0) {
            ret.append(attributes.size());
        } else {
            ret.append(numSampledFeatures);
        }
        ret.append("\n");
        ret.append("Confident level: " + getConfidenceLevel()).append("\n");
        ret.append("Tie confidence: " + getTieConfidence()).append("\n");
        ret.append("Window size: " + window.size()).append("\n");
        ret.append("Split recheck interval: " + splitRecheckInterval).append("\n");
        ret.append("Test interval: " + testInterval).append("\n");
        ret.append("Test duration: " + testDuration).append("\n");
        ret.append("Min alt error diff: " + minAltErrorDiff).append("\n");

        ret.append(getProfiler().toString());
        ret.append("# of all instances is " + getAllInstances(getRoot())).append("\n");
        ret.append(AbstractProfiler.getMemoryUsage());

        generateRules(getRoot(), new ArrayList<CNode>());

        return ret.toString();
    }

    @Override
    public OnlineModel getScoringModel() {
        SDT tree = new SDT("CVFDT", root.getSDT(), attributes, classAttribute, treeType, numSampledFeatures);
        return tree;
    }

    @Override
    public OnlineModel getTrainingModel() {
        return this;
    }

    public int getALTSize() {
        return getRoot().getTotalSize() - getRoot().getTreeSize();
    }

    @Override
    public Map<String, Double> getProfilerResult() {
        Map<String, Double> pr = new HashMap<String, Double>();
        CVFDTProfiler profiler = getProfiler();

        pr.put("CVFDT.ScoringCount", (double) profiler.getScoringCount());
        pr.put("CVFDT.ScoringCost", profiler.getScoringCost());
        pr.put("CVFDT.TrainingCount", (double) profiler.getTrainingCount());
        pr.put("CVFDT.TrainingCost", profiler.getTrainingCost());
        pr.put("CVFDT.CheckNodeSplitCount", (double) profiler.getCheckNodeSplitCount());
        pr.put("CVFDT.CheckNodeSplitCost", profiler.getCheckNodeSplitCost());
        pr.put("CVFDT.ReheckNodeSplitCount", (double) profiler.getRecheckNodeSplitCount());
        pr.put("CVFDT.ReheckNodeSplitCost", profiler.getRecheckNodeSplitCost());
        pr.put("CVFDT.TestALTCount", (double) profiler.getTestALTCount());
        pr.put("CVFDT.TestALTCost", profiler.getTestALTCost());
        pr.put("CVFDT.Preprune", (double) profiler.getNumPreprune());

        pr.put("CVFDT.TreeSize", (double) profiler.getTreeSize());
        pr.put("CVFDT.ALTSize", (double) profiler.getALTSize());
        pr.put("CVFDT.ALTNodesPruningSize", (double) profiler.getNumALTNodesPruning());
        pr.put("CVFDT.ALTPruningSize", (double) profiler.getNumALTPruning());
        pr.put("CVFDT.ALTNodesActivateSize", (double) profiler.getNumALTNodesActivate());
        pr.put("CVFDT.ALTActivateSize", (double) profiler.getNumALTActivate());
        pr.put("CVFDT.TotalSplit", (double) profiler.getNumTotalSplit());
        pr.put("CVFDT.TieSplit", (double) profiler.getNumTieSplit());
        pr.put("CVFDT.ALTTotalSplit", (double) profiler.getNumALTTotalSplit());
        pr.put("CVFDT.ALTTieSplit", (double) profiler.getNumALTTieSplit());

        return pr;
    }

    protected long getAllInstances(CNode node) {
        int total = node.getCount();

        for (CNode alt : node.getAlternativeTrees()) {
            total += getAllInstances(alt);
        }

        if (node.getSuccessors() != null) {
            for (VNode child : node.getSuccessors()) {
                total += getAllInstances((CNode) child);
            }
        }

        return total;
    }

    public void generateRules(CNode node, String path) {

        if (node.getAttribute() == null) {
            System.out.println(path + " => [LEAF " + node.getId() + "]");
        } else {
            Attribute splitAttr = node.getAttribute();
            path = path + " => [Node " + node.getId() + ", " + splitAttr.name() + ":" + node.getSplitValue() + "]";

            for (CNode alt : node.getAlternativeTrees()) {
                generateRules(alt, path + " ALT = ");
            }

            if (node.getSuccessors() != null) {
                for (VNode child : node.getSuccessors()) {
                    generateRules((CNode) child, path);
                }
            }
        }
    }

    public void generateRules(CNode node, List<CNode> path) {
        if (node.getAttribute() == null) {
            //System.out.println(path + " => [LEAF " + node.getId() + "]");

        } else {
            Attribute splitAttr = node.getAttribute();
            //path = path + " => [Node " + node.getId() + ", " + splitAttr.name() + ":" + node.getSplitValue() + "]";

            path.add(node);
            for (CNode alt : node.getAlternativeTrees()) {
                generateRules(alt, path);
            }

            if (node.getSuccessors() != null) {
                for (VNode child : node.getSuccessors()) {
                    generateRules((CNode) child, path);
                }
            }
        }
    }
}

