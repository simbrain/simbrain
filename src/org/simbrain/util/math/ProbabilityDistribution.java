package org.simbrain.util.math;

import org.simbrain.util.propertyeditor2.EditableObject;

//  This will take the place of probdistribution.  Possibly change its name later
public abstract class ProbabilityDistribution implements EditableObject{

    public abstract double nextRand();

    public abstract int nextRandInt();
//    public abstract NeuronUpdateRule deepCopy();
}
