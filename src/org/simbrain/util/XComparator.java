package org.simbrain.util;

import org.simbrain.network.pnodes.*;

	
public class XComparator implements java.util.Comparator {

	
    public XComparator() {}

    public int compare(Object o1, Object o2) {
    		PNodeNeuron p1 = (PNodeNeuron)o1;
    		PNodeNeuron p2 = (PNodeNeuron)o2;
    		Double d1 = new Double(p1.getXpos());
    		Double d2 = new Double(p2.getXpos());
        return d1.compareTo(d2);
    }
} 