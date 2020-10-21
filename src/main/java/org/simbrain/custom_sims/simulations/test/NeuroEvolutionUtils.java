package org.simbrain.custom_sims.simulations.test;

import org.simbrain.network.core.Neuron;
import org.simbrain.util.geneticalgorithms.numerical.DoubleChromosome;
import org.simbrain.util.geneticalgorithms.numerical.IntegerChromosome;

/**
 * Planning to develop utilities here for getting a neuron
 * associated with a numerical chromosome.   Possibly do this
 * automatically, scanning the annotations of a neuron type for
 * {@link org.simbrain.util.UserParameter}s , and setting max and min
 * on that basis.
 */
public class NeuroEvolutionUtils {

    public static Neuron getNeuron(IntegerChromosome ic, DoubleChromosome dc) {
        return null;
    }


}
