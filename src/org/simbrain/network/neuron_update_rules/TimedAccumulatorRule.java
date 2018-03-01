package org.simbrain.network.neuron_update_rules;

import org.simbrain.network.core.Network.TimeType;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;

import java.util.concurrent.ThreadLocalRandom;

public class TimedAccumulatorRule extends SpikingThresholdRule {

    public static final double DEFAULT_KAPPA = 0.9;

    private int maxState = 10;

    private int currentState;

    private double baseProb = 0.00001;

    private double b = 1.6;

    private double kappa = DEFAULT_KAPPA;

    private double expSum = 0;

    private int fanInSize = 0;

    @Override
    public TimeType getTimeType() {
        return TimeType.DISCRETE;
    }

    @Override
    public void update(Neuron neuron) {
        if (currentState >= 1) {
            currentState++;
            if (currentState > maxState) {
                currentState = 0;
            }
            neuron.setBuffer(currentState);
            neuron.setSpkBuffer(false);
            setHasSpiked(false, neuron);
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() < baseProb) {
            currentState++;
            neuron.setBuffer(1);
            neuron.setSpkBuffer(true);
            setHasSpiked(true, neuron);
            return;
        }
        for (int i = 0; i < fanInSize; i++) {
            // Skip if pre-synaptic node is off...
            if (neuron.getFanIn().get(i).getSource().getActivation() == 1) {
                // Using the exp weight value stored in the PSR from before
                // divide that by the exp sum to get the softmax value
                // then set this to a 1 state from a 0 with that probability.
                if (ThreadLocalRandom.current().nextDouble() < kappa * neuron.getFanIn().get(i).getPsr() / expSum) {
                    currentState++;
                    neuron.setBuffer(1);
                    neuron.setSpkBuffer(true);
                    setHasSpiked(true, neuron);
                    return;
                }
            }
        }
        neuron.setBuffer(0);
        neuron.setSpkBuffer(false);
        setHasSpiked(false, neuron);
    }

    public void init(Neuron neuron) {
        fanInSize = neuron.getFanIn().size();
        // Obtain the exponential sum for the denominator
        for (Synapse s : neuron.getFanIn()) {
            double expVal = Math.exp(-s.getStrength() * b);
            expSum += expVal;
            s.setPsr(expVal); // Store the exp val of the weight for later use
        }
    }

    @Override
    public TimedAccumulatorRule deepCopy() {
        TimedAccumulatorRule tar = new TimedAccumulatorRule();
        tar.setBaseProb(baseProb);
        tar.setCurrentState(currentState);
        tar.setMaxState(maxState);
        tar.setKappa(kappa);
        tar.setLastSpikeTime(getLastSpikeTime());
        return tar;
    }

    @Override
    public final boolean isSkipsSynapticUpdates() {
        return true;
    }

    public int getMaxState() {
        return maxState;
    }

    public void setMaxState(int maxState) {
        this.maxState = maxState;
    }

    public int getCurrentState() {
        return currentState;
    }

    public void setCurrentState(int currentState) {
        this.currentState = currentState;
    }

    public double getBaseProb() {
        return baseProb;
    }

    public void setBaseProb(double baseProb) {
        this.baseProb = baseProb;
    }

    public double getKappa() {
        return kappa;
    }

    public void setKappa(double kappa) {
        this.kappa = kappa;
    }

    public static double getDefaultKappa() {
        return DEFAULT_KAPPA;
    }

    public double getB() {
        return b;
    }

    public void setB(double b) {
        this.b = b;
    }

}
