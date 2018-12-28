/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.neuron_update_rules;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.SpikingNeuronUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.NoisyUpdateRule;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.UserParameter;
import org.simbrain.util.math.ProbDistributions.UniformDistribution;
import org.simbrain.util.math.ProbabilityDistribution;

/**
 * An implementation of adaptive exponential integrate and fire. This version
 * of integrate and fire includes an exponential term as a part of the
 * differential equation as well as an adaptation term which lowers the
 * membrane potential in response to successive spikes.
 * See Toboul &#38; Brette 2005.
 *
 * @author Zoë Tosi
 */
public class AdExIFRule extends SpikingNeuronUpdateRule implements NoisyUpdateRule {

    /**
     * A converter from pA to nA, since most other sims in Simbrain use
     * nano Amps.
     */
    public static final double CURRENT_CONVERTER = 1000;

    /**
     * Reset voltage (mV). Defaults to 3-spike bursting behavior at .8 nA
     * current. See Touboul & Brette 2005 -48.5: 2 spike burst -47.2: 4 spike
     * burst -48: chaotic spike response
     */
    @UserParameter(
            label = "Reset voltage (mV)",
            description = "This represents the voltage to which the membrane potential will be reset after "
                    + "an action potential has fired.",
            defaultValue = "-47.7", order = 3, tab = "Membrane Voltage")
    private double v_Reset = -47.7;

    /**
     * Threshold voltage (mV). This determines when a neuron will start a
     * divergent change in voltage that will tend toward infinity and is not the
     * voltage at which we consider the neuron to have spiked. External factors
     * can still cause an action potential to fail even if v_mem > v_Th.
     */
    @UserParameter(
            label = "Threshold voltage (mV)",
            description = "This determines when a neuron will start a divergent change in voltage that will tend "
                    + "toward infinity and is not the voltage at which we consider the neuron to have spiked.",
            defaultValue = "-50.4", order = 2, tab = "Membrane Voltage")
    private double v_Th = -50.4;

    /**
     * The peak voltage after which we say with certainty that an action
     * potential has occurred (mV).
     */
    @UserParameter(
            label = "Peak Voltage (mV)",
            description = "The peak voltage after which we say with certainty that an action potential has occurred (mV).",
            defaultValue = "20", order = 1, tab = "Membrane Voltage")
    private double v_Peak = 20;

    /**
     * Leak Conductance (nS).
     */
    @UserParameter(
            label = "Leak Conductance (nS)",
            description = "The inverse of the resistance of the channels through which current leaks from the neuron.",
            defaultValue = "30", order = 6, tab = "Input Currents")
    private double g_L = 30;

    /**
     * Maximal excitatory conductance. (nS)
     */
    @UserParameter(
            label = "Max Ex. Conductance (nS)",
            description = "The excitatory conductance if all excitatory channels are open.",
            defaultValue = "10", order = 7, tab = "Input Currents")
    private double g_e_bar = 10;

    /**
     * Maximal inhibitory conductance. (nS)
     */
    @UserParameter(
            label = "Max In. Conductance (nS)",
            description = "The inhibitory conductance if all inhibitory channels are open.",
            defaultValue = "10", order = 8, tab = "Input Currents")
    private double g_i_bar = 10;

    /**
     * Leak strength (mV).
     */
    @UserParameter(
            label = "Leak Reversal (mV)",
            description = "The membrane potential at which leak currents would no longer have "
                    + "any effect on the neuron's membrane potential.",
            defaultValue = "-70.6", order = 9, tab = "Input Currents")
    private double leakReversal = -70.6;

    /**
     * Excitatory reversal. (mV).
     */
    @UserParameter(
            label = "Excitatory Reversal (mV)",
            description = "The membrane potential at which impinging excitatory (depolarizing) "
                    + "inputs reach equilibrium.",
            defaultValue = "0", order = 10, tab = "Input Currents")
    private double exReversal = 0;

    /**
     * Inhibitory reversal. (mV)
     */
    @UserParameter(
            label = "Inbitatory Reversal (mV)",
            description = "The membrane potential at which impinging inhibitory (hyperpolarizing) "
                    + "inputs reach equilibrium.",
            defaultValue = "-75", order = 11, tab = "Input Currents")
    private double inReversal = -75;

    /**
     * Membrane potential (mV).
     */
    private double v_mem = leakReversal;

    /**
     * Adaptation variable.
     */
    private double w = 200;

    /**
     * Adaptation reset parameter (nA).
     */
    @UserParameter(
            label = "Reset (nA)",
            description = "Adaptation reset parameter (nA)",
            defaultValue = "0.0805", order = 12, tab = "Adaptation")
    private double b = 0.0805;

    /**
     * Adaptation time constant (ms).
     */
    @UserParameter(
            label = "Time constant (ms)",
            description = "Controls the rate at which the neuron attains its resting potential.",
            defaultValue = "40", order = 13, tab = "Adaptation")
    private double tauW = 40;

    /**
     * mV
     */
    @UserParameter(
            label = "Slope Factor",
            description = "A value which regulates the overall effect of the exponential term on "
                    + "the membrane potential equation.",
            defaultValue = "2", order = 6, tab = "Membrane Voltage")
    private double slopeFactor = 2;

    /**
     * Adaptation coupling parameter (nS).
     */
    @UserParameter(
            label = "Coupling Const.",
            description = "This represents the voltage to which the membrane potential will be reset after "
                    + "an action potential has fired.",
            defaultValue = "4", order = 14, tab = "Adaptation")
    private double a = 4;

    /**
     * Membrane Capacitance (pico Farads).
     */
    @UserParameter(
            label = "Capacitance (μF)",
            description = "A paramter designating the overall ability of the neuron's membrane to retain a charge.",
            defaultValue = "281", order = 4, tab = "Membrane Voltage")
    private double memCapacitance = 281;

    /**
     * Background current being directly injected into the neuron (nA).
     */
    @UserParameter(
            label = "Background Current (nA)",
            description = "A tunable parameter in some ways similar to a bias parameter for non-spiking neurons.",
            defaultValue = "0", order = 5, tab = "Membrane Voltage")
    private double i_bg = 0;

    /**
     * An option to add noise.
     */
    private boolean addNoise = false;

    /**
     * The noise generator randomizer.
     */
    private ProbabilityDistribution noiseGenerator = UniformDistribution.create();

    /**
     * An absolute refractory period. Not normally a part of AdEx, but can
     * optionally be used to promote network stability.
     */
    private double refractoryPeriod = 1.0;

    private double[] ei = new double[2];

    @Override
    public void update(Neuron neuron) {
        if (v_mem >= v_Peak) {
            v_mem = v_Reset;
            neuron.forceSetActivation(v_Reset);
        }
        // Retrieve integration time constant in case it has changed...
        final double dt = neuron.getNetwork().getTimeStep();
        //        final double ref = neuron.getNetwork().getTimeType()
        //                == TimeType.DISCRETE ? refractoryPeriod / dt
        //                        : refractoryPeriod;
        final boolean refractory = getLastSpikeTime() + refractoryPeriod >= neuron.getNetwork().getTime();


        // Retrieve membrane potential from host neuron's activation
        // in case some outside entity has explicitly changed the membrane
        // potential between updates.
        v_mem = neuron.getActivation();


        // Retrieve incoming ex/in currents or proportion of open channels
        ei[0] = 0;
        ei[1] = 0;
        for(int ii=0; ii<neuron.getFanIn().size(); ++ii) {
            double val = neuron.getFanIn().get(ii).calcPSR();
            if(neuron.getPolarity() == SimbrainConstants.Polarity.INHIBITORY) {
                ei[1] += val;
            } else {
                ei[0] += val;
            }
        }

        // Calculate incoming excitatory and inhibitory voltage changes
        double iSyn_ex = g_e_bar * ei[0] * (exReversal - v_mem);
        double iSyn_in = -g_i_bar * ei[1] * (inReversal - v_mem);

        // Calculate voltage changes due to leak
        double i_leak = g_L * (leakReversal - v_mem);
        double ibg = i_bg;


        // Add noise if there is any to be added
        if (addNoise) {
            ibg += noiseGenerator.getRandom();
        }

        // Calc dV/dt for membrane potential
        double dVdt = (g_L * slopeFactor * Math.exp((v_mem - v_Th) / slopeFactor)) + i_leak + iSyn_ex + iSyn_in + ibg - w;


        // Factor in membane capacitance...
        dVdt /= memCapacitance;

        // Calculate adaptation change
        double dwdt = (a * (v_mem - leakReversal) - w) / tauW;

        // Integrate membrane potential and adaptation parameter using
        // Euler integration
        v_mem += (dVdt * dt);
        w += (dwdt * dt);

        // Spike?
        if (v_mem >= v_Peak) {
            v_mem = v_Peak;
            w = w + (b * CURRENT_CONVERTER);
            if (!refractory) {
                neuron.setSpkBuffer(true);
                setHasSpiked(true, neuron);
            } else {
                neuron.setSpkBuffer(false);
                setHasSpiked(false, neuron);
            }
        } else {
            neuron.setSpkBuffer(false);
            setHasSpiked(false, neuron);
        }

        // Set the buffer to the membrane potential
        neuron.setBuffer(v_mem);

    }

    @Override
    public AdExIFRule deepCopy() {
        AdExIFRule cpy = new AdExIFRule();
        cpy.a = this.a;
        cpy.addNoise = this.addNoise;
        cpy.b = this.b;
        cpy.g_L = this.g_L;
        cpy.leakReversal = this.leakReversal;
        cpy.memCapacitance = this.memCapacitance;
        cpy.noiseGenerator = noiseGenerator.deepCopy();
        cpy.slopeFactor = this.slopeFactor;
        cpy.tauW = this.tauW;
        cpy.v_mem = this.v_mem;
        cpy.v_Reset = this.v_Reset;
        cpy.v_Th = this.v_Th;
        cpy.w = this.w;
        return cpy;
    }

    @Override
    public String getName() {
        return "AdEx Integrate and Fire";
    }

    @Override
    public ProbabilityDistribution getNoiseGenerator() {
        return noiseGenerator;
    }

    @Override
    public void setNoiseGenerator(final ProbabilityDistribution noise) {
        this.noiseGenerator = noise;
    }

    @Override
    public boolean getAddNoise() {
        return addNoise;
    }

    @Override
    public void setAddNoise(boolean noise) {
        this.addNoise = noise;
    }

    @Override
    public double getGraphicalLowerBound() {
        return leakReversal - 20;
    }

    @Override
    public double getGraphicalUpperBound() {
        return v_Th + 10;
    }

    public double getV_Reset() {
        return v_Reset;
    }

    public void setV_Reset(double v_Reset) {
        this.v_Reset = v_Reset;
    }

    public double getV_Th() {
        return v_Th;
    }

    public void setV_Th(double v_Th) {
        this.v_Th = v_Th;
    }

    public double getG_L() {
        return g_L;
    }

    public void setG_L(double g_L) {
        this.g_L = g_L;
    }

    public double getLeakReversal() {
        return leakReversal;
    }

    public void setLeakReversal(double leakReversal) {
        this.leakReversal = leakReversal;
    }

    public double getV_mem() {
        return v_mem;
    }

    public void setV_mem(double v_mem) {
        this.v_mem = v_mem;
    }

    public double getW() {
        return w;
    }

    public void setW(double w) {
        this.w = w;
    }

    public double getB() {
        return b;
    }

    public void setB(double b) {
        this.b = b;
    }

    public double getTauW() {
        return tauW;
    }

    public void setTauW(double tauW) {
        this.tauW = tauW;
    }

    public double getSlopeFactor() {
        return slopeFactor;
    }

    public void setSlopeFactor(double slopeFactor) {
        this.slopeFactor = slopeFactor;
    }

    public double getA() {
        return a;
    }

    public void setA(double a) {
        this.a = a;
    }

    public double getMemCapacitance() {
        return memCapacitance;
    }

    public void setMemCapacitance(double memCapacitance) {
        this.memCapacitance = memCapacitance;
    }

    public double getI_bg() {
        return i_bg / CURRENT_CONVERTER;
    }

    public void setI_bg(double i_bg) {
        // Conversion so that bg currents can be entered as nano amperes
        // instead of as pico amperes, which is more consistent with units
        // used elsewhere in Simbrain.
        this.i_bg = CURRENT_CONVERTER * i_bg;
    }

    public double getV_Peak() {
        return v_Peak;
    }

    public void setV_Peak(double v_Peak) {
        this.v_Peak = v_Peak;
    }

    public double getG_e_bar() {
        return g_e_bar;
    }

    public void setG_e_bar(double g_e_bar) {
        this.g_e_bar = g_e_bar;
    }

    public double getG_i_bar() {
        return g_i_bar;
    }

    public void setG_i_bar(double g_i_bar) {
        this.g_i_bar = g_i_bar;
    }

    public double getExReversal() {
        return exReversal;
    }

    public void setExReversal(double exReversal) {
        this.exReversal = exReversal;
    }

    public double getInReversal() {
        return inReversal;
    }

    public void setInReversal(double inReversal) {
        this.inReversal = inReversal;
    }

    public double getRefractoryPeriod() {
        return refractoryPeriod;
    }

    public void setRefractoryPeriod(double refractoryPeriod) {
        this.refractoryPeriod = refractoryPeriod;
    }

}
