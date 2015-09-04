package com.jd.si.ml.tree.node;


import com.jd.si.ml.core.Attribute;
import com.jd.si.ml.core.Instance;
import com.jd.si.ml.tree.CVFDT;
import com.jd.si.ml.tree.split.AttrStat;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


/**
 * Tree Node of VFDTProfiler
 */
public class VNode implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** The node's successors. */
    protected VNode[] successors;

    /** Attribute used for splitting. */
    protected Attribute attribute;
    /** Split value for a numeric attribute splitting. */
    protected double splitValue;

    /** Class value if node is leaf. */
    protected double classValue;
    
    /** Parent class value if node is leaf. */
    protected double parentClassValue;

    /** use new score if node is leaf and total count >= nScoreMin (10 percent of nMin) */
    protected int nScoreMin = 20;

    /** Number of instances corresponding to classValue for discrete feature. */
    protected int classCount;

    protected Attribute classAttribute;
    protected List<Attribute> attributes;

    protected AttrStat[] counts;
    protected int totalCount;

    protected int[] classCounts;

    protected double classSum;
    protected double classSquaredSum;

    protected int numSampledFeatures;
    protected int[] sampledFeatures;

    protected int numTotalFeatures;

    protected int height = 0;

    //protected double bestScv = 0d;

    public VNode(List<Attribute> attributes, Attribute classAttribute, int height) {
        this(attributes, classAttribute, 0, height);
    }

    public VNode(List<Attribute> attributes, Attribute classAttribute, int numSampledFeatures, int height) {
        this.classAttribute = classAttribute;
        this.attributes = attributes;
    	init(attributes, Double.NaN, numSampledFeatures, height);
    }
    
    public VNode(List<Attribute> attributes, Attribute classAttribute, double parentClassValue,
                 int numSampledFeatures, int height) {
        this.classAttribute = classAttribute;
        this.attributes = attributes;
    	init(attributes, parentClassValue, numSampledFeatures, height);
    }
    
    public VNode(Instance instance, Attribute classAttribute, double parentClassValue, int numSampledFeatures,
                 int height) {
        this(instance.getAttributes(), classAttribute, parentClassValue, numSampledFeatures, height);
    }
    
    public void init(List<Attribute> attributes, double superClassValue, int numSampledFeatures, int height) {
        this.numTotalFeatures = attributes.size();
        this.totalCount = 0;
        this.classCount = 0;
        this.classValue = 0;
        this.parentClassValue = superClassValue;
        this.height = height;
        
        if (classAttribute.isNumeric()) {
            this.classSum = 0d;
            this.classSquaredSum = 0d;
        } else {
            this.classCounts = new int[this.classAttribute.numValues()];
        }

        this.numSampledFeatures = numSampledFeatures;

        this.counts = new AttrStat[numTotalFeatures];

        if (numSampledFeatures > 0) {
            sampledFeatures = new int[numSampledFeatures];
            sample(sampledFeatures, numSampledFeatures);

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

    /**
     * Populate samples with m features by randomly selecting from training features.
     * @param samples
     * @param m
     */
    protected void sample(int[] samples, int m) {
        Random random = new Random();

        int total = numTotalFeatures;
        int[] candidates = new int[total];
        for (int i = 0; i < total; i++) {
            candidates[i] = i;
        }

        total--;
        int tmp = candidates[classAttribute.index()];
        candidates[classAttribute.index()] = candidates[total];
        candidates[total] = tmp;

        for (int i = 0; i < m && i < numTotalFeatures - 1; i++) {
            int selection = random.nextInt(total) + i;
            samples[i] = candidates[selection];
            candidates[selection] = candidates[i];
            candidates[i] = samples[i];
            total--;
        }

        Arrays.sort(samples);
    }

    public void copyNode(VNode node) {
        //throw new RuntimeException("Has not been implemented!");
        this.successors = node.successors;
        this.attribute = node.attribute;
        this.splitValue = node.splitValue;
        this.classValue = node.classValue;
        this.parentClassValue = node.parentClassValue;
        this.nScoreMin = node.nScoreMin;
        this.classCount = node.classCount;
        this.classAttribute = node.classAttribute;
        this.attributes = node.attributes;
        this.counts = node.counts;
        this.totalCount = node.totalCount;
        this.classCounts = node.classCounts;
        this.classSum = node.classSum;
        this.classSquaredSum = node.classSquaredSum;
        this.numSampledFeatures = node.numSampledFeatures;
        this.sampledFeatures = node.sampledFeatures;
        this.numTotalFeatures = node.numTotalFeatures;
        this.height = node.height;
    }

    public int[] getSampledFeatures() {
        return sampledFeatures;
    }

    public int getTreeSize()
    {
        if (successors != null)
        {
            int count = 0;
            for (VNode node : successors)
            {
                count += node.getTreeSize();
            }

            // add one for this node
            return count + 1;
        }
        else
        {
            return 1;
        }
    }

    public AttrStat getCount(Attribute attr) {
        return counts[attr.index()];
    }

    public VNode getSuccessor(int value)
    {
        if (successors != null)
        {
            return successors[value];
        }
        else
        {
            return null;
        }
    }

    /**
     * @see #getLeafNode(VNode, Instance)
     */
    public VNode getLeafNode(Instance instance)
    {
        return getLeafNode(this, instance);
    }

    protected VNode getLeafNode(VNode node, Instance instance)
    {
        // this is a leaf node, so return this node
        if (node.getAttribute() == null) {
            return node;
        } else if (node.getAttribute().isNumeric()) {
            double attributeValue = (double) instance.value(node.getAttribute());
            VNode childNode = null;
            if (attributeValue <= node.splitValue) {
                childNode = node.getSuccessor(0);
            } else {
                childNode = node.getSuccessor(1);
            }
            return getLeafNode(childNode, instance);
        } else {
            int attributeValue = (int) instance.value(node.getAttribute());
            VNode childNode = node.getSuccessor(attributeValue);
            return getLeafNode(childNode, instance);
        }
    }
    
    /**
     * update leaf node attribute status
     * 
     * @param attributes
     * @param numSampleFeatures
     */
    public void updateLeafNodeAttrStat(List<Attribute> attributes, int numSampleFeatures) {
    	// this is a leaf node, update leaf node
    	if (this.getAttribute() == null) {
    		// clean all status in this node
    		this.init(attributes, this.getScoreValue(), numSampledFeatures, height);
    	} else {
    		for (VNode successor : this.successors) {
    			successor.updateLeafNodeAttrStat(attributes, numSampleFeatures);
    		}
    	}
    }
    
    public Attribute getClassAttribute()
    {
        return classAttribute;
    }

    public Attribute getAttribute()
    {
        return attribute;
    }

    public double getClassValue()
    {
        return classValue;
    }

    public double getScoreValue() {
    	//return classValue;
    	
    	if (totalCount < nScoreMin && ! Double.isNaN(parentClassValue)) {
    		return parentClassValue;
    	} else {
    		return classValue;
    	}
    }

    public void split(Attribute attribute, Instance instance) {
        split(attribute, instance, false);
    }

    public void split(Attribute attribute, Instance instance, boolean lazyMode) {
        this.successors = new VNode[attribute.numValues()];
        this.attribute = attribute;

        for (int valueIndex = 0; valueIndex < attribute.numValues(); valueIndex++) {
            this.successors[valueIndex] = new VNode(instance, classAttribute, getScoreValue(),
                    numSampledFeatures, height + 1);
        }

        if (lazyMode) {
            counts = null;
        }
    }

    public int getNumClasses()
    {
        return this.classAttribute.numValues();
    }

    /**
     * @return the total number of instances in this VNode
     */
    public int getCount()
    {
        return totalCount;
    }

    /**
     * @param classIndex the class to get counts for
     * @return the total number of instances for the provided class
     */
    public int getCount(int classIndex)
    {
        return classCounts[classIndex];
    }

    /**
     * @param attribute the attribute to get a count for
     * @param attrValue the value of the attribute
     * @return the total number of instances with the provided attribute value
     */
    public int getCount(Attribute attribute, double attrValue)
    {
        AttrStat attrStat = counts[attribute.index()];

        return attrStat.getCount(attrValue);
    }

    public int getCount(Attribute attribute, double valueIndex, int classIndex)
    {
        return counts[attribute.index()].getCount(valueIndex, classIndex);
    }

    public int getHeight() {
        return height;
    }

    public void incrementCounts(Instance instance) {
        incrementCounts(instance, CVFDT.StatMode.NORM);
    }

    public void incrementCounts(Instance instance, CVFDT.StatMode statMode) {
        adjustCounts(instance, 1, statMode);
    }

    public void decrementCounts(Instance instance) {
        decrementCounts(instance, CVFDT.StatMode.NORM);
    }

    public void decrementCounts(Instance instance, CVFDT.StatMode statMode) {
        adjustCounts(instance, -1, statMode);
    }

    public void adjustCounts(Instance instance, int amount, CVFDT.StatMode statMode) {
        //XXX assumes nominal class
        double instanceClassValue = instance.classValue();

        if (statMode != CVFDT.StatMode.POPULATE) {
            adjustTotalCount(amount);
            adjustClassCount(instanceClassValue, amount);
        }

        // If in a lazy mode, we do not populate the instance to attribute statistics for non-leaf nodes.
        if (statMode != CVFDT.StatMode.LAZY || attribute == null) {
            if (numSampledFeatures > 0) {
                for (int i = 0; i < numSampledFeatures; i++) {
                    Attribute attribute = instance.attribute(sampledFeatures[i]);
                    adjustCount(attribute, instance, amount);
                }
            } else {
                for (int i = 0; i < instance.numAttributes(); i++) {
                    Attribute attribute = instance.attribute(i);
                    if (attribute != classAttribute) {
                        adjustCount(attribute, instance, amount);
                    }
                }
            }
        }

        if (statMode != CVFDT.StatMode.POPULATE) {
            if (classAttribute.isNumeric()) {
                if (totalCount > 0) {
                    classValue = classSum / totalCount;
                } else {
                    classValue = 0d;
                }
            } else {
                // update classValue and classCount
                int instanceClassCount = getCount((int) instanceClassValue);

                // if we incremented, and
                // if the count of the class we just added is greater than the current
                // largest count, it becomes the new classification for this node
                if (amount > 0 && instanceClassCount > classCount) {
                    classCount = instanceClassCount;
                    classValue = instance.value(classAttribute);
                }
                // if we decremented the current leading class, make sure it's
                // still the leading class
                else if (amount < 0 && instanceClassValue == classValue) {
                    int maxCount = 0;
                    int maxIndex = 0;
                    for (int i = 0; i < classCounts.length; i++) {
                        int count = classCounts[i];
                        if (count > maxCount) {
                            maxCount = count;
                            maxIndex = i;
                        }
                    }

                    classCount = maxCount;
                    //XXX assumes nominal class
                    classValue = maxIndex;
                }
            }
        }
    }

    protected void adjustTotalCount(int amount) {
        totalCount += amount;
    }
    
    protected void adjustClassCount(double classValue, int amount) {
        if (classAttribute.isNumeric()) {
            classSum += classValue * amount;
            classSquaredSum += Math.pow(classValue, 2) * amount;
        } else {
            classCounts[(int) classValue] += amount;
        }
    }

    protected void adjustCount(Attribute attribute, Instance instance, int amount) {
        int attributeIndex = attribute.index();
        AttrStat attrStat = counts[attributeIndex];
        double classValue = instance.value(classAttribute);
        double attrValue = instance.value(attribute);
        attrStat.adjustCount(attrValue, classValue, amount);
    }

    /**
     * Prints the decision tree using the private toString method from below.
     *
     * @return a textual description of the classifier
     */
    public String toString() {
        return toString(this, "", false);
    }

    public String toString(boolean verbose) {
        return toString(this, "", verbose);
    }

    protected String toString(VNode node, String indent, boolean verbose) {
        StringBuilder text = new StringBuilder();

        text.append(node.getText(verbose)).append("\n");
        if (node.getAttribute() != null) {
            for (int i = 0; i < node.successors.length; i++) {
                text.append(indent).append("|---");
                text.append(node.getText(i, verbose));
                VNode child = node.successors[i];

                if (i == node.successors.length - 1) {
                    text.append(toString(child, indent + "    ", verbose));
                } else {
                    text.append(toString(child, indent + "|   ", verbose));
                }
            }
        }

        return text.toString();
    }

    protected String getText(boolean verbose) {
        StringBuilder text = new StringBuilder();

        if (attribute != null) {
            text.append("[NODE: ");
        } else {
            text.append("[LEAF: ");
        }

        if (classAttribute.isNumeric()) {
            text.append(String.format("%.2f", getScoreValue()));
        } else {
            text.append(classAttribute.value((int) getScoreValue()));
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

            if (sampledFeatures != null) {
                text.append(", features: <");
                for (int idx : sampledFeatures) {
                    text.append(idx + "-" + attributes.get(idx).name()).append(", ");
                }
                text.append(">");
            } else {
                //text.append(", features: <");
                //text.append("all");
                //text.append(">");
            }

        }

        text.append("]");

        return text.toString();
    }

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

    public double getClassSum() {
        return classSum;
    }

    public double getClassSquaredSum() {
        return classSquaredSum;
    }

    public int[] getClassCounts() {
        return classCounts;
    }

    public double getSplitValue() {
        return splitValue;
    }

    public void setSplitValue(double splitValue) {
        this.splitValue = splitValue;
    }

    public Node getSDT() {
        Node[] successors = null;
        if (this.successors != null) {
            successors = new Node[this.successors.length];
            for (int i = 0; i < successors.length; i++) {
                successors[i] = this.successors[i].getSDT();
            }
        }

        return new Node(successors, attribute, classAttribute, attributes, splitValue, classValue, totalCount,
                sampledFeatures, getScoreValue());
    }

    public AttrStat[] getCounts() {
        return counts;
    }

    public VNode[] getSuccessors() {
        return successors;
    }
}
