package org.simbrain.custom_sims.simulations.sorn;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.neuron_update_rules.SpikingThresholdRule;
import org.simbrain.network.neuron_update_rules.interfaces.NoisyUpdateRule;
import org.simbrain.network.util.ScalarDataHolder;
import org.simbrain.util.math.ProbDistributions.NormalDistribution;
import org.simbrain.util.math.ProbabilityDistribution;

/**
 * An implementation of the specific type of threshold neuron used in Lazar,
 * Pipa, & Triesch (2009).
 *
 * @author Zoë Tosi
 *
 */
public class SORNNeuronRule extends SpikingThresholdRule implements
        NoisyUpdateRule {

    /** The noise generating randomizer. */
    private ProbabilityDistribution noiseGenerator =
            NormalDistribution.builder()
                    .standardDeviation(0.05).mean(0)
                .build();

    /** Whether or not to add noise to the inputs . */
    private boolean addNoise;

    /** The target rate. */
    private double hIP = 0.01;

    /** The learning rate for homeostatic plasticity. */
    private double etaIP = 0.001;

    /** The maximum value the threshold is allowed to take on. */
    private double maxThreshold = 1;

    private double refractoryPeriod = 0;

    @Override
    public SORNNeuronRule deepCopy() {
        SORNNeuronRule snr = new SORNNeuronRule();
        snr.setAddNoise(addNoise);
        snr.setNoiseGenerator(noiseGenerator);
        snr.setEtaIP(etaIP);
        snr.sethIP(hIP);
        snr.setMaxThreshold(maxThreshold);
        snr.setThreshold(getThreshold());
        snr.setRefractoryPeriod(getRefractoryPeriod());
        return snr;
    }

    @Override
    public void apply(Neuron neuron, ScalarDataHolder data) {
        // Synaptic Normalization
        neuron.normalizeExcitatoryFanIn();
        // Sum inputs including noise and applied (external) inputs
        double input = neuron.getInput()
                + (addNoise ? noiseGenerator.nextDouble() : 0)
                + getAppliedInput();
        // Check that we're not still in the refractory period
        boolean outOfRef = neuron.getNetwork().getTime()
            > getLastSpikeTime()+refractoryPeriod;
        // We fire a spike if input exceeds threshold and we're
        // not in the refractory period
        boolean spk = outOfRef && (input >= getThreshold());
        setHasSpiked(spk, neuron);
        neuron.setSpike(spk);
        neuron.setActivation(2*(input-getThreshold()));
        plasticUpdate(neuron);
    }

    /**
     * Homeostatic plasticity of the default SORN network. {@inheritDoc}
     */
    public void plasticUpdate(Neuron neuron) {
        setThreshold(getThreshold() + (etaIP * ((neuron.isSpike()?1:0) - hIP)));
//        if (getThreshold() > maxThreshold) {
//            setThreshold(maxThreshold);
//        }
    }
    
    public void init(Neuron n) {
        n.normalizeInhibitoryFanIn();
    }

    @Override
    public ProbabilityDistribution getNoiseGenerator() {
        return noiseGenerator;
    }

    @Override
    public void setNoiseGenerator(ProbabilityDistribution rand) {
        noiseGenerator = rand.deepCopy();
    }

    @Override
    public boolean getAddNoise() {
        return addNoise;
    }

    @Override
    public void setAddNoise(boolean noise) {
        this.addNoise = noise;
    }

    public double getMaxThreshold() {
        return maxThreshold;
    }

    public void setMaxThreshold(double maxThreshold) {
        this.maxThreshold = maxThreshold;
    }

    public double gethIP() {
        return hIP;
    }

    public void sethIP(double hIP) {
        this.hIP = hIP;
    }

    public double getEtaIP() {
        return etaIP;
    }

    public void setEtaIP(double etaIP) {
        this.etaIP = etaIP;
    }

    public void setRefractoryPeriod(double refP) {
        this.refractoryPeriod = refP;
    }

    public double getRefractoryPeriod() {
        return refractoryPeriod;
    }

}
