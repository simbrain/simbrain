package org.simbrain.custom_sims.simulations.simpleNeuroevolution;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.layouts.LineLayout.LineOrientation;
import org.simbrain.network.neuron_update_rules.LinearRule;

public class EvolveNet extends Network {
	long netSeed;

	
	ArrayList<NeuronGroup> input;
	ArrayList<Neuron> inputFlat;
	List<Neuron> hidden;
	NeuronGroup output;
	ArrayList<Neuron> sourceNeuron;
	List<Neuron> targetNeuron;
	
	
	static int netSize = 25;
	static int eliminationRate = 50;
	static int inputSize = 3;
	static int outputSize = 3;
	static int inputX = 0;
	static int inputY = 0;
	static int outputX = 500;
	static int outputY = 200;	// TODO: use relative
	static int sensoryDimension = 3;
	static int generation = 0;
	static boolean autoMutationControl = true;
	static double mutationRate = 5;
	static double minMutationRate = 0.1;
	static double maxMutationRate = 10;
	static double newSynapseMutationRate = 1;
	static double newNeuronMutationRate = 1;
	static double accidentRate = 1;
	
	public EvolveNet() {
		super();
		input = new ArrayList<>();
		inputFlat = new ArrayList<>();
		hidden = new ArrayList<>();
		sourceNeuron = new ArrayList<>();
		targetNeuron = new ArrayList<>();
	}
	
	public static void toggleAutoMutationControl() {
		autoMutationControl = !autoMutationControl;
	}
	
	public static void setMutationRate(double mr) {
		if(mr >= minMutationRate && mr <= maxMutationRate) {
			mutationRate = mr;
		} else {
			throw new IllegalArgumentException("Mutation rate must be between"
					+ minMutationRate + " and " + maxMutationRate);
		}
	}
	
	public static void setMaxMutationRate(double mr) {
		if(mr >= minMutationRate && mr <= 100) {
			maxMutationRate = mr;
		} else {
			throw new IllegalArgumentException("Max mutation rate must be between"
					+ minMutationRate + " and 100");
		}
	}
	

	
	public static void setMinMutationRate(double mr) {
		if(mr >= 0 && mr <= maxMutationRate) {
			minMutationRate = mr;
		} else {
			throw new IllegalArgumentException("Min rate must be between 0"
					+ " and " + maxMutationRate);
		}
	}
	
	public void setGeneration(int ng) {
		generation = ng;
	}
	
    private void addInput() {
		for(int i = 0; i < sensoryDimension; i++) {
			NeuronGroup newInput = addNeuronGroup(inputX, inputY + (i * 150), inputSize);
			newInput.setLayout(new LineLayout(40, LineLayout.LineOrientation.VERTICAL));
			newInput.setLabel("Input" + i);
			newInput.applyLayout();
			input.add(newInput);
			for(Neuron n : newInput.getNeuronList()) {
				// thisItemFlat.add(n);
				sourceNeuron.add(n);
				inputFlat.add(n);
				
			}
			
		}
		
		//TODO: noise
		NeuronGroup newInput = addNeuronGroup(inputX, inputY + (sensoryDimension * 150), 1);
		newInput.setLabel("Always on");
		input.add(newInput);
		for(Neuron n : newInput.getNeuronList()) {
			n.setActivation(10);
			n.setClamped(true);
			sourceNeuron.add(n);
			inputFlat.add(n);
		}
    	
    }
    
    
    private void addOutput() {
		NeuronGroup newOutput = addNeuronGroup(outputX, outputY, outputSize);
		newOutput.setLayout(new LineLayout(40, LineLayout.LineOrientation.VERTICAL));
		newOutput.setLabel("Output");
		newOutput.applyLayout();
		output = newOutput;
		for(Neuron n : newOutput.getNeuronList()) {
			n.setUpperBound(10);
			n.setLowerBound(0);
			targetNeuron.add(n);
		}
		newOutput.getNeuron(0).setLabel("F");
		newOutput.getNeuron(1).setLabel("L");
		newOutput.getNeuron(2).setLabel("R");
    }

	
	public void mutateNetwork() {
		Random rand = new Random(netSeed);
		
		synapseStrengthMutation();
		
		double newSynapseMutationRand = rand.nextDouble() * 100;
		if(newSynapseMutationRand < newSynapseMutationRate) {
			newSynapseMutation();
		}
		
		double newNeuronMutationRand = rand.nextDouble() * 100;
		if(newNeuronMutationRand < newNeuronMutationRate) {
			newNeuronMutation();
		}
		netSeed =  rand.nextLong();
	}
	
	void synapseStrengthMutation() {
		Random rand = new Random(netSeed);
		List<Synapse> synapseList = this.getFlatSynapseList();
		for(Synapse s : synapseList) {
			double upperBound = s.getUpperBound();
			double lowerBound = s.getLowerBound();
			
			
			double range = (Math.abs(upperBound) + Math.abs(lowerBound));
			
			double delta = ((rand.nextDouble() * range) + lowerBound) * (mutationRate / 100);
			double oldStrength = s.getStrength();
			double newStrength = oldStrength + delta;
			
			if(newStrength < lowerBound) {
				s.setStrength(lowerBound);
			} else if(newStrength > upperBound) {
				s.setStrength(upperBound);
			} else {
				s.setStrength(newStrength);
			}
		}
		netSeed = rand.nextLong();
	}
	
	public void newSynapseMutation() {
		Random rand = new Random(netSeed);
		
		
		int sourceCount = sourceNeuron.size();
		int targetCount = targetNeuron.size();
		
		int sourceIndex = rand.nextInt(sourceCount);
		int targetIndex = rand.nextInt(targetCount);
		Neuron source = sourceNeuron.get(sourceIndex);
		Neuron target = targetNeuron.get(targetIndex);
		
		
		// TODO: use a configurable bound (remember to replace (range / 2)
		int range = 2;
		
		double strength = (rand.nextDouble() * range) - (range / 2);
		
		if(source != target) {
			connect(source, target, strength);
//			synapseIndex.add(
//					new SynapseIndex(sourceIndex, targetIndex, strength)
//			);
		}
		
		netSeed = rand.nextLong();
	}
	

	
	public void newNeuronMutation() {
		Random rand = new Random(netSeed);
		
		int x = rand.nextInt(outputX - inputX) + inputX;
		int y = rand.nextInt(outputY - inputY) + inputY;
		
		
		int synapseCount = this.getSynapseCount();
		int synapseIndex = rand.nextInt(synapseCount);
		
		Synapse replacingSynapse = this.getFlatSynapseList().get(synapseIndex);
		double replacingSynapseStrength = replacingSynapse.getStrength();
		double replacingSynapseUpperBound = replacingSynapse.getUpperBound();
		double replacingSynapseLowerBound = replacingSynapse.getLowerBound();
		
		double range = (Math.abs(replacingSynapseUpperBound) + Math.abs(replacingSynapseLowerBound));
		
		double leftDelta  = ((rand.nextDouble() * range) + replacingSynapseLowerBound) * (mutationRate / 100);
		double rightDelta = ((rand.nextDouble() * range) + replacingSynapseLowerBound) * (mutationRate / 100);
		
		
		Neuron source = replacingSynapse.getSource();
		Neuron target = replacingSynapse.getTarget();
		
		
		// TODO: More rules
		Neuron newNeuron = new Neuron(this, "LinearRule");
		newNeuron.setLocation(x, y);
        addNeuron(newNeuron);
		
		hidden.add(newNeuron);
		
		this.removeSynapse(replacingSynapse);
		
		// TODO: fix potential strength out of bound
		connect(source, newNeuron, replacingSynapseStrength + leftDelta);
		connect(newNeuron, target, replacingSynapseStrength + rightDelta);
		
		sourceNeuron.add(newNeuron);
		targetNeuron.add(newNeuron);
		
		netSeed = rand.nextLong();
	}
	
	
    public Synapse connect(Neuron source, Neuron target, double value) {
        Synapse synapse = new Synapse(source, target);
        synapse.setStrength(value);
        source.getNetwork().addSynapse(synapse);
        return synapse;
    }
    
	public void init() {
		addInput();
		addOutput();
	}
    
	public void setSeed(long thisSeed) {
		netSeed = thisSeed;
		
	}
	
	public long getSeed() {
		return netSeed;
	}
	
	public NeuronGroup getOutput() {
		return this.output;
	}
	
	public NeuronGroup getInput(int i) {
		return this.input.get(i);
	}
	
	
	// TODO: not to copy paste from NetBuilder
	private double GRID_SPACE = 50; // todo; make this settable
	
    public NeuronGroup addNeuronGroup(double x, double y, int numNeurons) {
        return addNeuronGroup(x, y, numNeurons, "line", "LinearRule");
    }
	
    public NeuronGroup addNeuronGroup(double x, double y, int numNeurons,
            String layoutName, NeuronUpdateRule rule) {

        NeuronGroup ng;
        ng = new NeuronGroup(this, new Point2D.Double(x, y), numNeurons);
        ng.setNeuronType(rule);

        layoutNeuronGroup(ng, x, y, layoutName);
        this.addGroup(ng);
        return ng;

    }

    public NeuronGroup addNeuronGroup(double x, double y, int numNeurons,
            String layoutName, String type) {
        return addNeuronGroup(x, y, numNeurons, layoutName, new LinearRule());
    }
	
    private void layoutNeuronGroup(NeuronGroup ng, double x, double y,
            String layoutName) {

        if (layoutName.toLowerCase().contains("line")) {
            if (layoutName.equalsIgnoreCase("vertical line")) {
                LineLayout lineLayout = new LineLayout(x, y, 50,
                        LineOrientation.VERTICAL);
                ng.setLayout(lineLayout);
            } else {
                LineLayout lineLayout = new LineLayout(x, y, 50,
                        LineOrientation.HORIZONTAL);
                ng.setLayout(lineLayout);
            }
        } else if (layoutName.equalsIgnoreCase("grid")) {
            GridLayout gridLayout = new GridLayout(GRID_SPACE, GRID_SPACE,
                    (int) Math.sqrt(ng.size()));
            ng.setLayout(gridLayout);
        }
        ng.applyLayout();

    }
    
    public EvolveNet copy() {
        preSaveInit();
        String xml_rep = Network.getXStream().toXML(this);
        postSaveReInit();
        return (EvolveNet) Network.getXStream().fromXML(xml_rep);
    }

	public static void increaseGeneration() {
		generation++;
	}
    
    
	public static void setNetSize(int size) {
		if(size > 0) {
			netSize = size;
		} else {
			throw new IllegalArgumentException("Network Count must be greater than 0");
		}
	}
    
    
    
}
