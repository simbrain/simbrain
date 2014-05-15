package org.simbrain.network.neuron_update_rules.interfaces;

//TODO: Document.  Interface for bounded rules with no intrinsic boundaries.  E.g Linear.
// All clippable are bounded but not all bounded are clippable.
public interface ClippableUpdateRule {

    public double clip(double val);

    public boolean isClipped();

    public void setClipped(boolean clipping);

}
