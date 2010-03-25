import org.jgap.FitnessFunction;
import org.jgap.IChromosome;
import org.jgap.impl.DoubleGene;
import org.jgap.impl.IntegerGene;
import org.simbrain.network.connections.Radial;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.neurons.BinaryNeuron;
import org.simbrain.network.neurons.DecayNeuron;
import org.simbrain.network.neurons.LinearNeuron;
import org.simbrain.network.neurons.NakaRushtonNeuron;
import org.simbrain.network.neurons.SigmoidalNeuron;

/**
 * This class represents a neural network as a chromosome.
 * 
 * Various aspects of this method can be customized to evolve custom
 * networks.Several options for the specific fitness function computed are given
 * below, at the end of the evaluate function.
 * 
 * @author Jeff Yoshimi
 * 
 */
public class NetworkEvaluationFunction extends FitnessFunction {

    //TODO: Base Synapse Types for inhib and excit...

    @Override
    protected double evaluate(IChromosome chromosome) {

        RootNetwork network = new RootNetwork();
        chromosome.setApplicationData(network);
        
        // The basic description of the network using a chromosome
        // Currently the emphasis is on networks with radial connections
        int numNeurons = ((IntegerGene ) chromosome.getGene(0)).intValue();
        double gridSpace  = ((DoubleGene ) chromosome.getGene(1)).doubleValue();
        double inhibProb = ((DoubleGene ) chromosome.getGene(2)).doubleValue();;
        double inhibRadius = ((DoubleGene ) chromosome.getGene(3)).doubleValue();;
        double excitProb = ((DoubleGene ) chromosome.getGene(4)).doubleValue();;
        double excitRadius = ((DoubleGene ) chromosome.getGene(5)).doubleValue();;
        int neuronType = ((IntegerGene ) chromosome.getGene(6)).intValue();
        
        // Build the network based on the current chromosome
        for (int i = 0; i < numNeurons; i++) {
            Neuron neuron = getNeuron(neuronType);
            network.addNeuron(neuron);
        }
        GridLayout layout = new GridLayout(gridSpace, gridSpace, (int) Math.sqrt(numNeurons));
        layout.layoutNeurons(network);
        Radial connection = new Radial(network, network.getFlatNeuronList(),
                network.getFlatNeuronList());
        connection.setExcitatoryProbability(excitProb);
        connection.setInhibitoryProbability(inhibProb);
        connection.setExcitatoryRadius(excitRadius);
        connection.setInhibitoryRadius(inhibRadius);
        connection.connectNeurons();

        // Currently, the default behavior is to randomize the neurons of a
        // network and update it for a set number of iterations.
        network.randomizeNeurons();
        for (int i = 0; i < 50; i++) {
            network.updateRootNetwork();
        }

        // ---------------------------------------------------------------
        // Comment / uncomment / modify code below to change the specific
        // fitness function implemented.
        // ---------------------------------------------------------------

        //return howCloseToValue(network, 150);  
        //return percentActive(network);
        //return getAverageValue(network);
        return percentActive(network, .85);
    }
    
    /**
     * Helper method which returns a neuron type based on an integer value.
     * More options can obviously be added here.
     *
     * @param neuronType integer value
     * @return a neuron of the corresponding type
     */
    private Neuron getNeuron(int neuronType) {
        
        switch(neuronType) {
            case 0: 
                return new BinaryNeuron();
            case 1:
                return new LinearNeuron();
            case 2: 
                return new SigmoidalNeuron();
            case 3: 
                return new DecayNeuron();
            default:
                return new NakaRushtonNeuron();
        }
    }
    
    

    /**
     * Use this method to evolve a network for which a specified 
     * percentage of neurons will be active.
     * 
     * @param network network reference
     * @param target target percentage
     * @return how close the given network is to the specified percentage
     */
    private double percentActive(RootNetwork network, double target) {
        return 1 - Math.abs(percentActive(network) - target);
    }

    /**
     * Helper method. Determines the percentage of neurons in a network active,
     * where "active" means activation greater than 1/2 * that neuron's upper
     * bound.
     * 
     * Takes average over several iterations, to avoid solutions that oscillate.
     * 
     * @param network reference
     * @return percent active
     */
    private double percentActive(RootNetwork network) {
        
        int numNeurons = network.getFlatNeuronList().size();
        int numActive = 0;
        int numIterations = 2;
        
        for (int iterations = 0; iterations < numIterations; iterations++) {
            for (Neuron neuron : network.getFlatNeuronList()) {
                if (neuron.getActivation() > neuron.getUpperBound() / 2) {
                   //System.out.println(neuron.getId() + ":" + neuron.getActivation() + "/" + neuron.getUpperBound());
                    numActive++;
                }
            }
            network.update();
        }

        double percentActive = (double) numActive / (double) numNeurons /  numIterations;
        return percentActive;
    }
    
    /**
     * Use this method to evolve a network whose average activity is as close as possible to a specified value.
     * @param network reference to network
     * @param value target average activity
     * @return how close the network's average value is to the target value.
     */
    private double howCloseToValue(RootNetwork network, double value) {

        // A value between 0 and something..
        double distanceToValue = Math.abs(getAverageValue(network) - value); 
        return 1000 - distanceToValue;
    }

    /**
     * Helper method which returns the average activity of a network over a time
     * window (specified by numIterations). Return only values greater than 0.
     * 
     * Takes average over several iterations, to avoid solutions that oscillate.
     * 
     * @param network reference to network
     * @return average value over specified window.
     */
    private double getAverageValue(RootNetwork network) {
        double total = 0;
        int numIterations = 2;
        
        for (int iterations = 0; iterations < numIterations; iterations++) {
            for (Neuron neuron : network.getFlatNeuronList()) {
                total += neuron.getActivation();
            }
            network.update();
        }
        double averageVal = total / network.getFlatNeuronList().size() / numIterations;
        System.out.println("average value:" + averageVal);
        return Math.max(0, averageVal);
    }

}
