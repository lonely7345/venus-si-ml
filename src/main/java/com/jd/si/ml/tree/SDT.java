package com.jd.si.ml.tree;


import com.jd.si.ml.OnlineModel;
import com.jd.si.ml.core.Attribute;
import com.jd.si.ml.core.Instance;
import com.jd.si.ml.tree.node.Node;
import com.jd.si.ml.tree.profiler.SDTProfiler;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A Simple Decision Tree for scoring only.
 */
public class SDT extends OnlineModel implements Serializable {
    private static final Logger logger = Logger.getLogger(SDT.class.getName());

    private static final long serialVersionUID = 1L;

    private String model;

    /** Root node of classification tree. */
    protected Node root;

    protected VFDT.TreeType treeType; // A decision tree, otherwise a regression tree.

    protected int numSampledFeatures;
    protected SDTProfiler profiler = new SDTProfiler();

    public SDT(String model, Node root, List<Attribute> attributes, Attribute classAttribute, VFDT.TreeType treeType,
               int numSampledFeatures) {
        this.model = model;
        this.root = root;
        this.attributes = attributes;
        this.classAttribute = classAttribute;
        this.treeType = treeType;
        this.numSampledFeatures = numSampledFeatures;
    }

    /**
     * For regression tree, return continuous value;
     * For decision tree, return discrete value.
     * @param instance
     * @return
     */
    @Override
    public Object score(Object[] instance) {
        if (treeType == VFDT.TreeType.DECISION_TREE) {
            return classify(instance);
        } else {
            return predict(instance);
        }
    }

    @Override
    public void addInstance(Object[] instance) {
        throw new RuntimeException("A Simple Decision Tree is a compact tree for scoring only.");
    }

    @Override
    public OnlineModel getTrainingModel() {
        throw new RuntimeException("A Simple Decision Tree can't generate a training model.");
    }

    @Override
    public OnlineModel getScoringModel() {
        return this;
    }

    protected double score(Instance instance) {
        profiler.startScoring();

        if (instance.hasMissingValue()) {
            throw new RuntimeException(model + ": missing values not supported.");
        }

        // get the class value for the leaf node corresponding to the provided instance
        double scoreValue = root.getLeafNode(instance).getScoreValue();

        profiler.stopScoring();

        return scoreValue;
    }

    /**
     * For regression tree, return scoring target value.
     * @param values
     * @return
     */
    public double predict(Object[] values) {
        return score(new Instance(attributes, values, classAttribute));
    }

    /**
     * For decision tree, return discrete value.
     * @param values
     * @return
     */
    public String classify(Object[] values) {
        return classAttribute.value((int) score(new Instance(attributes, values, classAttribute)));
    }

    @Override
    public String toString() {
        return info();
    }

    @Override
    public String info() {
        if (root == null) {
            return model + ": No model built yet.";
        }
        return model + "\n" + root.toString();
    }

    @Override
    public String debug() {
        if (root == null) {
            return "VFDTProfiler: No model built yet.";
        }

        StringBuilder ret = new StringBuilder(model + "\n");
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

        ret.append(profiler.toString());

        return ret.toString();
    }

    @Override
    public Map<String, Double> getProfilerResult() {
        Map<String, Double> pr = new HashMap<String, Double>();

        pr.put(model + ".TreeSize", (double) root.getTreeSize());
        pr.put(model + ".ScoringCount", (double) profiler.getScoringCount());
        pr.put(model + ".ScoringCost", (double) profiler.getScoringCost());

        return pr;
    }
}
