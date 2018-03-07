package org.simbrain.util.math.PropDistribution;

import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.util.propertyeditor2.EditableObject;

public abstract class ProbabilityDistribution implements EditableObject{
    public abstract double nextRand();
    public abstract int nextRandInt();
//    public abstract NeuronUpdateRule deepCopy();
}
