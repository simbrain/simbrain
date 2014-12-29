package org.simbrain.network.synapse_update_rules.spikeresponders;

import org.simbrain.network.core.Synapse;

/**
 * 
 * @author Zach Tosi
 *
 */
public class ConvolvedJumpAndDecay extends SpikeResponder {

    /** Jump height value. */
    private double jumpHeight = 1;

    /** Base line value. */
    private double baseLine = 0.0;

    /** Rate at which synapse will decay (ms). */
    private double timeConstant = 3;

    /**
     * {@inheritDoc}
     */
    @Override
    public ConvolvedJumpAndDecay deepCopy() {
        ConvolvedJumpAndDecay jad = new ConvolvedJumpAndDecay();
        jad.setBaseLine(this.getBaseLine());
        jad.setJumpHeight(this.getJumpHeight());
        jad.setTimeConstant(this.getTimeConstant());
        return jad;
    }

    /**
     * {@inheritDoc}
     */
    public void update(final Synapse s) {
        value = s.getPsr();
        if (s.getSource().isSpike()) {
            value += jumpHeight * s.getStrength();
        } else {
            double timeStep = s.getParentNetwork().getTimeStep();
            value += timeStep * (baseLine - value) / timeConstant;
        }
        s.setPsr(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Convolved Jump and Decay";
    }

    /**
     * @return Returns the baseLine.
     */
    public double getBaseLine() {
        return baseLine;
    }

    /**
     * @param baseLine The baseLine to set.
     */
    public void setBaseLine(final double baseLine) {
        this.baseLine = baseLine;
    }

    /**
     * @return Returns the jumpHeight.
     */
    public double getJumpHeight() {
        return jumpHeight;
    }

    /**
     * @param jumpHeight The jumpHeight to set.
     */
    public void setJumpHeight(final double jumpHeight) {
        this.jumpHeight = jumpHeight;
    }

    /**
     * @return Name of synapse type.
     */
    public static String getName() {
        return "Convolved Jump and decay";
    }

    /**
     * @return the time constant of the exponential decay of the post synaptic
     *         response
     */
    public double getTimeConstant() {
        return timeConstant;
    }

    /**
     * @param decayTimeConstant the new time constant of the exponential decay
     *            of the post synaptic response
     */
    public void setTimeConstant(double decayTimeConstant) {
        this.timeConstant = decayTimeConstant;
    }

}
