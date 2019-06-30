package org.simbrain.util.geneticalgorithm;

import org.simbrain.util.math.SimbrainRandomizer;

import java.util.function.Supplier;

/**
 * A template that encodes information about an object used in an evolutionary
 * simulation.
 *
 * @param <T> The type of the object expressed by this gene.
 */
public abstract class Gene<T> {

    /**
     * Some genes should not be mutated, e.g. "input node" genes.
     */
    private boolean mutable = true;

    private SimbrainRandomizer randomizer;

    /**
     * Return a deep copy of this gene.
     *
     * @return a deep copy of this gene.
     */
    public abstract Gene<T> copy();

    /**
     * This is an object that holds the information related to this gene. It is
     * agnostic between simple and more complex genes.
     * <br>
     * A simple example is a DoubleObject that can then return double values
     * <br>
     * A more complex example is a Neuron "prototype" , that encodes information about a
     * neuron type (like upper bound, update rule, etc), and that can then return instances
     * of itself.
     *
     * @return the prototype object
     */
    public abstract T getPrototype();

    /**
     * Add random mutation to this gene.
     */
    public abstract void mutate();

    public boolean isMutable() {
        return mutable;
    }

    public void setMutable(boolean mutable) {
        this.mutable = mutable;
    }

}

