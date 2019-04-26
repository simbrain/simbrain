package org.simbrain.util.neat2;

import java.util.List;

public abstract class Genome<T> {

    private List<Gene<T>> genes;

    public abstract Genome<T> crossOver(Genome<T> other);

}
