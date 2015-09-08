package com.jd.si.venus.algorithm.rf.model;


import com.jd.si.venus.algorithm.rf.model.core.Attribute;

import java.util.List;
import java.util.Map;

/**
 * An Online Model
 */
public abstract class OnlineModel implements IModel {
	private static final long serialVersionUID = -7762112720648622188L;
	public List<Attribute> attributes;
    public Attribute classAttribute;

    public abstract Object score(Object[] instance);
    public abstract void addInstance(Object[] instance);
    public abstract OnlineModel getTrainingModel();
    public abstract OnlineModel getScoringModel();
    public abstract String debug();
    public abstract String info();
    public abstract Map<String, Double> getProfilerResult();
}
