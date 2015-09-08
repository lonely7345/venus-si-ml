package com.jd.si.venus.algorithm.rf.model.tree.node;



import com.jd.si.venus.algorithm.rf.model.core.Attribute;
import com.jd.si.venus.algorithm.rf.model.core.Instance;

import java.io.Serializable;
import java.util.List;

/**
 * A simple tree node.
 */
public class Node implements Serializable {
    private static final long serialVersionUID = 1L;

    /** The node's successors. */
    protected Node[] successors;

    /** Attribute used for splitting. */
    protected Attribute attribute;

    /** Class attribute */
    protected Attribute classAttribute;
    /** All attributes. */
    protected List<Attribute> attributes;

    /** Split value for a numeric attribute splitting. */
    protected double splitValue;

    /** Class value if node is leaf. */
    protected double classValue;

    protected double scoreValue;

    /** Number of instances that belongs to this node */
    protected int totalCount;

    /** */
    protected int[] sampledFeatures;

    public Node(Node[] successors, Attribute attribute, Attribute classAttribute, List<Attribute> attributes,
                double splitValue, double classValue, int totalCount, int[] sampledFeatures, double scoreValue) {
        this.successors = successors;
        this.attribute = attribute;
        this.classAttribute = classAttribute;
        this.attributes = attributes;
        this.splitValue = splitValue;
        this.classValue = classValue;
        this.totalCount = totalCount;
        this.sampledFeatures = sampledFeatures;
        this.scoreValue = scoreValue;
    }

    public int getTreeSize() {
        if (successors != null) {
            int count = 0;
            for (Node node : successors)
            {
                count += node.getTreeSize();
            }

            // add one for this node
            return count + 1;
        } else {
            return 1;
        }
    }

    public Node getSuccessor(int value)
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
     * @see #getLeafNode(Node, com.jd.si.venus.algorithm.rf.model.core.Instance)
     */
    public Node getLeafNode(Instance instance)
    {
        return getLeafNode(this, instance);
    }

    protected Node getLeafNode(Node node, Instance instance)
    {
        // this is a leaf node, so return this node
        if (node.getAttribute() == null) {
            return node;
        } else if (node.getAttribute().isNumeric()) {
            double attributeValue = (double) instance.value(node.getAttribute());
            Node childNode = null;
            if (attributeValue <= node.splitValue) {
                childNode = node.getSuccessor(0);
            } else {
                childNode = node.getSuccessor(1);
            }
            return getLeafNode(childNode, instance);
        } else {
            int attributeValue = (int) instance.value(node.getAttribute());
            Node childNode = node.getSuccessor(attributeValue);
            return getLeafNode(childNode, instance);
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

    /**
     * @return the total number of instances in this Node
     */
    public int getCount()
    {
        return totalCount;
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

    protected String toString(Node node, String indent, boolean verbose) {
        StringBuilder text = new StringBuilder();

        text.append(node.getText(verbose)).append("\n");
        if (node.getAttribute() != null) {
            for (int i = 0; i < node.successors.length; i++) {
                text.append(indent).append("|---");
                text.append(node.getText(i, verbose));
                Node child = node.successors[i];

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

    public double getScoreValue() {
        return scoreValue;
    }
}
