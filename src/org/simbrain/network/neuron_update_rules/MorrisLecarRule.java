package org.simbrain.network.neuron_update_rules;

/**
 * 
 * @author Zach Tosi
 */
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.core.SpikingNeuronUpdateRule;
import org.simbrain.network.neuron_update_rules.interfaces.NoisyUpdateRule;
import org.simbrain.util.math.ProbDistribution;
import org.simbrain.util.randomizer.Randomizer;

public class MorrisLecarRule extends SpikingNeuronUpdateRule
	implements NoisyUpdateRule {

	/** Calcium channel conductance (micro Siemens/cm^2). */
	private double g_Ca = 4.0;
	
	/** Potassium channel conductance (micro Siemens/cm^2). */
	private double g_K = 8.0;
	
	/** Leak conductance (micro Siemens/cm^2). */
	private double g_L = 2.0;
	
	/** Resting potential calcium (mV). */
	private double vRest_Ca = 120;
	
	/** Resting potential potassium (mV). */
	private double vRest_k = -80;
	
	/** Resting potential for leak current (mV). */
	private double vRest_L = -60;
	
	/** Membrane capacitance per unit area (micro Farads/cm^2). */
	private double cMembrane = 5;
	
	/** Membrane voltage constant 1. */
	private double v_m1 = -1.2;
	
	/** Membrane voltage constant 2. */
	private double v_m2 = 18;
	
	/** Potassium channel constant 1. */
	private double v_w1 = 2;
	
	/** Potassium channel constant 2. */
	private double v_w2 = 17.4;
	
	/** Fraction of open potassium channels. */
	private double w_K;
	
	/** Potassium channel time constant/decay rate (s^-1). */
	private double phi = 0.06667; // 1/15
	
	/** Background current (nA). */
	private double i_bg = 46;
	
	/** Threshold for neurotransmitter release (mV) */
	private double threshold = 40;
	
	/** A source of noise (nA). */
	private Randomizer noiseGenerator = new Randomizer();
	
	{
		noiseGenerator.setPdf(ProbDistribution.NORMAL);
		noiseGenerator.setParam2(1);
	}
	
	@Override
	public void update(Neuron neuron) {
		double dt = neuron.getNetwork().getTimeStep();
		double i_syn = inputType.getInput(neuron);
		// Under normal circumstances this will cause no change.
		double vMembrane = neuron.getActivation();
		
		double dVdt = dVdt(vMembrane, i_syn);
		double dWdt = dWdt(vMembrane, w_K);
		
		double vmFut = vMembrane + dt * dVdt;
		double wKFut = w_K + dt * dWdt;
		vMembrane = vMembrane + (dt/2) * ((dVdt) + dVdt(vmFut, i_syn));
		w_K = w_K + (dt/2) * ((dWdt) + dWdt(vMembrane, wKFut));
		
		neuron.setSpkBuffer(vMembrane > threshold);
		setHasSpiked(vMembrane > threshold, neuron);

		neuron.setBuffer(vMembrane);
		
	}
	
	private double dVdt(double vMembrane, double i_syn) {
		double i_Ca = g_Ca * membraneFunction(vMembrane)
				* (vMembrane - vRest_Ca);
		double i_K = g_K * w_K * (vMembrane - vRest_k);
		double i_L = g_L * (vMembrane - vRest_L);
		double i_ion = i_Ca + i_K + i_L;
		double i_noise = 0;
		if (getAddNoise()) {
		    i_noise = noiseGenerator.getRandom();
		}
		return ((i_bg - i_ion + i_syn + i_noise) / cMembrane);
	}
	
	private double dWdt(double vMembrane, double w_K) {
		return phi * lambdaFunction(vMembrane)
				* (k_fractionFunction(vMembrane) - w_K);
	}
	
	private double membraneFunction(double vMembrane) {
		return 0.5 * (1 + Math.tanh((vMembrane - v_m1) / v_m2));
	}

	private double k_fractionFunction(double vMembrane) {
		return 0.5 * (1 + Math.tanh((vMembrane - v_w1) / v_w2));
	}
	
	private double lambdaFunction(double vMembrane) {
		return Math.cosh((vMembrane - v_w1) / (2 * v_w2));
	}
	
	@Override
	public NeuronUpdateRule deepCopy() {
		MorrisLecarRule cpy = new MorrisLecarRule();
		cpy.setAddNoise(this.getAddNoise());
		cpy.g_Ca = this.g_Ca;
		cpy.g_K = this.g_K;
		cpy.cMembrane = this.cMembrane;
		cpy.g_L = this.g_L;
		cpy.i_bg = this.i_bg;
		cpy.phi = this.phi;
		cpy.v_m1 = this.v_m1;
		cpy.v_m2 = this.v_m2;
		cpy.v_w1 = this.v_w1;
		cpy.v_w2 = this.v_w2;
		cpy.threshold = this.threshold;
		cpy.vRest_Ca = this.vRest_Ca;
		cpy.vRest_k = this.vRest_k;
		cpy.vRest_L = this.vRest_L;
		cpy.w_K = this.w_K;
		cpy.noiseGenerator = new Randomizer(this.noiseGenerator);

		return cpy;
	}

	@Override
	public String getDescription() {
		return "Morris-Lecar";
	}

	@Override
	public Randomizer getNoiseGenerator() {
		return new Randomizer(noiseGenerator);
	}

	@Override
	public void setNoiseGenerator(Randomizer rand) {
		// Does Nothing
	}

	public void setNoiseAmplitude(double amp) {
		noiseGenerator.setParam2(amp);
	}
	
	@Override
	public boolean getAddNoise() {
		return true;
	}

	@Override
	public void setAddNoise(boolean noise) {
		// Does nothing
	}

	public double getG_Ca() {
		return g_Ca;
	}

	public void setG_Ca(double g_Ca) {
		this.g_Ca = g_Ca;
	}

	public double getG_K() {
		return g_K;
	}

	public void setG_K(double g_K) {
		this.g_K = g_K;
	}

	public double getG_L() {
		return g_L;
	}

	public void setG_L(double g_L) {
		this.g_L = g_L;
	}

	public double getvRest_Ca() {
		return vRest_Ca;
	}

	public void setvRest_Ca(double vRest_Ca) {
		this.vRest_Ca = vRest_Ca;
	}

	public double getvRest_k() {
		return vRest_k;
	}

	public void setvRest_k(double vRest_k) {
		this.vRest_k = vRest_k;
	}

	public double getvRest_L() {
		return vRest_L;
	}

	public void setvRest_L(double vRest_L) {
		this.vRest_L = vRest_L;
	}

	public double getcMembrane() {
		return cMembrane;
	}

	public void setcMembrane(double cMembrane) {
		this.cMembrane = cMembrane;
	}

	public double getV_m1() {
		return v_m1;
	}

	public void setV_m1(double v_m1) {
		this.v_m1 = v_m1;
	}

	public double getV_m2() {
		return v_m2;
	}

	public void setV_m2(double v_m2) {
		this.v_m2 = v_m2;
	}

	public double getV_w1() {
		return v_w1;
	}

	public void setV_w1(double v_w1) {
		this.v_w1 = v_w1;
	}

	public double getV_w2() {
		return v_w2;
	}

	public void setV_w2(double v_w2) {
		this.v_w2 = v_w2;
	}

	public double getPhi() {
		return phi;
	}

	public void setPhi(double phi) {
		this.phi = phi;
	}

	public double getI_bg() {
		return i_bg;
	}

	public void setI_bg(double i_bg) {
		this.i_bg = i_bg;
	}

	public double getThreshold() {
		return threshold;
	}

	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

}
