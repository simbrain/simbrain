import org.simbrain.network.core.*;
import org.simbrain.network.neuron_update_rules.interfaces.*;
import org.simbrain.network.neuron_update_rules.*;
import org.simbrain.util.randomizer.*;
import org.simbrain.util.math.*;

/**
 * An implementation of the specific type of threshold neuron used in Lazar,
 * Pipa, & Triesch (2009).
 *
 * @author Zoë Tosi
 *
 */
public class SORNNeuronRule extends SpikingThresholdRule implements
        NoisyUpdateRule {

    {
        inputType = InputType.SYNAPTIC;
    }

    /** The noise generating randomizer. */
    private Randomizer noiseGenerator = new Randomizer();

    {
        noiseGenerator.setPdf(ProbDistribution.NORMAL);
        noiseGenerator.setParam2(0.05);
    }

    /** Whether or not to add noise to the inputs . */
    private boolean addNoise;

    /** The target rate. */
    private double hIP = 0.1;

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
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void update(Neuron neuron) {
        // Synaptic Normalization
        neuron.normalizeExcitatoryFanIn();
        // Sum inputs including noise and applied (external) inputs
        double input = inputType.getInput(neuron)
                + (addNoise ? noiseGenerator.getRandom() : 0)
                + getAppliedInput();
        // Check that we're not still in the refractory period
        boolean outOfRef = neuron.getNetwork().getTime()
            > getLastSpikeTime()+refractoryPeriod;
        // We fire a spike if input exceeds threshold and we're
        // not in the refractory period
        boolean spk = outOfRef && (input >= getThreshold());
        setHasSpiked(spk, neuron);
        neuron.setSpkBuffer(spk);
        neuron.setBuffer(2*(input-getThreshold()));
        plasticUpdate(neuron);
    }

    /**
     * Homeostatic plasticity of the default SORN network. {@inheritDoc}
     */
    public void plasticUpdate(Neuron neuron) {
        setThreshold(getThreshold() + (etaIP * ((neuron.isSpike()?1:0) - hIP)));
        if (getThreshold() > maxThreshold) {
            setThreshold(maxThreshold);
        }
    }
    
    public void init(Neuron n) {
        n.normalizeInhibitoryFanIn();
    }

    @Override
    public Randomizer getNoiseGenerator() {
        return noiseGenerator;
    }

    @Override
    public void setNoiseGenerator(Randomizer rand) {
        noiseGenerator = new Randomizer(rand);
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
