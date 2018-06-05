package org.simbrain.network.synapse_update_rules.spikeresponders;

import org.simbrain.network.core.Synapse;
import org.simbrain.util.UserParameter;

/**
 * @author Zoë Tosi
 */
public class ConvolvedJumpAndDecay extends SpikeResponder {

    /**
     * Jump height value.
     */
    @UserParameter(label = "Jump Height",
            description = "This value is multiplied by the strength to determine the total instantaneous rise in a"
                    + " post-synaptic response to an action potential or spike.",
            defaultValue = "1", order = 1)
    private double jumpHeight;

    /**
     * Base line value.
     */
    @UserParameter(label = "Base-Line",
            description = "The post-synaptic response value when no spike have occurred. Alternatively, the "
                    + "post synaptic response to which decays to over time.",
            defaultValue = "0.0001", order = 1)
    private double baseLine;

    /**
     * Rate at which synapse will decay (ms).
     */
    @UserParameter(label = "Time Constant",
            description = "The time constant of decay and recovery (ms).",
            defaultValue = "3", order = 1)
    private double timeConstant;

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

    @Override
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

    public void update(final Synapse s, double jump) {
        value = s.getPsr();
        if (s.getSource().isSpike()) {
            value += jump;
        } else {
            double timeStep = s.getParentNetwork().getTimeStep();
            value += timeStep * (baseLine - value) / timeConstant;
        }
        s.setPsr(value);
    }

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
    public String getName() {
        return "Convolved Jump and decay";
    }

    /**
     * @return the time constant of the exponential decay of the post synaptic
     * response
     */
    public double getTimeConstant() {
        return timeConstant;
    }

    /**
     * @param decayTimeConstant the new time constant of the exponential decay
     *                          of the post synaptic response
     */
    public void setTimeConstant(double decayTimeConstant) {
        this.timeConstant = decayTimeConstant;
    }

}
