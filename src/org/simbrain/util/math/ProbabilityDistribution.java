package org.simbrain.util.math;

import org.simbrain.util.propertyeditor2.EditableObject;

import umontreal.iro.lecuyer.rng.LFSR113;
import umontreal.iro.lecuyer.rng.RandomStream;

//  This will take the place of probdistribution.  Possibly change its name later
public abstract class ProbabilityDistribution implements EditableObject{
    
    public static final RandomStream DEFAULT_RANDOM_STREAM = new LFSR113();

    public abstract double nextRand();

    public abstract int nextRandInt();
    public abstract ProbabilityDistribution deepCopy();
    public abstract String getName();

    protected static double clipping(double value, double lowerBound, double upperBound) {
        double result = value;

        if (result > upperBound) {
            result = upperBound;
        } else if (result < lowerBound) {
            result = lowerBound;
        }

        return result;
    }
}
