package org.simbrain.network.trainers;

public abstract class TrainingMethod {

    /**
     * Apply the algorithm. For iterable algorithms, this represents a single
     * iteration.
     */
    public abstract void apply(Trainer trainer);

    /**
     * Override for initialization.
     */
    public void init(Trainer trainer) {};
    
    /**
     * Randomize the network associated with this trainer, as appropriate to the
     * algorithm.
     */
    public void randomize(Trainer trainer) {};
}
