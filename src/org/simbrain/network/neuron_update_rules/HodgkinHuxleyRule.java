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

import org.simbrain.network.core.Network.TimeType;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.NoisyUpdateRule;
import org.simbrain.util.UserParameter;
import org.simbrain.util.math.ProbDistributions.UniformDistribution;
import org.simbrain.util.math.ProbabilityDistribution;

// TODO: deal with ENa, EK
/**
 * Hodgkin-Huxley Neuron.
 * <p>
 * Adapted from software written by Anthony Fodor, with help from Jonathan
 * Vickrey.
 */
public class HodgkinHuxleyRule extends NeuronUpdateRule implements NoisyUpdateRule {

    /**
     * Sodium Channels
     */
    @UserParameter(
            label = "Sodium Channels",
            description = "Sodium Channels",
            defaultValue = "100", order = 1)
    private float perNaChannels = 100f;

    /**
     * Potassium
     */
    @UserParameter(
            label = "Potassium Channels",
            description = "Sodium Channels",
            defaultValue = "100", order = 2)
    private float perKChannels = 100f;

    /**
     * Resting Membrane Potential
     */
    private double resting_v = 65;

    /** */
    private double dv;

    /**
     * Membrane Capacitance
     */
    private double cm;

    /**
     * Constant leak permeabilities
     */
    private double gk, gna, gl;

    /**
     * voltage-dependent gating parameters
     */
    private double n, m, h;

    /**
     * corresponding deltas
     */
    private double dn, dm, dh;

    /**
     * // rate constants
     */
    private double an, bn, am, bm, ah, bh;

    /**
     * Ek-Er, Ena - Er, Eleak - Er
     */
    private double vk, vna, vl;

    /** */
    private double n4;

    /** */
    private double m3h;

    /**
     * Sodium current
     */
    private double na_current;

    /**
     * Potassium current
     */
    private double k_current;

    /** */
    private double temp = 0;

    /** */
    private boolean vClampOn = false;

    /** */
    float vClampValue = convertV(0F);

    /**
     * Noise generator.
     */
    private ProbabilityDistribution noiseGenerator = UniformDistribution.create();

    /**
     * Add noise to the neuron.
     */
    private boolean addNoise = false;

    @Override
    public void update(Neuron neuron) {

        // Advances the model by dt and returns the new voltage

        double v = neuron.getInput();
        bh = 1 / (Math.exp((v + 30) / 10) + 1);
        ah = 0.07 * Math.exp(v / 20);
        dh = (ah * (1 - h) - bh * h) * neuron.getNetwork().getTimeStep();
        bm = 4 * Math.exp(v / 18);
        am = 0.1 * (v + 25) / (Math.exp((v + 25) / 10) - 1);
        bn = 0.125 * Math.exp(v / 80);
        an = 0.01 * (v + 10) / (Math.exp((v + 10) / 10) - 1);
        dm = (am * (1 - m) - bm * m) * neuron.getNetwork().getTimeStep();
        dn = (an * (1 - n) - bn * n) * neuron.getNetwork().getTimeStep();

        n4 = n * n * n * n;
        m3h = m * m * m * h;

        na_current = gna * m3h * (v - vna);
        k_current = gk * n4 * (v - vk);

        dv = -1 * neuron.getNetwork().getTimeStep() * (k_current + na_current + gl * (v - vl)) / cm;

        neuron.setBuffer(-1 * (v + dv + resting_v));
        h += dh;
        m += dm;
        n += dn;

        // if (vClampOn)
        // v = vClampValue;

        // getV() converts the model's v to present day convention

    }

    // Initializer quickly hacked from old init. Zoë this is in your hands to fix! :)
    {
        cm = 1.0;
        double v = -70; // Arbitrary starting voltage
        double dv = .001; // Arbitrary starting dv.  Not sure how to set.
        vna = -115;
        vk = 12;
        vl = -10.613;
        gna = perNaChannels * 120 / 100;
        gk = perKChannels * 36 / 100;
        gl = 0.3;

        bh = 1 / (Math.exp((v + 30) / 10) + 1);
        ah = 0.07 * Math.exp(v / 20);
        bm = 4 * Math.exp(v / 18);
        am = 0.1 * (v + 25) / (Math.exp((v + 25) / 10) - 1);
        bn = 0.125 * Math.exp(v / 80);
        an = 0.01 * (v + 10) / (Math.exp((v + 10) / 10) - 1);
        dh = (ah * (1 - h) - bh * h) * dv;
        dm = (am * (1 - m) - bm * m) * dv;
        dn = (an * (1 - n) - bn * n) * dv;

        // start these parameters in steady state
        n = an / (an + bn);
        m = am / (am + bm);
        h = ah / (ah + bh);

    }

    /**
     * {@inheritDoc}
     */
    public TimeType getTimeType() {
        return TimeType.CONTINUOUS;
    }

    public double get_n4() {
        return n4;
    }

    public double get_m3h() {
        return m3h;
    }

    public synchronized float getEna() {
        return (float) (-1 * (vna + resting_v));
    }

    public synchronized float getEk() {
        return (float) (-1 * (vk + resting_v));
    }

    public synchronized void setEna(float Ena) {
        vna = -1 * Ena - resting_v;
    }

    public synchronized void setEk(float Ek) {
        vk = -1 * Ek - resting_v;
    }

    // The -1 is to correct for the fact that in the H & H paper, the currents
    // are reversed.
    public double get_na_current() {
        return -1 * na_current;
    }

    public double get_k_current() {
        return -1 * k_current;
    }

    // negative values set to zero
    public synchronized void setPerNaChannels(float perNaChannels) {
        if (perNaChannels < 0) {
            perNaChannels = 0;
        }
        this.perNaChannels = perNaChannels;
        gna = 120 * perNaChannels / 100;
    }

    public float getPerNaChannels() {
        return perNaChannels;
    }

    public synchronized void setPerKChannels(float perKChannels) {
        if (perKChannels < 0) {
            perKChannels = 0;
        }
        this.perKChannels = perKChannels;
        gk = 36 * perKChannels / 100;
    }

    public float getPerKChannels() {
        return perKChannels;
    }

    // remember that H&H voltages are -1 * present convention
    // TODO: should eventually calculate this instead of setting it

    // convert between internal use of V and the user's expectations
    // the V will be membrane voltage using present day conventions
    // see p. 505 of Hodgkin & Huxley, J Physiol. 1952, 117:500-544

    public void setCm(double inCm) {
        cm = inCm;
    }

    public double getCm() {
        return cm;
    }

    public double getN() {
        return n;
    }

    public double getM() {
        return m;
    }

    public double getH() {
        return h;
    }

    /**
     * Converts a voltage from the modern convention to the convention used by
     * the program.
     *
     * @param voltage
     * @return
     */
    public float convertV(float voltage) {
        return (float) (-1 * voltage - resting_v);
    }

    public boolean getVClampOn() {
        return vClampOn;
    }

    public void setVClampOn(boolean vClampOn) {
        this.vClampOn = vClampOn;
    }

    float get_vClampValue() {
        return (float) (-1 * (vClampValue + resting_v));
    }

    void set_vClampValue(float vClampValue) {
        this.vClampValue = convertV(vClampValue);
    }

    public double getTemp() {
        return temp;
    }

    public void setTemp(double temp) {
        this.temp = temp;
    }

    @Override
    public NeuronUpdateRule deepCopy() {
        HodgkinHuxleyRule hhr = new HodgkinHuxleyRule();
        hhr.set_vClampValue(this.get_vClampValue());
        hhr.setAddNoise(this.getAddNoise());
        hhr.setCm(this.getCm());
        hhr.setEk(this.getEk());
        hhr.setEna(this.getEna());
        hhr.setNoiseGenerator(this.getNoiseGenerator());
        hhr.setPerKChannels(this.getPerKChannels());
        hhr.setPerNaChannels(this.getPerNaChannels());
        hhr.setTemp(this.getTemp());
        hhr.setVClampOn(this.getVClampOn());
        return hhr;
    }

    @Override
    public String getName() {
        return "Hodgkin-Huxley";
    }

    @Override
    public ProbabilityDistribution getNoiseGenerator() {
        return noiseGenerator;
    }

    @Override
    public void setNoiseGenerator(ProbabilityDistribution rand) {
        noiseGenerator = rand;
    }

    @Override
    public boolean getAddNoise() {
        return addNoise;
    }

    @Override
    public void setAddNoise(boolean noise) {
        this.addNoise = noise;
    }

}
