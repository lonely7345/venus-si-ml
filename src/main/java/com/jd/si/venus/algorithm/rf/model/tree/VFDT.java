package com.jd.si.venus.algorithm.rf.model.tree;



import com.jd.si.venus.algorithm.rf.model.OnlineModel;
import com.jd.si.venus.algorithm.rf.model.core.Attribute;
import com.jd.si.venus.algorithm.rf.model.core.Instance;
import com.jd.si.venus.algorithm.rf.model.tree.profiler.AbstractProfiler;
import com.jd.si.venus.algorithm.rf.model.tree.split.*;
import com.jd.si.venus.algorithm.rf.model.tree.node.VNode;
import com.jd.si.venus.algorithm.rf.model.tree.profiler.VFDTProfiler;
import com.jd.si.venus.algorithm.rf.model.util.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;


/**
 * A Very Fast Decision Tree model.
 */
public class VFDT extends OnlineModel implements Serializable
{
    private static final Logger logger = Logger.getLogger(VFDT.class.getName());

    private static final long serialVersionUID = 1L;

    protected VNode root;

    protected int numClasses;
    protected double targetRange = 1d;

    // if the hoeffding bound drops below tie confidence, assume the best two attributes
    // are very similar (and thus might require an extremely large number of instances
    // to separate with high confidence), so just choose the current best
    protected double tieConfidence = 0.01;
    // 1-delta is the probability of choosing the correct attribute at any given node
    protected double delta = 1e-8;
    // nodes are only rechecked for potential splits every nmin data instances
    protected int nMin = 200;

    protected double R_squared; // log2(numClasses)^2
    protected double ln_inv_delta; // ln(1 / delta)

    enum TreeType { DECISION_TREE, REGRESSION_TREE }
    protected TreeType treeType; // A decision tree, otherwise a regression tree.
    protected int numSampledFeatures;

    protected AttrImprStat attrImprStat = new AttrImprStat();

    //protected AbstractProfiler profiler = new AbstractProfiler();
    protected AbstractProfiler treeProfiler;

    protected boolean verbose = false;

    // max tree height
    protected int maxTreeHeight = 15;

    // min rate of instance within one node
    protected double minInstRate = 0.001d;

    protected VFDT (int numCols, String[] colNames, boolean[] isCont, String[][] distVals, int targetIdx) {
        attributes = new ArrayList<Attribute>();
        for (int i = 0; i < numCols; i++) {
            attributes.add(new Attribute(i, colNames[i], isCont[i], distVals[i], targetIdx));
        }

        classAttribute = attributes.get(targetIdx);
        numSampledFeatures = 0;

        ln_inv_delta = Math.log(1 / delta);

        if (attributes.size() - 1 < numSampledFeatures) {
            throw new RuntimeException("Given number of sampled features is greater than the training attribute " +
                    "number " + (attributes.size() - 1));
        }

        treeProfiler = initProfiler();
    }

    /**
     * Classification Tree
     *
     * @param numCols
     * @param colNames
     * @param isCont
     * @param distVals
     * @param targetIdx
     * @param numClasses
     */
    public VFDT (int numCols, String[] colNames, boolean[] isCont, String[][] distVals, int targetIdx, int numClasses) {
        this(numCols, colNames, isCont, distVals, targetIdx, numClasses, 0);
    }

    /**
     * Classification Tree

     * @param numCols
     * @param colNames
     * @param isCont
     * @param distVals
     * @param targetIdx
     * @param numClasses
     * @param numSampledFeatures
     */
    public VFDT (int numCols, String[] colNames, boolean[] isCont, String[][] distVals, int targetIdx, int numClasses,
                 int numSampledFeatures) {
        this(numCols, colNames, isCont, distVals, targetIdx);

        this.treeType = TreeType.DECISION_TREE;
        this.R_squared = Math.pow(Utils.log2(numClasses), 2);
        this.numClasses = numClasses;
        this.numSampledFeatures = numSampledFeatures;

        init();
    }

    /**
     * Regression Tree
     *
     * @param numCols
     * @param colNames
     * @param isCont
     * @param distVals
     * @param targetIdx
     */
    public VFDT (int numCols, String[] colNames, boolean[] isCont, String[][] distVals, int targetIdx,
                 double targetRange) {
        this(numCols, colNames, isCont, distVals, targetIdx, targetRange, 0);
    }

    /**
     * Regression Tree

     * @param numCols
     * @param colNames
     * @param isCont
     * @param distVals
     * @param targetIdx
     * @param targetRange
     * @param numSampledFeatures
     */
    public VFDT (int numCols, String[] colNames, boolean[] isCont, String[][] distVals, int targetIdx,
                 double targetRange, int numSampledFeatures) {
        this(numCols, colNames, isCont, distVals, targetIdx);

        this.treeType = TreeType.REGRESSION_TREE;
        this.R_squared = Math.pow(targetRange, 2);
        this.targetRange = targetRange;
        this.numSampledFeatures = numSampledFeatures;

        init();
    }

    protected void init() {
        // create root node
        root = new VNode(attributes, classAttribute, numSampledFeatures, 0);
    }

    public int getMaxTreeHeight() {
        return maxTreeHeight;
    }

    public double getMinInstRate() {
        return minInstRate;
    }

    public void setMaxTreeHeight(int maxTreeHeight) {
        this.maxTreeHeight = maxTreeHeight;
    }

    public void setMinInstRate(double minInstRate) {
        this.minInstRate = minInstRate;
    }

    protected AbstractProfiler initProfiler() {
        return new VFDTProfiler(this);
    }

    protected VFDTProfiler getProfiler() {
        return (VFDTProfiler) treeProfiler;
    }

    /**
     * Dynamic add a new Attribute
     *
     * @param colName
     * @param isCont
     * @param distVals
     * @param targetRange
     * @param numSampledFeatures
     */
    public void addAttribute (String colName, boolean isCont, String[] distVals,
                              double targetRange, int numSampledFeatures, String distDefault, double numDefault) {

        // update numSampledFeatures due to a new added attribute
        this.numSampledFeatures = numSampledFeatures;
        Attribute newAttribute;

        if (isCont) {
            newAttribute = new Attribute(attributes.size(), colName, isCont, distVals, classAttribute.index(), numDefault);
        } else {
            newAttribute = new Attribute(attributes.size(), colName, isCont, distVals, classAttribute.index(), distDefault);
        }

        this.attributes.add(newAttribute);

        root.updateLeafNodeAttrStat(attributes, numSampledFeatures);
    }

    /**
     * Nodes are only checked for splits when the reach multiple of nMin instances.
     */
    public int getNMin()
    {
        return nMin;
    }

    /**
     * @see #getNMin()
     */
    public void setNMin(int nmin)
    {
        this.nMin = nmin;
    }

    public AttrImprStat getAttrImprStat() {
        return attrImprStat;
    }

    /**
     * See equation (1) in "Mining High-Speed Data Streams." The Hoeffding Bound provides
     * a bound on the true mean of a random variable given n independent
     * observations of the random variable, with probability 1 - delta
     * (where delta is the confidence level returned by this method).
     *
     * @return the Hoeffding Bound confidence level
     */
    public double getConfidenceLevel()
    {
        return delta;
    }

    /**
     * @see #getConfidenceLevel()
     * @param delta
     */
    public void setConfidenceLevel(double delta)
    {
        this.delta = delta;
        this.ln_inv_delta = Math.log(1 / this.delta);
    }

    public void setTargetRange(double range) {
        this.R_squared = Math.pow(range, 2.0);
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * If two attributes have very similar information gain, then
     * it may take many instances to choose between them with
     * high confidence. Tie confidence sets an alternative threshold
     * which causes a split decision to be automatically made if the
     * Hoeffding bound drops below the tie confidence.
     *
     * @return
     */
    public double getTieConfidence()
    {
        return this.tieConfidence;
    }

    public int getNumSampledFeatures() {
        return numSampledFeatures;
    }

    /**
     * #see {@link #getConfidenceLevel()}
     * @param tieConfidence
     */
    public void setTieConfidence(double tieConfidence)
    {
        this.tieConfidence = tieConfidence;
    }

    public VNode getRoot()
    {
        return root;
    }

    protected double score(Instance instance) {
        VFDTProfiler profiler = getProfiler();
        profiler.startScoring();

        if (instance.hasMissingValue()) {
            throw new RuntimeException("VFDT: missing values not supported.");
        }

        // get the class value for the leaf node corresponding to the provided instance
        double scoreValue = root.getLeafNode(instance).getScoreValue();

        profiler.stopScoring();

        return scoreValue;
    }

    /**
     * For regression tree, return continuous value;
     * For decision tree, return discrete value.
     * @param values
     * @return
     */
    public Object score(Object[] values) {
        if (treeType == TreeType.DECISION_TREE) {
            return classify(values);
        } else {
            return predict(values);
        }
    }

    /**
     * For regression tree, return scoring target value;
     * @param values
     * @return
     */
    public double predict(Object[] values) {
        return score(new Instance(attributes, values, classAttribute));
    }

    /**
     * For decision tree, return discr
     * @param values
     * @return
     */
    public String classify(Object[] values) {
        return classAttribute.value((int) score(new Instance(attributes, values, classAttribute)));
    }

    public Attribute getClassAttribute() {
        return classAttribute;
    }

    public void addInstance(Object[] values) {
        addInstance(new Instance(attributes, values, classAttribute));
    }

    public void addInstance(String[] values) {
        Object[] oValues = new Object[attributes.size()];
        for (int i = 0; i < attributes.size(); i++) {
            if (attributes.get(i).isNumeric()) {
                oValues[i] = Double.parseDouble(values[i]);
            } else {
                oValues[i] = values[i];
            }
        }

        addInstance(new Instance(attributes, oValues, classAttribute));
    }

    public void addInstance(Instance instance)
    {
        VFDTProfiler profiler = getProfiler();
        profiler.startTraining();

        try {
            // traverse the classification tree to find the leaf node for this instance
            VNode node = root.getLeafNode(instance);

            // update the counts associated with this instance
            node.incrementCounts(instance);

            // check whether or not to split the node on an attribute
            if (node.getCount() % nMin == 0) {
                profiler.startCheckNodeSplit();
                checkNodeSplit(instance, node);
                profiler.stopCheckNodeSplit();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        profiler.stopTraining();
    }

    protected boolean preprune(VNode node) {
        double instRate = (double) node.getCount() / (double) getRoot().getCount();
        if (node.getCount() == 0 ||
                (minInstRate > 0 && instRate < minInstRate) ||
                (maxTreeHeight > 0 && node.getHeight() >= maxTreeHeight)) {
            return true;
        }

        double nullValue = 0d;
        if (classAttribute.isNumeric()) {
            nullValue = computeResidual(node);
        } else {
            nullValue = computeEntropy(node);
        }
        if (nullValue == 0d) {
            return true;
        }

        return false;
    }

    protected void checkNodeSplit(Instance instance, VNode node) {
        VFDTProfiler profiler = getProfiler();

        if (preprune(node)) {
            profiler.addPreprune();
            return;
        }

        // In the worst case, all the records belong to one leaf are the same class or same target value.
        // For batch training, this is one of the stop criterion.
        // For streaming training, we don't split this node.
        double nullValue = 0d;
        if (classAttribute.isNumeric()) {
            // for numeric class, its null value is residual rate.
            nullValue = 1d;
        } else {
            nullValue = computeEntropy(node);
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

            Attribute attribute = instance.attribute(attrIndex);
            double value = 0d;
            Split bestSplit = null;
            if (treeType == TreeType.DECISION_TREE) {
                bestSplit = computeEntropySum(node, attribute);
            } else {
                bestSplit = computeResidualSum(node, attribute);
            }
            value = bestSplit.getScv();

            if (value < firstValue)
            {
                secondValue = firstValue;
                firstValue = value;
                firstIndex = attrIndex;
                splitValue = bestSplit.getSplitValue();
            }
            else if (value < secondValue)
            {
                secondValue = value;
            }
        }

        //node.setSplitValue(splitValue);

        // if the difference between the information gain of the two best attributes
        // has exceeded the Hoeffding bound (which will continually shrink as more
        // attributes are added to the node) then split on the best attribute
        double hoeffdingBound = calculateHoeffdingBound(node);
        if (node.getCount() < 300) {
            //System.out.println();
        }

        // split if there is a large enough entropy difference between the first/second place attributes
        boolean confident = secondValue - firstValue > hoeffdingBound;
        // or if the first/second attributes are so close that the hoeffding bound has decreased below
        // the tie threshold (in this case it really doesn't matter which attribute is chosen
        boolean tie = tieConfidence > hoeffdingBound;

        // don't split if even the best split would increase overall entropy
        boolean preprune = nullValue <= firstValue;

        // see: vfdt-engine.c:871
        if ((tie || confident) && !preprune) {
            Attribute attribute = instance.attribute(firstIndex);

            node.setSplitValue(splitValue);
            //node.setBestScv(firstValue);
            splitNode(node, attribute, instance);

            // update attribute importance
            attrImprStat.updateAttrImpr(attribute, nullValue - firstValue);

            profiler.addTotalSplit();
            if (tie) {
                profiler.addTieSplit();
            }
        }
    }

    protected void splitNode(VNode node, Attribute attribute, Instance instance)
    {
        node.split(attribute, instance);
    }

    /*
    protected double computeInfoGain(VNode node, Attribute attr)
    {
        return computeEntropy(node) - computeEntropySum(node, attr);
    }
    */

    protected Split computeEntropySum(VNode node, Attribute attr) {
        double sum = 0.0;
        double bestSplit = 0d;

        if (classAttribute.isNumeric()) {
            throw new RuntimeException("It is not supported to compute entropy sum for numeric class.");
        }

        if (attr.isNumeric()) {
            int[] leClassCounts = new int[numClasses];
            for (int i = 0 ; i < numClasses; i++) {
                leClassCounts[i] = 0;
            }
            double minEntropy = Double.MAX_VALUE;
            NumAttrStat attrStat = (NumAttrStat) node.getCount(attr);
            TreeMap<Double, ClassStat> counts = attrStat.getCounts();
            int[] totalClassCounts = node.getClassCounts();

            for (Map.Entry<Double, ClassStat> entry : counts.entrySet()) {
                double splitValue = entry.getKey();
                int[] splitClassCounts = entry.getValue().getCounts();

                int leTotalCount = 0;
                for (int i = 0; i < leClassCounts.length; i++) {
                    leClassCounts[i] += splitClassCounts[i];
                    leTotalCount += leClassCounts[i];
                }
                int gtTotalCount = node.getCount() - leTotalCount;

                double leEntropy = computeEntropy(leClassCounts, leTotalCount);
                double leRatio = (double) leTotalCount / (double) node.getCount();

                int[] gtClassCounts = new int[numClasses];
                for (int i = 0; i < numClasses; i++) {
                    gtClassCounts[i] = totalClassCounts[i] - leClassCounts[i];
                }
                double gtEntropy = computeEntropy(gtClassCounts, gtTotalCount);
                double gtRatio = (double) gtTotalCount / (double) node.getCount();

                double entropy = leEntropy * leRatio + gtEntropy * gtRatio;
                if (entropy < minEntropy) {
                    minEntropy = entropy;
                    bestSplit = splitValue;
                }
            }

            sum = minEntropy;
        } else {
            for (int valueIndex = 0; valueIndex < attr.numValues(); valueIndex++) {
                int count = node.getCount(attr, valueIndex);

                if (count > 0) {
                    double entropy = computeEntropy(node, attr, valueIndex);
                    double ratio = ((double) count / (double) node.getCount());
                    sum += ratio * entropy;
                }
            }
        }

        return new Split(attr, sum, bestSplit);
    }

    protected double computeEntropy(VNode node) {
        double entropy = 0;
        double totalCount = (double) node.getCount();
        for (int classIndex = 0; classIndex < numClasses; classIndex++)
        {
            int count = node.getCount(classIndex);

            if (count > 0)
            {
                double p = count / totalCount;
                entropy -= p * Utils.log2(p);
            }
        }

        return entropy;
    }

    /**
     * Computes the entropy of the child node created by splitting on the
     * provided attribute and value.
     *
     * @param node the tree node for which entropy is to be computed
     * @param attribute the attribute to split on before calculating entropy
     * @param valueIndex calculate entropy for the child node corresponding
     *        to this nominal attribute value index
     * @return calculated entropy
     * @throws Exception if computation fails
     */
    protected double computeEntropy(VNode node, Attribute attribute, int valueIndex) {
        double entropy = 0;
        double totalCount = (double) node.getCount(attribute, valueIndex);
        for (int classIndex = 0; classIndex < numClasses; classIndex++)
        {
            int count = node.getCount(attribute, valueIndex, classIndex);

            if (count > 0)
            {
                double p = count / totalCount;
                entropy -= p * Utils.log2(p);
            }
        }

        return entropy;
    }

    /**
     * @param classCounts
     * @return
     */
    protected double computeEntropy(int[] classCounts, int totalCount) {
        double entropy = 0d;

        for (int i = 0; i < classCounts.length; i++) {
            int count = classCounts[i];

            if (count > 0) {
                double p = (double) count / (double) totalCount;
                entropy -= p * Utils.log2(p);
            }
        }

        return entropy;
    }

    protected double computeResidualSum(VNode node, Attribute attribute, double splitValue) {
        double residualSum = Double.MAX_VALUE;

        if (!classAttribute.isNumeric()) {
            throw new RuntimeException("It is not supported to compute residual sum for discrete class.");
        }

        if (attribute.isNumeric()) {
            NumAttrStat attrStat = (NumAttrStat) node.getCount(attribute);
            TreeMap<Double, ClassStat> counts = attrStat.getCounts();

            double totalSum = node.getClassSum();
            double totalSquaredSum = node.getClassSquaredSum();
            int totalCount = node.getCount();

            double leSum = 0d;
            double leSquaredSum = 0d;
            int leCount = 0;
            double gtSum = totalSum - leSum;
            double gtSquaredSum = totalSquaredSum - leSquaredSum;
            int gtCount = totalCount - leCount;

            for (Map.Entry<Double, ClassStat> entry : counts.entrySet()) {
                if (splitValue < entry.getKey()) {
                    break;
                }

                ClassStat classStat = entry.getValue();
                leSum += classStat.getSum();
                leSquaredSum += classStat.getSquaredSum();
                leCount += classStat.getCount();

                gtSum = totalSum - leSum;
                gtSquaredSum = totalSquaredSum - leSquaredSum;
                gtCount = totalCount - leCount;
            }

            if (leCount > 0 && gtCount > 0) {
                residualSum = computeResidual(leSum, leSquaredSum, leCount) / totalCount +
                        computeResidual(gtSum, gtSquaredSum, gtCount) / totalCount;
            }
        } else {
            DistAttrStat attrStat = (DistAttrStat) node.getCount(attribute);
            ClassStat[] classStats = attrStat.getCounts();
            for (int valueIndex = 0; valueIndex < attribute.numValues(); valueIndex++) {
                ClassStat classStat = classStats[valueIndex];
                residualSum += computeResidual(classStat.getSum(), classStat.getSquaredSum(), classStat.getCount());
            }
        }

        return residualSum / computeResidual(node);
    }

    protected Split computeResidualSum(VNode node, Attribute attribute) {
        double residualSum = 0d;
        double bestSplit = 0d;
        int totalCount = node.getCount();

        if (!classAttribute.isNumeric()) {
            throw new RuntimeException("It is not supported to compute residual sum for discrete class.");
        }

        if (attribute.isNumeric()) {
            NumAttrStat attrStat = (NumAttrStat) node.getCount(attribute);
            TreeMap<Double, ClassStat> counts = attrStat.getCounts();

            //double totalSum = attrStat.getSum();
            double totalSum = node.getClassSum();
            double totalSquaredSum = node.getClassSquaredSum();

            double leSum = 0d;
            double leSquaredSum = 0d;
            int leCount = 0;

            double minResidualSum = Double.MAX_VALUE;

            for (Map.Entry<Double, ClassStat> entry : counts.entrySet()) {
                double splitValue = entry.getKey();
                ClassStat classStat = entry.getValue();

                leSum += classStat.getSum();
                leSquaredSum += classStat.getSquaredSum();
                leCount += classStat.getCount();

                if (node.getAttribute() == attribute && splitValue == node.getSplitValue()) {
                    continue;
                }

                double gtSum = totalSum - leSum;
                double gtSquaredSum = totalSquaredSum - leSquaredSum;
                int gtCount = totalCount - leCount;

                if (gtCount > 0) {
                    double tmpResidualSum = computeResidual(leSum, leSquaredSum, leCount) / totalCount +
                            computeResidual(gtSum, gtSquaredSum, gtCount) / totalCount;
                    if (tmpResidualSum <= minResidualSum) {
                        minResidualSum = tmpResidualSum;
                        bestSplit = splitValue;
                    }
                }
            }

            residualSum = minResidualSum;
        } else {
            DistAttrStat attrStat = (DistAttrStat) node.getCount(attribute);
            ClassStat[] classStats = attrStat.getCounts();
            for (int valueIndex = 0; valueIndex < attribute.numValues(); valueIndex++) {
                ClassStat classStat = classStats[valueIndex];
                residualSum += computeResidual(classStat.getSum(), classStat.getSquaredSum(), classStat.getCount());
            }
        }

        residualSum = residualSum / computeResidual(node);

        return new Split(attribute, residualSum, bestSplit);
    }

    /**
     *
     * @param sum
     * @param squaredSum
     * @param count
     * @return
     */
    protected double computeResidual(double sum, double squaredSum, int count) {
        double residual = squaredSum - Math.pow(sum, 2) / (double) count;
        return residual;
    }

    protected double computeResidual(VNode node) {
        return node.getClassSquaredSum() / (double) node.getCount() - Math.pow(node.getClassSum() / (double) node.getCount(), 2);
    }

    /**
     * Calculates the difference in information gain, epsilon, between the
     * attribute with the best and second best information gain necessary to
     * make a splitting decision based on the current set of observed attributes.
     * As more attributes are gathered, the required difference will decrease.
     *
     * @param node
     * @return
     */
    // see: vfdt-engine.c:833
    protected double calculateHoeffdingBound(VNode node) {
        int n = node.getCount();
        double epsilon = Math.sqrt((R_squared * ln_inv_delta) / (2 * n));
        return epsilon;
    }

    /**
     * Prints the decision tree using the private toString method from below.
     *
     * @return a textual description of the classifier
     */
    @Override
    public String toString() {
        return info();
    }

    @Override
    public String info() {
        if (root == null) {
            return "VFDT: No model built yet.";
        }
        return "VFDT\n" + root.toString();
    }

    @Override
    public String debug() {
        if (root == null) {
            return "VFDT: No model built yet.";
        }

        StringBuilder ret = new StringBuilder("VFDT\n");
        ret.append(root.toString(true)).append("\n");
        ret.append("Tree size: ").append(root.getTreeSize()).append("\n");
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

        ret.append(getProfiler().toString());
        ret.append(AbstractProfiler.getMemoryUsage());

        return ret.toString();
    }

    @Override
    public OnlineModel getScoringModel() {
        SDT tree = new SDT("VFDT", root.getSDT(), attributes, classAttribute, treeType, numSampledFeatures);
        return tree;
    }

    @Override
    public OnlineModel getTrainingModel() {
        return this;
    }

    @Override
    public Map<String, Double> getProfilerResult() {
        Map<String, Double> pr = new HashMap<String, Double>();
        VFDTProfiler profiler = getProfiler();

        pr.put("VFDT.ScoringCount", (double) profiler.getScoringCount());
        pr.put("VFDT.ScoringCost", profiler.getScoringCost());
        pr.put("VFDT.TrainingCount", (double) profiler.getTrainingCount());
        pr.put("VFDT.TrainingCost", profiler.getTrainingCost());
        pr.put("VFDT.CheckNodeSplitCount", (double) profiler.getCheckNodeSplitCount());
        pr.put("VFDT.CheckNodeSplitCost", profiler.getCheckNodeSplitCost());
        pr.put("VFDT.Preprune", (double) profiler.getNumPreprune());

        pr.put("VFDT.TreeSize", (double) profiler.getTreeSize());
        pr.put("VFDT.TotalSplit", (double) profiler.getNumTotalSplit());
        pr.put("VFDT.TieSplit", (double) profiler.getNumTieSplit());

        return pr;
    }
}
