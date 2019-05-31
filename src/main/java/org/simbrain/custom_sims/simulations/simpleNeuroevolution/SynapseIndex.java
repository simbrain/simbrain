package org.simbrain.custom_sims.simulations.simpleNeuroevolution;


public class SynapseIndex {
    int src;
    int tgt;
    double str;

    public SynapseIndex(int src, int tgt, double str) {
        setSrc(src);
        setTgt(tgt);
        setStrength(str);
    }

    public void setSrc(int src) {
        this.src = src;
    }

    public void setTgt(int tgt) {
        this.tgt = tgt;
    }

    public void setStrength(double str) {
        this.str = str;
    }

    public int getSrc() {
        return this.src;
    }

    public int getTgt() {
        return this.tgt;
    }


    public double getStrength() {
        return this.str;
    }

}
