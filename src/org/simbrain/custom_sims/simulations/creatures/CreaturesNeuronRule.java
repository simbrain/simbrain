package org.simbrain.custom_sims.simulations.creatures;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.neuron_update_rules.LinearRule;

/**
 * 
 * @author Sharai
 *
 */

// TODO: Maybe change this into a spiking neuron rule once we figure out how to
// approach spike responders (should we transfer some functions/variables from
// here to a custom spike responder?) Might not be necessary if we can get
// linear neurons to retain a below-threshold activation value without
// firing.
public class CreaturesNeuronRule extends LinearRule {

    // TODO: Wire this up
    /** Reference to parent  lobe. */
    private NeuronGroup parentLobe;

    /**
     * The threshold at which the neuron will either fire (if state value is
     * greater than this number) or not.
     */
    private double nominalThreshold;

    /**
     * The rate at which the state value drops back to restState when the neuron
     * doesn't fire.
     */
    private double leakageRate;

    /**
     * The value that the state value rests at.
     */
    private double restState;

    /**
     * Modulates inputs. With high gain, the effects of input values are
     * increased; with low gain, the effects of input values are reduced.
     */
    private double inputGain;

    /**
     * Similar to activation. May ultimately be replaced by activation.   
     */
    private double stateValue;

    /**
     * Set this value to TRUE to skip the update process. Good for neurons that
     * are coupled as outputs.
     */
    // TODO: Is this necessary to have here, or does getting coupled as output
    // already override the typical update rule for neurons?
    private boolean override;

    public CreaturesNeuronRule() {
        this.nominalThreshold = 0.1;
        this.leakageRate = 0;
        this.restState = 0;
        this.inputGain = 0;
        this.override = false;
        this.stateValue = 0.0;
        setUpperBound(255);
        setLowerBound(0);
    }

    @Override
    public void update(Neuron neuron) {
        //System.out.println("updating neuron: " + neuron.getLabel());
        // SVRule stuff goes here.

        // Get input and modulate it with inputGain.
        double input = inputType.getInput(neuron);
        // TODO: Find an algorithm that can modulate input with inputGain. (In
        // the default genome in Creatures, input gain is always set at max
        // (255),
        // except for the decisions lobe (#6), which is set at 128)

        // Placeholder until SVRule stuff is implemented
        stateValue += input;

        // Clip values
        if (isClipped()) {
            stateValue = super.clip(stateValue);
        }

        // Update the neuron if override is not enabled
        if (!override) {
            if (stateValue > nominalThreshold) {
                neuron.setBuffer(stateValue);
                stateValue -= nominalThreshold;
            } else {
                neuron.setBuffer(0);
                // TODO: Find an algorithm that better replicates how leakage
                // rate in Creatures
                // works, such that 0 = Instant, 255 = 52 years.
                stateValue = restState + (stateValue * leakageRate);
            }

        }

        // super.update(neuron);
    }

    @Override
    public CreaturesNeuronRule deepCopy() {
        CreaturesNeuronRule nr = new CreaturesNeuronRule();
        nr.setNominalThresh(getNominalThresh());
        nr.setLeakageRate(getLeakageRate());
        nr.setRestState(getRestState());
        nr.setInputGain(getInputGain());
        nr.setStateValue(getStateValue());
        nr.setOverride(getOverride());
        super.setClipped(isClipped());
        super.setUpperBound(getUpperBound());
        super.setLowerBound(getLowerBound());
        return nr;
    }

    public void setNominalThresh(double num) {
        this.nominalThreshold = num;
    }

    public double getNominalThresh() {
        return nominalThreshold;
    }

    public void setLeakageRate(double num) {
        this.leakageRate = num;
    }

    public double getLeakageRate() {
        return leakageRate;
    }

    public void setRestState(double num) {
        this.restState = num;
    }

    public double getRestState() {
        return restState;
    }

    public void setInputGain(double num) {
        this.inputGain = num;
    }

    public double getInputGain() {
        return inputGain;
    }

    public void setStateValue(double num) {
        this.stateValue = num;
    }

    public double getStateValue() {
        return stateValue;
    }

    public void setOverride(boolean bool) {
        this.override = bool;
    }

    public boolean getOverride() {
        return override;
    }

    @Override
    public String getName() {
        return "Creatures Neuron";
    }
}
