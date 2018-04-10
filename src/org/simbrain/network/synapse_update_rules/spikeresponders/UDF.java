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
package org.simbrain.network.synapse_update_rules.spikeresponders;

import org.simbrain.network.core.Synapse;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.math.ProbDistributions.NormalDistribution;
import org.simbrain.util.randomizer.Randomizer;

/**
 * An experimental no-GUI only implementation of the UDF synapse. This is a
 * stop gap implementation. UDF isn't really a spike responder, as it determines
 * the jump-height of a convolved jump and decay spike responder.
 *
 * @author Zoë Tosi
 */
public class UDF extends JumpAndDecay {

    /**
     * Use constant.
     */
    private double U;

    /**
     * Depression constant.
     */
    private double D;

    /**
     * Facilitation constant.
     */
    private double F;

    /**
     * The time of the last spike (recorded here since
     * SpikingNeuronUpdateRule writes over its own copy).
     */
    private double lastSpikeTime;

    /**
     * Use/Facilitation variable
     */
    private double u = 0.0;

    /**
     * Depression variable.
     */
    private double R = 1.0;

    /**
     * The actual spike responder for the post synaptic response for UDF.
     */
    private final ConvolvedJumpAndDecay spikeDecay = new ConvolvedJumpAndDecay();

    /**
     * Whether or not this is the first time this is being updated. If so it
     * initializes the variables.
     */
    private boolean firstTime = true;

    /**
     * Default constructor.
     */
    public UDF() {
    }

    /**
     * Does not actually copy this UDF object. Since UDF has values always
     * drawn from a distribution, it simply gives a new UDF object which
     * proceeds to draw its parameters from the same distributions.
     */
    @Override
    public UDF deepCopy() {
        return new UDF();
    }

    @Override
    public void update(Synapse s) {
        if (firstTime) {
            init(s);
            firstTime = false;
        }
        final double A;
        if (s.getSource().isSpike()) {
            final double ISI = lastSpikeTime - s.getNetwork().getTime();
            u = U + (u * (1 - U) * Math.exp(ISI / F));
            R = 1 + ((R - (u * R) - 1) * Math.exp(ISI / D));
            A = R * s.getStrength() * u;
            lastSpikeTime = s.getNetwork().getTime();
            spikeDecay.update(s, A);
        } else {
            spikeDecay.update(s);
        }
    }

    //    @Override
    //    public String getDescription() {
    //        return "Use, Depression, Facilitation (UDF) Short-term Plasticity";
    //    }

    /**
     * Sets the time constant for the decay of the PSR which is always
     * governed by a ConvolvedJumpAndDecay spike responder.
     *
     * @param timeConstant the time constant for PSR decay
     */
    public void setPSRDecayTimeConstant(double timeConstant) {
        spikeDecay.setTimeConstant(timeConstant);
    }

    /**
     * @return the decay time constant for the PSR.
     */
    public double getPSRDecayTimeConstant() {
        return spikeDecay.getTimeConstant();
    }

    /**
     * Initializes this UDF object based on the synapse it governs. UDF draws
     * its values from different distributions based on the polarity of the
     * source and target neurons.
     *
     * @param s the synapse which is used to determine what polarities of
     *          neurons the synapse connects and draw values based on that.
     */
    public void init(Synapse s) {
        Randomizer rand = new Randomizer();
        rand.setPdf(new NormalDistribution());
        rand.setClipping(true);
        rand.setUpperBound(Double.MAX_VALUE);
        rand.setLowerBound(0.0000001);
        if (s.getSource().getPolarity() == Polarity.EXCITATORY && s.getTarget().getPolarity() == Polarity.EXCITATORY) {
            ((NormalDistribution) rand.getPdf()).setMean(0.5);
            ((NormalDistribution) rand.getPdf()).setStandardDeviation(0.25);
            U = rand.getRandom();
            ((NormalDistribution) rand.getPdf()).setMean(1100);
            ((NormalDistribution) rand.getPdf()).setStandardDeviation(550);
            D = rand.getRandom();
            ((NormalDistribution) rand.getPdf()).setMean(50);
            ((NormalDistribution) rand.getPdf()).setStandardDeviation(25);
            F = rand.getRandom();
            spikeDecay.setTimeConstant(3);
        } else if (s.getSource().getPolarity() == Polarity.EXCITATORY && s.getTarget().getPolarity() == Polarity.INHIBITORY) {
            ((NormalDistribution) rand.getPdf()).setMean(0.05);
            ((NormalDistribution) rand.getPdf()).setStandardDeviation(0.025);
            U = rand.getRandom();
            ((NormalDistribution) rand.getPdf()).setMean(125);
            ((NormalDistribution) rand.getPdf()).setStandardDeviation(62.5);
            D = rand.getRandom();
            ((NormalDistribution) rand.getPdf()).setMean(120);
            ((NormalDistribution) rand.getPdf()).setStandardDeviation(60);
            F = rand.getRandom();
            spikeDecay.setTimeConstant(3);
        } else if (s.getSource().getPolarity() == Polarity.INHIBITORY && s.getTarget().getPolarity() == Polarity.EXCITATORY) {
            ((NormalDistribution) rand.getPdf()).setMean(0.25);
            ((NormalDistribution) rand.getPdf()).setStandardDeviation(0.125);
            U = rand.getRandom();
            ((NormalDistribution) rand.getPdf()).setMean(700);
            ((NormalDistribution) rand.getPdf()).setStandardDeviation(350);
            D = rand.getRandom();
            ((NormalDistribution) rand.getPdf()).setMean(20);
            ((NormalDistribution) rand.getPdf()).setStandardDeviation(10);
            F = rand.getRandom();
            spikeDecay.setTimeConstant(6);
        } else if (s.getSource().getPolarity() == Polarity.INHIBITORY && s.getTarget().getPolarity() == Polarity.INHIBITORY) {
            ((NormalDistribution) rand.getPdf()).setMean(0.32);
            ((NormalDistribution) rand.getPdf()).setStandardDeviation(0.16);
            U = rand.getRandom();
            ((NormalDistribution) rand.getPdf()).setMean(144);
            ((NormalDistribution) rand.getPdf()).setStandardDeviation(72);
            D = rand.getRandom();
            ((NormalDistribution) rand.getPdf()).setMean(60);
            ((NormalDistribution) rand.getPdf()).setStandardDeviation(30);
            F = rand.getRandom();
            spikeDecay.setTimeConstant(6);
        } else {
            ((NormalDistribution) rand.getPdf()).setMean(0.5);
            ((NormalDistribution) rand.getPdf()).setStandardDeviation(0.25);
            U = rand.getRandom();
            ((NormalDistribution) rand.getPdf()).setMean(1100);
            ((NormalDistribution) rand.getPdf()).setStandardDeviation(550);
            D = rand.getRandom();
            ((NormalDistribution) rand.getPdf()).setMean(50);
            ((NormalDistribution) rand.getPdf()).setStandardDeviation(25);
            F = rand.getRandom();
            spikeDecay.setTimeConstant(3);
        }
        u = U;
    }

}
