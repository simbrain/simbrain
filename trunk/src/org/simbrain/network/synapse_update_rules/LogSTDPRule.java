package org.simbrain.network.synapse_update_rules;

import org.simbrain.network.core.SpikingNeuronUpdateRule;
import org.simbrain.network.core.Synapse;
import org.simbrain.util.math.ProbDistribution;

public class LogSTDPRule extends STDPRule {

    private double smallWtThreshold = 1.0;

    private double w_plus = 0.8;

    private double w_minus = 0.6;

    private double logSaturation = 5;

    private double ltpMod = 5;

    private double noiseVar = 0.6;

    public void update(Synapse synapse) {
        boolean sourceSpiking = synapse.getSource().getUpdateRule()
                .isSpikingNeuron();
        boolean targetSpiking = synapse.getTarget().getUpdateRule()
                .isSpikingNeuron();
        if (!sourceSpiking || !targetSpiking) {
            return; // STDP is non-sensical if one of the units doesn't spike...
        }
        SpikingNeuronUpdateRule src = (SpikingNeuronUpdateRule) synapse
                .getSource().getUpdateRule();
        SpikingNeuronUpdateRule tar = (SpikingNeuronUpdateRule) synapse
                .getTarget().getUpdateRule();
        double delta_t, delta_w;
        final double timeStep = synapse.getNetwork().getTimeStep();
        final double delay = synapse.getDelay() * timeStep;
        if (synapse.getStrength() >= 0) {
            delta_t = (src.getLastSpikeTime() + delay)
                    - tar.getLastSpikeTime();
        } else {
            delta_t = tar.getLastSpikeTime()
                    - (src.getLastSpikeTime() + delay);
        }
        double noise = (1 + ProbDistribution.NORMAL.nextRand(0, noiseVar));
        if (delta_t < 0) {
            calcW_plusTerm(synapse);
            delta_w = timeStep * learningRate * (W_plus * Math.exp(delta_t
                    / tau_plus)) * noise;
        } else if (delta_t > 0) {
            calcW_minusTerm(synapse);
            delta_w = timeStep * learningRate * (-W_minus * Math.exp(-delta_t
                    / tau_minus)) * noise;
        } else {
            delta_w = 0;
        }
        synapse.setStrength(synapse.clip(synapse.getStrength() + delta_w));
    }

    /**
     * 
     * @param s
     * @return
     */
    private double calcW_plusTerm(Synapse s) {
        W_plus = w_plus
                * Math.exp(-Math.abs(s.getStrength()) / (smallWtThreshold
                        * ltpMod));
        if (s.getStrength() > 0) {
            if (s.getStrength() >= s.getUpperBound()) {
                W_plus = 0;
            } else {
                W_plus *= Math.exp(-2 *Math.pow(s.getStrength()
                        / (s.getUpperBound() - s.getStrength()), 2));
            }
        } else {
            if (s.getStrength() <= s.getLowerBound()) {
                W_plus = 0;
            } else {
                W_plus *= Math.exp(-2 *Math.pow(s.getStrength()
                        / (s.getLowerBound() - s.getStrength()), 2));
            }
        }
        return W_plus;
    }

    private double calcW_minusTerm(Synapse s) {
        double wt = Math.abs(s.getStrength());
        if (wt <= smallWtThreshold) {
            W_minus = w_minus * wt / smallWtThreshold;
        } else {

            double numerator = Math.log(1 + (logSaturation
                    * ((wt / smallWtThreshold) - 1)));
            W_minus = w_minus * (1 + (numerator / logSaturation));
        }
        if (s.getStrength() < 0) {
            if (s.getStrength() >= s.getUpperBound()) {
                W_minus = 0;
            } else {
                W_minus *= Math.exp(-2 * Math.pow((s.getLowerBound()
                        - s.getStrength()) / (s.getUpperBound()
                                - s.getStrength()), 2));
            }
        } else {
            if (s.getStrength() <= s.getLowerBound()) {
                W_minus = 0;
            } else {
                W_minus *= Math.exp(-2 * Math.pow((s.getUpperBound()
                        - s.getStrength()) / (s.getLowerBound()
                                - s.getStrength()), 2));
            }
        }
        return W_minus;
    }

    public double getSmallWtThreshold() {
        return smallWtThreshold;
    }

    public void setSmallWtThreshold(double smallWtThreshold) {
        this.smallWtThreshold = smallWtThreshold;
    }

    public double getW_plus() {
        return w_plus;
    }

    public void setW_plus(double w_plus) {
        this.w_plus = w_plus;
    }

    public double getW_minus() {
        return w_minus;
    }

    public void setW_minus(double w_minus) {
        this.w_minus = w_minus;
    }

    public double getLogSaturation() {
        return logSaturation;
    }

    public void setLogSaturation(double logSaturation) {
        this.logSaturation = logSaturation;
    }

    public double getLtpMod() {
        return ltpMod;
    }

    public void setLtpMod(double ltpMod) {
        this.ltpMod = ltpMod;
    }

    public double getNoiseVar() {
        return noiseVar;
    }

    public void setNoiseVar(double noiseVar) {
        this.noiseVar = noiseVar;
    }
}
