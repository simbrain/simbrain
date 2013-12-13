package org.simbrain.network.neuron_update_rules.interfaces;

//TODO: Document
public interface ClippableUpdateRule {

    public double clip(double val);

    public boolean isClipped();

    public void setClipped(boolean clipping);

}
