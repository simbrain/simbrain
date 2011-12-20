import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.jgap.*;
import org.jgap.impl.*;
import org.simbrain.network.interfaces.RootNetwork;

/**
 * This class evolves a neural network using the NetworkEvaluationFunction
 * Adapted from a test program written by the creators of JGap.
 * 
 * @author Jeff Yoshimi
 * @author Neil Rotstan
 * @author Klaus Meffert
 * 
 */
public class EvolveNeuralNetwork {
    
    /** Where to write the output of this genetic algorithm. */
    private final static String FILE_OUTPUT_LOCATION = "./";

    /** Sample size. */
    private final static int SAMPLE_SIZE = 20;
    
    /** Number of evolutions to run the evolver. */
    private final static int NUM_EVOLUTIONS = 500;

    /** Maximum fitness. */
    private final static int MAX_FITNESS = 1000;


    /*
     * Runs the evolver.
     */
    public static void main(String[] args) {

        // Sample chromosome, population size, and desired fitness function.
        Configuration defaultConfig = new DefaultConfiguration();
        defaultConfig.setPreservFittestIndividual(true);
        defaultConfig.setKeepPopulationSizeConstant(false);

        // Initialize genotype
        Genotype genotype = null;
        try {

            // Neural network chromosome parameters are specified here.
            //  Each gene has a minimum and maximum value
            Gene[] genes = new Gene[7];
            genes[0] = new IntegerGene(defaultConfig, 20, 100); // Neurons
            genes[1] = new DoubleGene(defaultConfig, 50, 100); // Grid space
            genes[2] = new DoubleGene(defaultConfig, 0, 1); // Inhibitory prob
            genes[3] = new DoubleGene(defaultConfig, 10, 100); // Inhibitory radius
            genes[4] = new DoubleGene(defaultConfig, 0, 1); // Excitatory prob
            genes[5] = new DoubleGene(defaultConfig, 10, 100); // Excitatory radius
            genes[6] = new IntegerGene(defaultConfig, 0, 3); // Neuron Type

            IChromosome sampleChromosome = new Chromosome(defaultConfig, genes);
            defaultConfig.setSampleChromosome(sampleChromosome);
            defaultConfig.setPopulationSize(SAMPLE_SIZE);
            defaultConfig.setFitnessFunction(new NetworkEvaluationFunction());
            genotype = Genotype.randomInitialGenotype(defaultConfig);
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
            System.exit(-2);
        }
        int progress = 0;
        int percentEvolution = NUM_EVOLUTIONS / 100;
        for (int i = 0; i < NUM_EVOLUTIONS; i++) {
            genotype.evolve();
            // Print progress.
            // ---------------
            if (percentEvolution > 0 && i % percentEvolution == 0) {
                progress++;
                IChromosome fittest = genotype.getFittestChromosome();
                double fitness = fittest.getFitnessValue();
                System.out.println("Currently fittest Chromosome has fitness "
                        + fitness);
                if (fitness >= MAX_FITNESS) {
                    break;
                }
            }
        }

        // --------------
        // Print summary.
        // --------------
        IChromosome fittest = genotype.getFittestChromosome();
        System.out.println("Fittest Chromosome has fitness "
                + fittest.getFitnessValue() + "  number of neurons: "
                + ((IntegerGene) fittest.getGene(0)).intValue());

        // -----------------------------
        // Print winning network to file
        // -----------------------------
        RootNetwork network = (RootNetwork) fittest.getApplicationData();
        File theFile = new File(FILE_OUTPUT_LOCATION + "JGapResult.xml");
        try {
            RootNetwork.getXStream().toXML(network,
                    new FileOutputStream(theFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

}