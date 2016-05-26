import org.simbrain.network.core.*;
import org.simbrain.network.neuron_update_rules.interfaces.*;
import org.simbrain.network.neuron_update_rules.*;
import org.simbrain.util.randomizer.*;

/**
 * An implementation of the specific type of threshold neuron used in Lazar,
 * Pipa, & Triesch (2009).
 *
 * @author Zach Tosi
 *
 */
public class SORNNeuronRule extends SpikingThresholdRule implements
        NoisyUpdateRule {

    {
        inputType = InputType.WEIGHTED;
    }

    /** The noise generating randomizer. */
    private Randomizer noiseGenerator = new Randomizer();

    /** Whether or not to add noise to the inputs . */
    private boolean addNoise;

    /** The target rate. */
    private double hIP = 0.1;

    /** The learning rate for homeostatic plasticity. */
    private double etaIP = 0.001;

    /** The maximum value the threshold is allowed to take on. */
    private double maxThreshold = 1;
    
    private double threshold;

    @Override
    public SORNNeuronRule deepCopy() {
        SORNNeuronRule snr = new SORNNeuronRule();
        snr.setAddNoise(addNoise);
        snr.setNoiseGenerator(noiseGenerator);
        snr.setEtaIP(etaIP);
        snr.sethIP(hIP);
        snr.setMaxThreshold(maxThreshold);
        snr.setThreshold(threshold);
        return snr;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void update(Neuron neuron) {
        neuron.normalizeExcitatoryFanIn();
        double input = inputType.getInput(neuron)
                + (addNoise ? noiseGenerator.getRandom() : 0)
                + getAppliedInput();
        boolean spk = input >= threshold;
        // While technically not a spiking update rule this gives us
        // flexibility. For instance we can use the much more compressed spike
        // time output file for recording.
        neuron.setSpkBuffer(spk);
        neuron.setBuffer(spk ? 1 : 0);
        plasticUpdate(neuron);
    }

    /**
     * Homeostatic plasticity of the default SORN network. {@inheritDoc}
     */
    public void plasticUpdate(Neuron neuron) {
        threshold += etaIP * (neuron.getActivation() - hIP);
        if (threshold > maxThreshold) {
            threshold = maxThreshold;
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

}
