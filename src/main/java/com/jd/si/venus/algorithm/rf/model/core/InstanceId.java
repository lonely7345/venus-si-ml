package com.jd.si.venus.algorithm.rf.model.core;

import java.io.Serializable;

/**
 * Instance with id
 */
public class InstanceId implements Serializable {
    protected Instance instance;
    protected int id;

    public InstanceId(Instance instance, int id) {
        this.instance = instance;
        this.id = id;
    }

    public Instance getInstance() {
        return instance;
    }

    public int getId() {
        return id;
    }
}

