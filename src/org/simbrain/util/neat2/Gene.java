package org.simbrain.util.neat2;

public abstract class Gene<T> {

    private boolean mutable = true;

    /**
     * Return a deep copy of this gene.
     *
     * @return a deep copy of this gene.
     */
    public abstract Gene<T> copy();

    /**
     * Crossing over where this gene is the dominant gene and the parameter gene is the gene with less fitness.
     *
     * @param other the other gene to cross over with
     * @return A offspring of this gene and the other gene.
     */
    public abstract Gene crossOver(Gene<T> other);

    /**
     * Get the prototype that encodes/defines this gene.
     *
     * @return the prototype
     */
    public abstract T getPrototype();

    public boolean isMutable() {
        return mutable;
    }

    /**
     * Add random mutation to this gene.
     */
    public abstract void mutate();

    public void setMutable(boolean mutable) {
        this.mutable = mutable;
    }
}
