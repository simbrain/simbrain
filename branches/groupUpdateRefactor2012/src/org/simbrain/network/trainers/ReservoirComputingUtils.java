package org.simbrain.network.trainers;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.groups.subnetworks.EchoStateNetwork;
import org.simbrain.network.interfaces.BiasedNeuron;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.neurons.ClampedNeuron;
import org.simbrain.network.neurons.LinearNeuron;
import org.simbrain.network.neurons.SigmoidalNeuron;
import org.simbrain.network.synapses.ClampedSynapse;
import org.simbrain.network.trainers.LMSOffline.SolutionType;

public class ReservoirComputingUtils {
    // TODO: These methods may not work if more complex network updating is
    // required when setting the reservoir states.
    // TODO: Functions for setting trained ESN weights may not function
    // properly for recurrent output weights
	// TODO: These methods and variables should be made dynamic 
    
    
    private static boolean noise;
    private static double noiseMax;
    private static double noiseMin;
    private static double percentComplete;
    private static PropertyChangeListener stateListener;
    
    //TODO: Generalize
    private static EchoStateNetwork esn;
    
    /**
     * Create data to be used in training a reservoir based network, as follows:
     * 1) Iterate through each row of the input data and use it to set the input
     * nodes 2) Update the reservoir 3) Concatenate the input and reservoir
     * states in to one state, one row of the return matrix.
     *
     * @param network root network
     * @param inputNeurons input neurons
     * @param inputData data for input neurons
     * @param reservoirNeurons reservoir neurons
     * @return a matrix of data to be used in training. Each row is an input +
     *         reservoir state.
     */
    public static double[][] generateCombinedInputReservoirData(
            RootNetwork network, List<Neuron> inputNeurons,
            double [][] inputData, List<Neuron> reservoirNeurons) {
 
        // / Return matrix
        double[][] returnMatrix = new double[inputData.length][inputNeurons
                .size() + reservoirNeurons.size()];

        // Iterate through each row of the input data
        for (int row = 0; row < inputData.length; row++) {

            // Clamp the input nodes and add their values to the current row of
            // the return matrix
            int col = 0;
            for (Neuron neuron : inputNeurons) {
                double clampValue = inputData[row][col];
                neuron.setActivation(clampValue);
                returnMatrix[row][col] = neuron.getActivation();
                col++;
            }
            
            // Update reservoir and add the resulting values to the
            // return matrix
            network.updateNeurons(reservoirNeurons);
            for (Neuron neuron : reservoirNeurons) {
                returnMatrix[row][col] = neuron.getActivation();
                col++;
            }

            // TODO?: Teacher-forcing recurrent output connections
        }

        //System.out.println(Utils.doubleMatrixToString(returnMatrix));

        return returnMatrix;

    }

    /**
     * Create data to be used in training a reservoir based network, as follows:
     * 1) Iterate through each row of the input data and use it to set the input
     * nodes 2) Update the reservoir 3) Add the reservoir state as one row of
     * the return matrix.
     *
     * @param network root network
     * @param inputNeurons input neurons
     * @param inputData data for input neurons
     * @param reservoirNeurons reservoir neurons
     * @return a matrix of data to be used in training. Each row is an input +
     *         reservoir state.
     */
    public static double[][] generateReservoirData(RootNetwork network,
            List<Neuron> inputNeurons, double[][] inputData,
            List<Neuron> reservoirNeurons) {

        // / Return matrix
        double[][] returnMatrix = new double[inputData.length][reservoirNeurons
                .size()];

        // Iterate through each row of the input data
        for (int row = 0; row < inputData.length; row++) {

            // Clamp the input nodes using the input data.
            int col = 0;
            for (Neuron neuron : inputNeurons) {
                double clampValue = inputData[row][col];
                neuron.setActivation(clampValue);
                col++;
            }

            // Update reservoir nodes and add the resulting values to the
            // return matrix
            col = 0;
            network.updateNeurons(reservoirNeurons);
            for (Neuron neuron : reservoirNeurons) {
                returnMatrix[row][col] = neuron.getActivation();
                col++;
            }

        }

        //System.out.println(Utils.doubleMatrixToString(returnMatrix));

        return returnMatrix;

    }

    /**
     * A general method for harvesting state data for an arbitrary Echo-State
     * Network. This method iterates through each row of input and teacher data
     * (if the network possesses back weights and/or recurrent output weights),
     * and updates the reservoir. Depending on the ESN's un-frozen connectivity
     * the resulting return matrix will have rows consisting of concatenated
     * input, reservoir, and (teacher-forced) output states in that order.
     * 
     * @param esn
     *            The ESN builder from which parameters for state harvesting are
     *            inferred
     * @param inputData
     *            data input to the ESN
     * @param teacherData
     *            training data used for teacher-forcing if required
     * @return a matrix of data to be used for offline training
     */
    public static double[][] generateData(double[][] inputData, double[][] teacherData) {
        // The minimum number of state matrix columns
        int columnNumber = esn.getNumReservoirNodes();

        if (esn.hasDirectInOutWeights()) {
            //Add columns for the input layer states
            columnNumber += esn.getNumInputNodes();
        }
        if (esn.hasRecurrentOutWeights()) {
            //Add columns for output layer states
            columnNumber += esn.getNumOutputNodes();
        }
        //State matrix
        double[][] returnMatrix = new double[inputData.length][columnNumber];

        //Iterate through each row of input data
        for (int row = 0; row < inputData.length; row++) {

            int col = 0;

            //Clamp input neurons based on input data
            for (Neuron neuron : esn.getInputLayer().getNeuronList()) {
                double clampValue = inputData[row][col];
                neuron.setActivation(clampValue);
                if (esn.hasDirectInOutWeights()) {
                    //Add input states to state matrix if direct in to out
                    //connections are desired
                    returnMatrix[row][col] = neuron.getActivation();
                    col++;
                }

            }

            
            if (esn.hasBackWeights()) {
                int count = 0;
                double clampValue = 0.5;
                for (Neuron neuron : esn.getOutputLayer().getNeuronList()) {
                    // Teacher forcing
                    if (row > 0) {
                        clampValue = teacherData[row - 1][count];
                    }
                    neuron.setActivation(clampValue);
                    count++;
                }
            }

            //Update the reservoir: handles teacher-forced back-weights
            for (Neuron n : esn.getReservoirLayer().getNeuronList()) {
                n.update();
            }
            for (Neuron n : esn.getReservoirLayer().getNeuronList()) {
                double val = n.getBuffer();
                if (noise) {
                    n.setActivation(val + reservoirNoise());
                } else {
                    n.setActivation(val);
                }
                returnMatrix[row][col] = n.getActivation();
                col++;
            }

            //Add output states to state matrix if there are recurrent outputs
            if (esn.hasRecurrentOutWeights()) {
                for (int i = 0; i < teacherData[0].length; i++) {
                    //Teacher-forcing
                    returnMatrix[row][col] = teacherData[row][i];
                    col++;
                }
            }
            
            firePropertyChange((double) row/inputData.length);
            setPercentComplete((double) row/inputData.length); 

        }

        return returnMatrix;
    }


    public static void setStateListener(PropertyChangeListener pcl) {
    	stateListener = pcl;
    }
    
    private static void firePropertyChange(double percentComplete){
    	stateListener.propertyChange(new PropertyChangeEvent (
    			ReservoirComputingUtils.class, "State Harvesting", 
    			getPercentComplete(), percentComplete));
    }
    
    private static class StateHarvestEvent extends PropertyChangeEvent{

		public StateHarvestEvent(Object source, String propertyName,
				Object oldValue, Object newValue) {
			super(source, propertyName, oldValue, newValue);
			// TODO Auto-generated constructor stub
		}



    }
    
    /**
     *  Adds noise to the reservoir's part of the state matrix.
     * @return noise
     */
    public static double reservoirNoise(){
        return (noiseMax - noiseMin) * Math.random() + noiseMin;
    }

    /**
     * Test methods.
     *
     * @param args not used
     */
    public static void main(String[] args) {

        RootNetwork network = testReservoirNetwork();
        System.out.println(network);

        // Write to file
        String FILE_OUTPUT_LOCATION = "./";
        File theFile = new File(FILE_OUTPUT_LOCATION + "result.xml");
        try {
            RootNetwork.getXStream().toXML(network,
                    new FileOutputStream(theFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }


    /**
     * Train a simple reservoir network using the data generator and LMSOffline.
     *
     * @return trained root network
     */
    private static RootNetwork testReservoirNetwork() {

        RootNetwork network = new RootNetwork();

        // "Input" data, to be expanded to use hidden layer units as well
        double clampData[][] = { { .95, 0, 0, 0 }, { 0, .95, 0, 0 },
                { 0, 0, .95, 0 }, { 0, 0, 0, .95 } };

        // Training data
        double trainingData[][] = { { .95, 0 }, { .95, 0 }, { 0, .95 },
                { 0, .95 } };

        // Set up clamped layer
        List<Neuron> clampedLayer = new ArrayList<Neuron>();
        for (int i = 0; i < 4; i++) {
            Neuron neuron = new Neuron(network, new ClampedNeuron());
            neuron.setLocation(10 + (i * 40), 70);
            neuron.setIncrement(1);
            network.addNeuron(neuron);
            clampedLayer.add(neuron);
            // System.out.println("Input " + i + " = " + neuron.getId());
        }

        // Set up hidden layer
        List<Neuron> hiddenLayer = new ArrayList<Neuron>();
        for (int i = 0; i < 2; i++) {
            Neuron neuron = new Neuron(network, new LinearNeuron());
            neuron.setLocation(200 + (i * 40), 35);
            neuron.setIncrement(1);
            network.addNeuron(neuron);
            hiddenLayer.add(neuron);
            // System.out.println("Input " + i + " = " + neuron.getId());
        }

        // Set up output layer
        List<Neuron> outputLayer = new ArrayList<Neuron>();
        for (int i = 0; i < 2; i++) {
            Neuron neuron = new Neuron(network, new LinearNeuron());
            ((BiasedNeuron) neuron.getUpdateRule()).setBias(0);
            neuron.setLocation(15 + (i * 40), 0);
            neuron.setLowerBound(0);
            neuron.setUpperBound(1);
            network.addNeuron(neuron);
            // System.out.println("Output " + i + " = " + neuron.getId());
            outputLayer.add(neuron);
        }

        // Connect clamped to hidden layer
        AllToAll connection = new AllToAll(network, clampedLayer, outputLayer);
        connection.connectNeurons();

        // Connect clamped to output layer
        AllToAll connection2 = new AllToAll(network, clampedLayer, hiddenLayer);
        connection2.connectNeurons();

        // Connect hidden to output layer
        AllToAll connection3 = new AllToAll(network, hiddenLayer, outputLayer);
        connection3.connectNeurons();

        // Randomize synapses
        network.randomizeWeights();

        // Create input data for trainer
        double[][] inputData = ReservoirComputingUtils
                .generateCombinedInputReservoirData(network, clampedLayer,
                        clampData, hiddenLayer);

        // Create input layer for trainer (clamped + hidden)
        List<Neuron> inputLayer = new ArrayList<Neuron>();
        for (Neuron neuron : clampedLayer) {
            inputLayer.add(neuron);
        }
        for (Neuron neuron : hiddenLayer) {
            inputLayer.add(neuron);
        }

        // Initialize the trainer (comment / uncomment below for different
        // configurations)
        Trainer trainer = new Trainer(network, inputLayer, outputLayer, new LMSOffline());
        trainer.setInputData(inputData);
        trainer.setTrainingData(trainingData);
        ((LMSOffline) trainer.getTrainingMethod())
                .setSolutionType(SolutionType.MOORE_PENROSE);
        trainer.update();
        // trainer.train(1000);
        return network;
    }

    public static void setNoise(boolean addNoise) {
        ReservoirComputingUtils.noise = addNoise;
    }

    public static boolean hasNoise() {
        return noise;
    }

    public static void setNoiseMax(double noise) {
        ReservoirComputingUtils.noiseMax = noise;
    }

    public static double getNoiseMax() {
        return noiseMax;
    }

    public static void setNoiseMin(double noiseMin) {
        ReservoirComputingUtils.noiseMin = noiseMin;
    }

    public static double getNoiseMin() {
        return noiseMin;
    }

	public static double getPercentComplete() {
		return percentComplete;
	}

	public static void setPercentComplete(double percentComplete) {
		ReservoirComputingUtils.percentComplete = percentComplete;
	}

	public static EchoStateNetwork getEsn() {
		return esn;
	}

	public static void setEsn(EchoStateNetwork esn) {
		ReservoirComputingUtils.esn = esn;
	}

   
}
