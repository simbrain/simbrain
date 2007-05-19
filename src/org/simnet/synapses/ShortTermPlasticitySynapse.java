/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/Documentation/docs/SimbrainDocs.html#Credits
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
package org.simnet.synapses;

import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.SpikingNeuron;
import org.simnet.interfaces.Synapse;


/**
 * <b>ShortTermPlasticitySynapse</b>.
 */
public class ShortTermPlasticitySynapse extends Synapse {

    /** STD. */
    private static final int STD = 0;

    /** STF. */
//    private static final int STF = 1;

    /** Plasticity type. */
    private int plasticityType = STD;

    /** Pseudo spike threshold. */
    private double firingThreshold = 0;

    /** Base line strength. */
    private double baseLineStrength = 1;

    /** Input threshold. */
    private double inputThreshold = 0;

    /** Bump rate. */
    private double bumpRate = .5;

    /** Rate at which the synapse will decay. */
    private double decayRate = .2;

    /** Activated. */
    private boolean activated = false;

    /**
     * Creates a weight of some value connecting two neurons.
     *
     * @param src source neuron
     * @param tar target neuron
     * @param val initial weight value
     * @param theId Id of the synapse
     */
    public ShortTermPlasticitySynapse(final Neuron src, final Neuron tar, final double val, final String theId) {
        source = src;
        target = tar;
        strength = val;
        id = theId;
    }

    /**
     * Default constructor needed for external calls which create neurons then  set their parameters.
     */
    public ShortTermPlasticitySynapse() {
    }

    /**
     * This constructor is used when creating a neuron of one type from another neuron of another type Only values
     * common to different types of neuron are copied.
     * @param s Synapse to make of the type
     */
    public ShortTermPlasticitySynapse(final Synapse s) {
        super(s);
    }

    /**
     * @return Name of synapse type.
     */
    public static String getName() {
        return "Short term plasticity";
    }

    /**
     * @return duplicate DeltaRuleSynapse (used, e.g., in copy/paste).
     */
    public Synapse duplicate() {
        ShortTermPlasticitySynapse stp = new ShortTermPlasticitySynapse();
        stp = (ShortTermPlasticitySynapse) super.duplicate(stp);
        stp.setBaseLineStrength(getBaseLineStrength());
        stp.setBumpRate(getBumpRate());
        stp.setDecayRate(getDecayRate());
        stp.setInputThreshold(getInputThreshold());
        stp.setPlasticityType(getPlasticityType());

        return stp;
    }

    /**
     * Creates a weight connecting source and target neurons.
     *
     * @param source source neuron
     * @param target target neuron
     */
    public ShortTermPlasticitySynapse(final Neuron source, final Neuron target) {
        this.source = source;
        this.target = target;
    }

    /**
     * Updates the synapse.
     */
    public void update() {
        // Determine whether to activate short term dynamics
        if (this.getSource() instanceof SpikingNeuron) {
            if (((SpikingNeuron) this.getSource()).hasSpiked()) {
                activated = true;
            } else {
                activated = false;
            }
        } else {
            if (this.getSource().getActivation() > firingThreshold) {
                activated = true;
            } else {
                activated = false;
            }
        }

        if (activated) {
            if (plasticityType == STD) {
                strength -= (bumpRate * (strength - lowerBound));
            } else {
                strength += (bumpRate * (upperBound - strength));
            }
        } else {
            strength -= (decayRate * (strength - baseLineStrength));
        }

        strength = clip(strength);
    }

    /**
     * @return Returns the baseLineStrength.
     */
    public double getBaseLineStrength() {
        return baseLineStrength;
    }

    /**
     * @param baseLineStrength The baseLineStrength to set.
     */
    public void setBaseLineStrength(final double baseLineStrength) {
        this.baseLineStrength = baseLineStrength;
    }

    /**
     * @return Returns the decayRate.
     */
    public double getDecayRate() {
        return decayRate;
    }

    /**
     * @param decayRate The decayRate to set.
     */
    public void setDecayRate(final double decayRate) {
        this.decayRate = decayRate;
    }

    /**
     * @return Returns the growthRate.
     */
    public double getBumpRate() {
        return bumpRate;
    }

    /**
     * @param growthRate The growthRate to set.
     */
    public void setBumpRate(final double growthRate) {
        this.bumpRate = growthRate;
    }

    /**
     * @return Returns the inputThreshold.
     */
    public double getInputThreshold() {
        return inputThreshold;
    }

    /**
     * @param inputThreshold The inputThreshold to set.
     */
    public void setInputThreshold(final double inputThreshold) {
        this.inputThreshold = inputThreshold;
    }

    /**
     * @return Returns the plasticityType.
     */
    public int getPlasticityType() {
        return plasticityType;
    }

    /**
     * @param plasticityType The plasticityType to set.
     */
    public void setPlasticityType(final int plasticityType) {
        this.plasticityType = plasticityType;
    }

    /**
     * @return the firing threshold.
     */
    public double getFiringThreshold() {
        return firingThreshold;
    }

    /**
     * @param firingThreshold The firingThreshold to set.
     */
    public void setFiringThreshold(final double firingThreshold) {
        this.firingThreshold = firingThreshold;
    }
}
