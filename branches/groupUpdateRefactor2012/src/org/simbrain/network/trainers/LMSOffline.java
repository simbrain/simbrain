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
package org.simbrain.network.trainers;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.gui.trainer.TrainerProgressBar;
import org.simbrain.network.interfaces.BiasedNeuron;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.neurons.ClampedNeuron;
import org.simbrain.network.neurons.LinearNeuron;
import org.simbrain.network.neurons.SigmoidalNeuron;
import org.simbrain.network.synapses.ClampedSynapse;
import org.simbrain.network.util.SimnetUtils;
import org.simbrain.util.Matrices;
import org.simbrain.util.propertyeditor.ComboBoxWrapper;

import Jama.Matrix;

/**
 * Offline/Batch Learning with least mean squares.
 *
 * @author ztosi
 * @author jyoshimi
 */
public class LMSOffline extends TrainingMethod {

	/** A listener that tracks progress of training.  */
	private ArrayList<PropertyChangeListener> progressListeners =
			new ArrayList<PropertyChangeListener>();
	
	/** The percent of the task which has been completed. */
	private double percentComplete = 0;
	
    /**
     * Solution methods for offline LMS.
     */
    public enum SolutionType {
        /**
         * Wiener-Hopf solution.
         */
        WIENER_HOPF {
            @Override
            public String toString() {
                return "Wiener-Hopf";
            }
        },

        /**
         * Moore-Penrose Solution.
         */
        MOORE_PENROSE {
            @Override
            public String toString() {
                return "Moore-Penrose";
            }
        }

    };

    /** Current solution type. */
    private SolutionType solutionType = SolutionType.MOORE_PENROSE;


    @Override
    public void apply(Trainer trainer) {
    	
    	
    	
    	//TODO: Clean up? Currently ONLY works for ESNs
    	if(trainer.isStateHarvester()) {
    		trainer.setInputData(ReservoirComputingUtils.generateData
    				(trainer.getInputData(), trainer.getTrainingData()));
    		
    	}

    	int index = 0;
    	for(Neuron n : trainer.getOutputLayer()) {
    		if(n.getUpdateRule() instanceof SigmoidalNeuron) {
    			for(int i = 0; i < trainer.getTrainingData().length; i++) {
    				trainer.getTrainingData()[i][index] = 
    						((SigmoidalNeuron) n.getUpdateRule())
    						.getInverse(trainer.getTrainingData()[i][index], n);
    			}
    		} 
    		index++;
    	}
    	
    	
        if (solutionType == SolutionType.WIENER_HOPF) {
            weinerHopfSolution(trainer);
        } else if (solutionType == SolutionType.MOORE_PENROSE) {
            moorePenroseSolution(trainer);
        } else {
            throw new IllegalArgumentException("Solution type must be "
                    + "'MoorePenrose' or 'WeinerHopf'.");
        }
    }

    /**
     * Implements the Wiener-Hopf solution to LMS linear regression.
     */
    public void weinerHopfSolution(Trainer trainer) {
        Matrix inputMatrix = new Matrix(trainer.getInputData());
        Matrix trainingMatrix = new Matrix(trainer.getTrainingData());

        fireProgressUpdate("Correlating State Matrix (R = S'S)...", 0);
        trainingMatrix = inputMatrix.transpose().times(trainingMatrix);
        
        fireProgressUpdate("Cross-Correlating States with Teacher data (P = S'D)...", 0.15);
        setPercentComplete(0.15);
        inputMatrix = inputMatrix.transpose().times(inputMatrix);

        fireProgressUpdate("Computing Inverse Correlation Matrix...", 0.3);
        setPercentComplete(0.3);
        try {
        	inputMatrix = inputMatrix.inverse();
        	
        fireProgressUpdate("Computing Weights...", 0.8);
        setPercentComplete(0.8);
        double[][] wOut = inputMatrix.times(trainingMatrix).getArray();
        fireProgressUpdate("Setting Weights...", 0.95);
        setPercentComplete(0.95);
        SimnetUtils.setWeightsFillBlanks(trainer.getNetwork(), trainer.getInputLayer(),
                trainer.getOutputLayer(), wOut);
        fireProgressUpdate("Done!", 1.0);
        
        //TODO: What error does JAMA actually throw for singular Matrices?
        } catch (RuntimeException e) {
        	JOptionPane.showMessageDialog(new JFrame(), "" +
        			"State Correlation Matrix is Singular", "Training Failed",
        			JOptionPane.ERROR_MESSAGE);
        	fireProgressUpdate("Training Failed", 0);
        }
        
        trainingMatrix = null;
        inputMatrix = null;
    }

    /**
     * Moore penrose.
     */
    public void moorePenroseSolution(Trainer trainer) {
        Matrix inputMatrix = new Matrix(trainer.getInputData());
        Matrix trainingMatrix = new Matrix(trainer.getTrainingData());

        fireProgressUpdate("Computing Moore-Penrose Pseudoinverse...", Double.NaN);
        // Computes Moore-Penrose Pseudoinverse
        inputMatrix = Matrices.pinv(inputMatrix);

        fireProgressUpdate("Computing Weights...", 0.8);
        setPercentComplete(0.8);
        double[][] wOut = inputMatrix.times(trainingMatrix).getArray();
        
        fireProgressUpdate("Setting Weights...", 0.95);
        setPercentComplete(0.95);
        SimnetUtils.setWeightsFillBlanks(trainer.getNetwork(), trainer.getInputLayer(),
                trainer.getOutputLayer(), wOut);
        fireProgressUpdate("Done!", 1.0);
        
        inputMatrix = null;
        trainingMatrix = null;
    }

    private void fireProgressUpdate (String progressUpdate, double percentComplete) {
    	for(PropertyChangeListener pcl : progressListeners){
    			if(pcl instanceof TrainerProgressBar){
    				pcl.propertyChange(new PropertyChangeEvent(this,
    					"Training", getPercentComplete(), percentComplete));
    			} else {
    				pcl.propertyChange(new PropertyChangeEvent(this,
    						progressUpdate, null, null));
    			}
    	}
    }
    
    private double getPercentComplete () {
    	return percentComplete;
    }
    
    private void setPercentComplete (double percentComplete) {
    	this.percentComplete = percentComplete;
    }
    
    public void addPropertyChangeListener(PropertyChangeListener pcl) {
    	progressListeners.add(pcl);
    }
    
    /**
     * Set solution type.
     *
     * @param solutionType the solutionType to set
     */
    public void setSolutionType(SolutionType solutionType) {
        this.solutionType = solutionType;
    }

    /**
     * Returns the current solution type inside a comboboxwrapper. Used by
     * preference dialog.
     *
     * @return the the comboBox
     */
    public ComboBoxWrapper getSolutionType() {
        return new ComboBoxWrapper() {
            public Object getCurrentObject() {
                return solutionType;
            }

            public Object[] getObjects() {
                return SolutionType.values();
            }
        };
    }

    /**
     * Set the current parse style. Used by preference dialog.
     *
     * @param solutionType the current solution.
     */
    public void setSolutionType(ComboBoxWrapper solutionType) {
        setSolutionType((SolutionType) solutionType.getCurrentObject());
    }

    /**
     * Main method for testing.
     *
     * @param args not used
     */
    public static void main(String[] args) {

        RootNetwork network = test2();
        //System.out.println(network);

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
     * Simple AND Test
     */
    private static RootNetwork test1() {
        RootNetwork network = new RootNetwork();
        Neuron input1 = new Neuron(network, "ClampedNeuron");
        input1.setLocation(10, 70);
        input1.setIncrement(1);
        Neuron input2 = new Neuron(network, "ClampedNeuron");
        input2.setLocation(70, 70);
        input2.setIncrement(1);
        Neuron output = new Neuron(network, "LinearNeuron");
        output.setLocation(15, 0);
        network.addNeuron(input1);
        network.addNeuron(input2);
        network.addNeuron(output);
        List<Neuron> inputList = new ArrayList<Neuron>();
        inputList.add(input1);
        inputList.add(input2);
        List<Neuron> outputList = new ArrayList<Neuron>();
        outputList.add(output);

        // ConnectionLayers
        AllToAll connection = new AllToAll(network, inputList, outputList);
        connection.connectNeurons();

        // AND Task
        double inputData[][] = { { -1, -1 }, { 1, -1 }, { -1, 1 }, { 1, 1 } };
        double trainingData[][] = { { -1 }, { -1 }, { -1 }, { 1 } };

        // Initialize the trainer
        //REDO
//        LMSOffline trainer = new LMSOffline(network, inputList, outputList);
//        trainer.setInputData(inputData);
//        trainer.setTrainingData(trainingData);
//        //trainer.setSolutionType(SolutionType.MOORE_PENROSE);
//        trainer.setSolutionType(SolutionType.WIENER_HOPF);
//        trainer.apply();
        return network;
    }

    /**
     * Simple association test
     */
    private static RootNetwork test2() {

        RootNetwork network = new RootNetwork();

        double inputData[][] = { { .95, 0, 0, 0 }, { 0, .95, 0, 0 },
                { 0, 0, .95, 0 }, { 0, 0, 0, .95 } };
        double trainingData[][] = { { .95, 0 }, { .95, 0 }, { 0, .95 },
                { 0, .95 } };

        // Set up input layer
        List<Neuron> inputLayer = new ArrayList<Neuron>();
        for (int i = 0; i < 4; i++) {
            Neuron neuron = new Neuron(network, new ClampedNeuron());
            neuron.setLocation(10 + (i*40), 70);
            neuron.setIncrement(1);
            network.addNeuron(neuron);
            inputLayer.add(neuron);
            System.out.println("Input " + i + " = " + neuron.getId());
        }

        // Set up output layer
        List<Neuron> outputLayer = new ArrayList<Neuron>();
        for (int i = 0; i < 2; i++) {
            Neuron neuron = new Neuron(network, new LinearNeuron());
            ((BiasedNeuron)neuron.getUpdateRule()).setBias(0);
            neuron.setLocation(15 + (i*40), 0);
            neuron.setLowerBound(0);
            neuron.setUpperBound(1);
            network.addNeuron(neuron);
            //System.out.println("Output " + i + " = " + neuron.getId());
            outputLayer.add(neuron);
        }

        // Connect Layers
        AllToAll connection = new AllToAll(network, inputLayer, outputLayer);
        connection.connectNeurons();

        // Initialize the trainer
        //REDO
//        LMSOffline trainer = new LMSOffline(network, inputLayer, outputLayer);
//        trainer.setInputData(inputData);
//        trainer.setTrainingData(trainingData);
//        //trainer.setSolutionType(SolutionType.MOORE_PENROSE);
//        trainer.setSolutionType(SolutionType.WIENER_HOPF);
//        trainer.apply();
        return network;
    }

}
