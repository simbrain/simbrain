package org.simbrain.network.neuron_update_rules;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.core.SpikingNeuronUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.NoisyUpdateRule;
import org.simbrain.util.randomizer.Randomizer;

public class AdExIFRule extends SpikingNeuronUpdateRule implements
		NoisyUpdateRule {

	public static final double CURRENT_CONVERTER = 1000;

	/**
	 * Reset voltage (mV). Defaults to 3-spike bursting behavior at .8 nA
	 * current. See Touboul & Brette 2005 -48.5: 2 spike burst -47.2: 4 spike
	 * burst -48: chaotic spike response
	 */
	private double v_Reset = -47.7; //

	/**
	 * Threshold voltage (mV). This determines when a neuron will start a
	 * divergent change in voltage that will tend toward infinity and is not the
	 * voltage at which we consider the neuron to have spiked. External factors
	 * can still cause an action potential to fail even if v_mem > v_Th.
	 */
	private double v_Th = -50.4;

	/**
	 * The peak voltage after which we say with certainty that an action
	 * potential has occurred (mV).
	 */
	private double v_Peak = 20;

	/** Leak Conductance (nS). */
	private double g_L = 30;

	/** Maximal excitatory conductance. (nS) */
	private double g_e_bar = 10;

	/** Maximal inhibitory conductance. (nS) */
	private double g_i_bar = 10;

	/** Leak strength (mV). */
	private double leakReversal = -70.6;

	/** Excitatory reversal. (mV). */
	private double exReversal = 0;

	/** Inhibitory reversal. (mV) */
	private double inReversal = -75;

	/** Membrane potential (mV). */
	private double v_mem = leakReversal;

	/** Adaptation variable. */
	private double w = 200;

	/** Adaptation reset parameter (nA). */
	private double b = 0.0805;

	/** Adaptation time constant (ms). */
	private double tauW = 40;

	/** mV */
	private double slopeFactor = 2;

	/** Adaptation coupling parameter (nS). */
	private double a = 4;

	/** Membrane Capacitance (pico Farads). */
	private double memCapacitance = 281;

	/** Background current being directly injected into the neuron (nA). */
	private double i_bg = 0;

	private boolean addNoise = false;

	private Randomizer noiseGenerator = new Randomizer();

	@Override
	public void update(Neuron neuron) {

		// Retrieve integration time constant in case it has changed...
		double dt = neuron.getNetwork().getTimeStep();

		// Retrieve membrane potential from host neuron's activation
		// in case some outside entity has explicitly changed the membrane
		// potential between updates.
		v_mem = neuron.getActivation();

		// Retrieve incoming ex/in currents or proportion of open channels
		double[] ei = inputType.getNormalizedSeparatedInput(neuron);

		// Calculate incoming excitatory and inhibitory voltage changes
		double iSyn_ex = g_e_bar * ei[0] * (exReversal - v_mem);
		double iSyn_in = -g_i_bar * ei[1] * (inReversal - v_mem);

		// Calculate voltage changes due to leak
		double i_leak = g_L * (leakReversal - v_mem);

		// Calc dV/dt for membrane potential
		double dVdt = (g_L * slopeFactor * Math.exp((v_mem - v_Th)
				/ slopeFactor))
				+ i_leak + iSyn_ex + iSyn_in + i_bg - w;

		// Add noise if there is any to be added
		if (addNoise) {
			dVdt += noiseGenerator.getRandom();
		}

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
			v_mem = v_Reset;
			w = w + (b * CURRENT_CONVERTER);
			neuron.setSpkBuffer(true);
			setHasSpiked(true, neuron);
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
		cpy.increment = this.increment;
		cpy.inputType = this.inputType;
		cpy.leakReversal = this.leakReversal;
		cpy.memCapacitance = this.memCapacitance;
		cpy.noiseGenerator = new Randomizer(noiseGenerator);
		cpy.slopeFactor = this.slopeFactor;
		cpy.tauW = this.tauW;
		cpy.v_mem = this.v_mem;
		cpy.v_Reset = this.v_Reset;
		cpy.v_Th = this.v_Th;
		cpy.w = this.w;
		return cpy;
	}

	@Override
	public String getDescription() {
		return "AdEx Integrate and Fire";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Randomizer getNoiseGenerator() {
		return noiseGenerator;
	}

	@Override
	public void setNoiseGenerator(Randomizer rand) {
		this.noiseGenerator = rand;
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

}
