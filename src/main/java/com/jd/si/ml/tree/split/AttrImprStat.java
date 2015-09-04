package com.jd.si.ml.tree.split;



import com.jd.si.ml.core.Attribute;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

public class AttrImprStat implements Serializable {

	private Map<String, Double> attrImpr;

	public AttrImprStat() {
		attrImpr = new HashMap<String, Double>();
	}

	public void updateAttrImpr(Attribute attr, double value) {
		String key = attr.index() + "-" + attr.name();

		if (attrImpr.containsKey(key)) {
			attrImpr.put(key, attrImpr.get(key) + value);
		} else {
			attrImpr.put(key, value);
		}
	}

	public Map<String, Double> getAttrImpr() {
		return attrImpr;
	}

	public void displayImpr() {
		List<Entry<String,Double>> attrImprList = new ArrayList<Entry<String,Double>>(attrImpr.entrySet());   
		
		Collections.sort(attrImprList, new Comparator<Entry<String, Double>>() {
		    public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
		    	if (o2.getValue() - o1.getValue() == 0) {
		    		return 0;
		    	} else if (o2.getValue() - o1.getValue() > 0) {
		    		return 1;
		    	} else {
					return -1;
				}
		    }
		}); 
		
		for (Entry<String, Double> entry : attrImprList) {
			System.out.println(entry.getKey() + ":" + entry.getValue());
		}
	}
}
