package org.simbrain.custom_sims.simulations.simpleNeuroevolution;

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.custom_sims.helper_classes.OdorWorldBuilder;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.util.CopyPaste;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;
import org.simbrain.world.odorworld.sensors.SmellSensor;

public class SimpleNeuroevolution extends RegisteredSimulation {

	long seed = 0;
	Random globalRand;
	int borderSize = 5;	// TODO: Find out the real panel size
	int imageSize = 32;
	int netSize = 25;
	int inputSize = 3;
	int outputSize = 3;
	int inputX = 0;
	int inputY = 0;
	int outputX = 500;
	int outputY = 200;	// TODO: use relative
	int sensoryDimension = 3;
	int eliminationRate = 50;
	
	boolean run = false;
	boolean autoMutationControl = true;
	double mutationRate = 5;
	double minMutationRate = 0.1;
	double maxMutationRate = 10;
	double newSynapseMutationRate = 1;
	double newNeuronMutationRate = 1;
	double accidentRate = 1;
	
	int generation = 0;
	List<NetBuilder> net = new ArrayList<>();
	List<Long> netSeed = new ArrayList<>();
	
//	List<ArrayList<Object>> itemFlat = new ArrayList<>();
//	List<ArrayList<Neuron>> neuronFlat = new ArrayList<>();
//	List<ArrayList<Integer[]>> inputIndex = new ArrayList<>();
//	List<ArrayList<Integer>> inputFlatIndex = new ArrayList<>();
//	List<ArrayList<Integer>> hiddenIndex = new ArrayList<>();
//	List<ArrayList<Integer>> outputIndex = new ArrayList<>();
//	List<ArrayList<Integer>> sourceNeuronIndex = new ArrayList<>();
//	List<ArrayList<Integer>> targetNeuronIndex = new ArrayList<>();
//	List<ArrayList<SynapseIndex>> synapseIndex = new ArrayList<>();
	
	List<ArrayList<NeuronGroup>> input = new ArrayList<>();
	List<ArrayList<Neuron>> inputFlat = new ArrayList<>();
	List<ArrayList<Neuron>> hidden = new ArrayList<>();
	List<NeuronGroup> output = new ArrayList<>();
	List<ArrayList<Neuron>> sourceNeuron = new ArrayList<>();
	List<ArrayList<Neuron>> targetNeuron = new ArrayList<>();
	List<OdorWorldBuilder> world = new ArrayList<>();
	List<RotatingEntity> mouse = new ArrayList<>();
	List<OdorWorldEntity> cheese = new ArrayList<>();
	List<OdorWorldEntity> poison = new ArrayList<>();
	List<NetWorldPairAttribute> attribute = new ArrayList<>();
	ControlPanel cp, dp;
	
	boolean componentMinimized = false;
	
	public SimpleNeuroevolution(SimbrainDesktop desktop) {
		super(desktop);
	}

	public SimpleNeuroevolution() {
		super();
	}

	public void setNetSize(int size) {
		if(size > 0) {
			netSize = size;
		} else {
			throw new IllegalArgumentException("Network Count must be greater than 0");
		}
	}
	
	public void toggleAutoMutationControl() {
		autoMutationControl = !autoMutationControl;
	}
	
	public void setMutationRate(double mutationRate) {
		if(mutationRate >= minMutationRate && mutationRate <= maxMutationRate) {
			this.mutationRate = mutationRate;
		} else {
			throw new IllegalArgumentException("Mutation rate must be between"
					+ minMutationRate + " and " + maxMutationRate);
		}
	}
	
	public void setMaxMutationRate(double maxMutationRate) {
		if(maxMutationRate >= minMutationRate && maxMutationRate <= 100) {
			this.maxMutationRate = maxMutationRate;
		} else {
			throw new IllegalArgumentException("Max mutation rate must be between"
					+ minMutationRate + " and 100");
		}
	}
	

	
	public void setMinMutationRate(double minMutationRate) {
		if(minMutationRate >= 0 && minMutationRate <= maxMutationRate) {
			this.minMutationRate = minMutationRate;
		} else {
			throw new IllegalArgumentException("Min rate must be between 0"
					+ " and " + maxMutationRate);
		}
	}
	
	private void setUpWorkSpace() {
		int frameHeight	= (int) sim.getDesktop().getHeight();
		int frameWidth  = (int) sim.getDesktop().getWidth();
		int cpWidth     = cp.getWidth() + borderSize * 2;
		int netLayOutColumn = (int) Math.ceil(Math.sqrt(netSize));
		int netLayOutRow    = (int) Math.ceil((double)netSize / netLayOutColumn);
		int netLayOutColumnPx = (frameWidth - cpWidth) / netLayOutColumn;
		int netLayOutRowPx    = frameHeight / netLayOutRow;
		
		SwingUtilities.invokeLater(() -> {
			for(int i = 0, currentX = cpWidth, currentY = 0, index = 0; i < netLayOutRow; i++) {
				currentY = netLayOutRowPx * i;
				
				for(int j = 0; j < netLayOutColumn && index + 1 < netSize; j++) {
					index = i * netLayOutColumn + j;
					currentX = cpWidth + netLayOutColumnPx * j;
					int width = netLayOutColumnPx / 2;
					int height = netLayOutRowPx;
									
					initializeNetwork(index, currentX, currentY, width, height);
					addWorld(index, (currentX + width), currentY, width, height);
				}
				
			}
			
			
			for(int i = 0; i < net.size(); i++) {
				addNetwork(i);
				newSynapseMutation(i);
				setUpCoupling(i);
			}
				
		});

		
	}
	
	private void deleteWorkspacePair(int netIndex) {
		System.out.println("Removing " + netIndex);
		NetBuilder netToDelete = net.get(netIndex);
		OdorWorldBuilder worldToDelete = world.get(netIndex);
		sim.getWorkspace().removeWorkspaceComponent(netToDelete.getNetworkComponent());
		sim.getWorkspace().removeWorkspaceComponent(worldToDelete.getOdorWorldComponent());
		net.set(netIndex, null);
		netSeed.set(netIndex, null);
		input.set(netIndex, null);
		inputFlat.set(netIndex, null);
		hidden.set(netIndex, null);
		output.set(netIndex, null);
		sourceNeuron.set(netIndex, null);
		targetNeuron.set(netIndex, null);
		world.set(netIndex, null);
		mouse.set(netIndex, null);
		cheese.set(netIndex, null);
		poison.set(netIndex, null);
		attribute.set(netIndex, null);
//		synapseIndex.set(netIndex, null);
	}
	
	
	// TODO: Configurable distance
	// TODO: Use event
    private void updateFitnessScore() {
    	for(int i = 0; i < net.size(); i++) {
            int cheeseDistance = (int) SimbrainMath.distance(
                    mouse.get(i).getCenterLocation(), cheese.get(i).getCenterLocation());
            if (cheeseDistance < 16) {
                attribute.get(i).addFitness();
                randomizeEntityLocation(i, 400, 400, cheese.get(i));
            }
    	}
    }
    
    private double finalizeFitnessScore(int netIndex) {
        int cheeseDistance = (int) SimbrainMath.distance(
                mouse.get(netIndex).getCenterLocation(), cheese.get(netIndex).getCenterLocation());
        double delta = (128 - cheeseDistance) / 128;
        attribute.get(netIndex).addFitness(delta);
        return attribute.get(netIndex).getFitnessScore();
    }
    
    private ArrayList<NetWorldPairAttribute> sortFitness() {
    	ArrayList<NetWorldPairAttribute> acp = new ArrayList<>(attribute);
    	Collections.sort(acp, new FitnessComparator());
		return acp;
    }
    
    private void randomizeEntityLocation(int netIndex, int width, int height, OdorWorldEntity e) {
    	int x, y;
    	long thisSeed = netSeed.get(netIndex);
		Random rand = new Random(thisSeed);
		x = rand.nextInt(width);
		y = rand.nextInt(height);
    	e.setLocation(x + (imageSize / 2), y + (imageSize / 2));
    	netSeed.set(netIndex, rand.nextLong());
    	
    }
	
    private void newGeneration() {
    	SwingUtilities.invokeLater(() -> {
    		generation++;
	    	ArrayList<NetWorldPairAttribute> performance = sortFitness();
	    	// TODO: may be ceil?
	    	int accidentCount = 1 + (int) ((accidentRate/100) * netSize);
	    	
	    	for(int i = 0; i < accidentCount; i++) {
	    		Collections.swap(performance, globalRand.nextInt(netSize), globalRand.nextInt(netSize));
	    	}
	    	
	    	
	    	int eliminationCount = netSize * eliminationRate / 100;
	    	for(int i = 0; i < eliminationCount; i++) {
	    		int parentIndex = performance.get(globalRand.nextInt(netSize - eliminationCount) + eliminationCount).getNetIndex();
	    		int currentNetIndex = performance.get(i).getNetIndex();
	    		System.out.println("CurrentIndex: " + currentNetIndex);
	    		System.out.println("Current Generation: " + generation);
	    		int x = performance.get(i).getWindowLocationX();
	    		int y = performance.get(i).getWindowLocationY();
	    		int w = performance.get(i).getWindowWidth();
	    		int h = performance.get(i).getWindowHeight();
	    		
	    		ArrayList<Object> allStuff = new ArrayList<>();
	    		for(NeuronGroup ng : net.get(parentIndex).getNetwork().getFlatNeuronGroupList()) {
	    			allStuff.add(ng);
	    		}
	    		for(Synapse s : net.get(parentIndex).getNetwork().getFlatSynapseList()) {
	    			allStuff.add(s);
	    		}
	    		
	    		deleteWorkspacePair(currentNetIndex);
//	    		ArrayList<?> newItem = 
//	    				CopyPaste.getCopy(net.get(currentNetIndex).getNetwork(), allStuff);
	    		initializeNetwork(currentNetIndex, x, y, w, h);
	    		addNetwork(currentNetIndex);
	    		addWorld(currentNetIndex, (x + w), y, w, h);
	    	}
	    	
	    	
	    	for(int i = 0; i < eliminationCount; i++) {
	    		setUpCoupling(i);
	    	}
    	
    	});
    }
    
    private void initializeNetwork(int netIndex, int x, int y, int width, int height) {
		Random rand = new Random(seed + netIndex);
		long thisSeed = rand.nextLong();
		
		int prefixDigit = (int)(Math.log10(netSize)+1);
		int prefix = (int) ((generation + 1) * Math.pow(10, prefixDigit));
		
		System.out.println(net.size());
		if(netIndex < net.size()) {
			System.out.println("set");
			netSeed.set(netIndex, thisSeed);
//			itemFlat.set(netIndex, new ArrayList<>());
			sourceNeuron.set(netIndex, new ArrayList<>());
			targetNeuron.set(netIndex, new ArrayList<>());
			input.set(netIndex, new ArrayList<>());
			inputFlat.set(netIndex, new ArrayList<>());
			attribute.set(netIndex, new NetWorldPairAttribute(prefix + netIndex, netIndex, x, y, width, height));
			net.set(netIndex, sim.addNetwork(x, y, width, height, "N" + attribute.get(netIndex).getNetID()));
			hidden.set(netIndex, new ArrayList<>());
//			synapseIndex.set(netIndex, new ArrayList<>());
		} else {
			System.out.println("add");
			netSeed.add(thisSeed);
//			itemFlat.add(new ArrayList<>());
			sourceNeuron.add(new ArrayList<>());
			targetNeuron.add(new ArrayList<>());
			input.add(new ArrayList<>());
			inputFlat.add(new ArrayList<>());
			attribute.add(new NetWorldPairAttribute(prefix + netIndex, netIndex, x, y, width, height));
			net.add(sim.addNetwork(x, y, width, height, "N" + attribute.get(netIndex).getNetID()));
			hidden.add(new ArrayList<>());
//			synapseIndex.add(new ArrayList<>());
		}
		
    }
    
    private void addInput(int netIndex) {
		ArrayList<Neuron> thisSourceNeuronList = sourceNeuron.get(netIndex);
		ArrayList<NeuronGroup> thisInput = input.get(netIndex);
		ArrayList<Neuron> thisInputFlat = inputFlat.get(netIndex);
		
		for(int i = 0; i < sensoryDimension; i++) {
			NeuronGroup newInput = net.get(netIndex).addNeuronGroup(inputX, inputY + (i * 150), inputSize);
			newInput.setLayout(new LineLayout(40, LineLayout.LineOrientation.VERTICAL));
			newInput.setLabel("Input" + i);
			newInput.applyLayout();
			thisInput.add(newInput);
			for(Neuron n : newInput.getNeuronList()) {
				// thisItemFlat.add(n);
				thisSourceNeuronList.add(n);
				thisInputFlat.add(n);
				
			}
			
		}
		
		//TODO: noise
		NeuronGroup newInput = net.get(netIndex).addNeuronGroup(inputX, inputY + (sensoryDimension * 150), 1);
		newInput.setLabel("Always on");
		thisInput.add(newInput);
		for(Neuron n : newInput.getNeuronList()) {
			n.setActivation(10);
			n.setClamped(true);
			thisSourceNeuronList.add(n);
			thisInputFlat.add(n);
		}
    	
    }
    
    
    private void addOutput(int netIndex) {
    	ArrayList<Neuron> thisTargetNeuronList = targetNeuron.get(netIndex);
    	
		NeuronGroup newOutput = net.get(netIndex).addNeuronGroup(outputX, outputY, outputSize);
		newOutput.setLayout(new LineLayout(40, LineLayout.LineOrientation.VERTICAL));
		newOutput.setLabel("Output");
		newOutput.applyLayout();
		output.add(newOutput);
		for(Neuron n : newOutput.getNeuronList()) {
			n.setUpperBound(10);
			n.setLowerBound(0);
			thisTargetNeuronList.add(n);
		}
    }
    
	private void addNetwork(int netIndex) {
		addInput(netIndex);
		addOutput(netIndex);
	}
	
	
	private void addWorld(int worldIndex, int x, int y, int width, int height) {
		if(worldIndex < world.size()) {
			world.set(worldIndex, sim.addOdorWorld(
					x, y, width, height, "W" + attribute.get(worldIndex).getNetID()
			));
		} else {
			world.add(sim.addOdorWorld(
					x, y, width, height, "W" + attribute.get(worldIndex).getNetID()
			));
		}
		OdorWorldBuilder currentWorld = world.get(worldIndex);
		currentWorld.getWorld().setObjectsBlockMovement(false);
		int worldHeight = currentWorld.getWorld().getHeight();
		int worldWidth  = currentWorld.getWorld().getWidth();
//		int worldHeight = 133; // TODO: use real value
//		int worldWidth  = 164;
		
		// Print world size for debug
		System.out.println("I: " + worldIndex + "; (" + worldWidth + ", " + worldHeight + ")");
		
		// TODO: Make configurable
		cheese.add(currentWorld.addEntity(
				worldWidth / 6 * 5 - (imageSize/2),
				worldHeight / 3 - (imageSize/2),
				"Swiss.gif",
                new double[] { 1, 0.1, 0.2 }));
		
		poison.add(currentWorld.addEntity(worldWidth / 6 * 5 - (imageSize/2),
				worldHeight / 3 * 2 - (imageSize/2),
				"Poison.gif",
                new double[] { 0.2, 0, 1 }));
		
		mouse.add(currentWorld.addAgent(worldWidth / 6 - (imageSize/2),
				worldHeight / 2 - (imageSize/2), "Mouse"));
	}
	
	
	private void setUpCoupling(int netIndex) {
		// TODO: make flexible
//		for(int k = 0; k < 3; k++) {
//			sim.couple((SmellSensor) mouse.get(netIndex).getSensor("Smell-Left"), k,
//	                input.get(netIndex).getNeuron(k));
//		}
//		for(int k = 3; k < 6; k++) {
//			sim.couple((SmellSensor) mouse.get(netIndex).getSensor("Smell-Center"), k,
//	                input.get(netIndex).getNeuron(k));
//		}
//		for(int k = 6; k < 9; k++) {
//			sim.couple((SmellSensor) mouse.get(netIndex).getSensor("Smell-Right"), k,
//	                input.get(netIndex).getNeuron(k));
//		}
		
		int sensoorSize = mouse.get(netIndex).getSensors().size();
		// TODO: Couple sensor to input.
		mouse.get(netIndex).getSensors().get(1).getId();
		
		sim.couple((SmellSensor) mouse.get(netIndex).getSensors().get(0), input.get(netIndex).get(0));
		sim.couple((SmellSensor) mouse.get(netIndex).getSensors().get(1), input.get(netIndex).get(1));
		sim.couple((SmellSensor) mouse.get(netIndex).getSensors().get(2), input.get(netIndex).get(2));
//		sim.couple((SmellSensor) mouse.get(netIndex).getSensors().get(0), 0, inputFlat.get(netIndex).get(0));
		
		
		sim.couple(output.get(netIndex).getNeuron(0), mouse.get(netIndex).getEffectors().get(0));
		sim.couple(output.get(netIndex).getNeuron(1), mouse.get(netIndex).getEffectors().get(1));
		sim.couple(output.get(netIndex).getNeuron(2), mouse.get(netIndex).getEffectors().get(2));
	}
	
	
	private void setUpControlPanel() {
		cp = ControlPanel.makePanel(sim, "Control Panel", 0, 0);
		JButton createBtn = cp.addButton("Create " + netSize + " Networks", () -> {
			int result = 0;
			if(net.size() != 0) {
				result = JOptionPane.showConfirmDialog(
					    null,
					    "Current networks will be deleted. Are you sure?",	// TODO: implement delete
					    "Warning",
					    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			}
			if(result == 0) {
				setUpWorkSpace();
				for(int i = 0; i < net.size(); i++) {
					newSynapseMutation(i);
				}
			}
		});
		
		JTextField networkCountTF = cp.addTextField("Network Count", "" + netSize);
		cp.addButton("Set Network Size", () -> {
			try {
				setNetSize(Integer.parseInt(networkCountTF.getText()));
				createBtn.setText("Create " + netSize + " Networks");
			}
			catch(NumberFormatException ex) {
				JOptionPane.showMessageDialog(null, "Network Count must be an integer");
				networkCountTF.setText("" + netSize);
			}
			catch(IllegalArgumentException ex) {
				JOptionPane.showMessageDialog(null, ex.getMessage());
				networkCountTF.setText("1");
			}
		});
		
		
		// Auto mutation control stuff are not that important at this moment.
		// When implement, each network will have its mutation rate adjusted base on performance.
		// At this moment all networks use the global `mutationRate`.
		
		JLabel currentMutationRateLabel = cp.addLabel("Current Mutation %", "" + mutationRate);
		JTextField currentMutationRateTF = cp.addTextField("Set mutation rate to", "" + mutationRate);
		JButton currentMutationRateTFBtn = cp.addButton("Set Mutation Rate", () -> {
			try {
				setMutationRate(Double.parseDouble(currentMutationRateTF.getText()));
				currentMutationRateLabel.setText("" + mutationRate);
			}
			catch(NumberFormatException ex) {
				JOptionPane.showMessageDialog(null, "Mutation rate must be an number");
				currentMutationRateTF.setText("" + mutationRate);
			}
			catch(IllegalArgumentException ex) {
				JOptionPane.showMessageDialog(null, ex.getMessage());
				currentMutationRateTF.setText("" + mutationRate);
			}
		});
		
		JLabel currentMaxMutationRateLabel = cp.addLabel("Current Max Mutation %", "" 
		+ maxMutationRate);
		JTextField currentMaxMutationRateTF = cp.addTextField("Set mutation rate to", "" 
		+ maxMutationRate);
		cp.addButton("Set Max Mutation Rate", () -> {
			try {
				setMaxMutationRate(Double.parseDouble(currentMaxMutationRateTF.getText()));
				currentMaxMutationRateLabel.setText("" + maxMutationRate);
			}
			catch(NumberFormatException ex) {
				JOptionPane.showMessageDialog(null, "Mutation rate must be an number");
				currentMaxMutationRateTF.setText("" + maxMutationRate);
			}
			catch(IllegalArgumentException ex) {
				JOptionPane.showMessageDialog(null, ex.getMessage());
				currentMaxMutationRateTF.setText("" + maxMutationRate);
			}
		});
		
		JLabel currentMinMutationRateLabel = cp.addLabel("Current Min Mutation %", "" 
		+ minMutationRate);
		JTextField currentMinMutationRateTF = cp.addTextField("Set mutation rate to", "" 
		+ minMutationRate);
		cp.addButton("Set Min Mutation Rate", () -> {
			try {
				setMinMutationRate(Double.parseDouble(currentMinMutationRateTF.getText()));
				currentMinMutationRateLabel.setText("" + minMutationRate);
			}
			catch(NumberFormatException ex) {
				JOptionPane.showMessageDialog(null, "Mutation rate must be an number");
				currentMinMutationRateTF.setText("" + minMutationRate);
			}
			catch(IllegalArgumentException ex) {
				JOptionPane.showMessageDialog(null, ex.getMessage());
				currentMinMutationRateTF.setText("" + minMutationRate);
			}
		});
		
		JLabel autoMutationLabel = cp.addLabel("Auto Mutation Control:",
				(autoMutationControl ? "ON" : "OFF"));
		cp.addButton("Toggle", () -> {
			toggleAutoMutationControl();
			autoMutationLabel.setText((autoMutationControl ? "ON" : "OFF"));
			currentMutationRateTF.setEnabled(!autoMutationControl);
			currentMutationRateTFBtn.setEnabled(!autoMutationControl);
		});
		
		cp.addButton("Run", () -> {
			run = true;
			while(run) {
				for(int i = 0; i < 1000 && run; i++) {
					sim.iterate();
					updateFitnessScore();
				}
				
				// TODO: Apply to new network only. Move to newGeneration()
				for(int i = 0; i < net.size(); i++) {
					mutateNetwork(i);
				}
			}
//			sim.getWorkspace().stop();
		});
		
		cp.addButton("Stop", () -> {
			run = false;
//			sim.getWorkspace().stop();
		});
		
		currentMutationRateTF.setEnabled(!autoMutationControl);
		currentMutationRateTFBtn.setEnabled(!autoMutationControl);
	}


	private void setUpDebugPanel() {
		int y = cp.getHeight();
		dp = ControlPanel.makePanel(sim, "Debug Panel", 0, y + borderSize + 24);
		
		// Manual mutation control
		dp.addButton("New Synapse Mutation", () -> {
			for(int i = 0; i < net.size(); i++) {
				newSynapseMutation(i);
			}
		});
		dp.addButton("Synapse Strength Mutation", () -> {
			for(int i = 0; i < net.size(); i++) {
				synapseStrengthMutation(i);
			}
		});
		dp.addButton("New Neuron Mutation", () -> {
			for(int i = 0; i < net.size(); i++) {
				newNeuronMutation(i);
			}
		});
		
		
		// Just to test network removal.
		dp.addButton("Remove network 1", () -> {
			sim.getWorkspace().removeWorkspaceComponent(net.get(1).getNetworkComponent());
			sim.getWorkspace().removeWorkspaceComponent(world.get(1).getOdorWorldComponent());
		});
		dp.addButton("Add network 1 back", () -> {
			sim.getWorkspace().addWorkspaceComponent(net.get(1).getNetworkComponent());
			sim.getWorkspace().addWorkspaceComponent(world.get(1).getOdorWorldComponent());
		});
		dp.addButton("Remove All Networks", () -> {
			for(int i = 0; i < net.size(); i++) {
				sim.getWorkspace().removeWorkspaceComponent(net.get(i).getNetworkComponent());
				sim.getWorkspace().removeWorkspaceComponent(world.get(i).getOdorWorldComponent());
			}
		});
		
		
		dp.addButton("Toggle Minimize", () -> {
			for(int i = 0; i < net.size(); i++) {
				try {
					sim.getDesktop().getDesktopComponent(net.get(i).getNetworkComponent()).getParentFrame().setIcon(!componentMinimized);
					sim.getDesktop().getDesktopComponent(world.get(i).getOdorWorldComponent()).getParentFrame().setIcon(!componentMinimized);
				} catch (PropertyVetoException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			componentMinimized = !componentMinimized;
		});
		
		dp.addButton("Print Fitness", () -> {
			for(int i = 0; i < attribute.size(); i++) {
				System.out.println(attribute.get(i).getFitnessScore());
			}
		});
		
		dp.addButton("Elimiate", () -> {
			newGeneration();
		});
	}
	
	public void mutateNetwork(int netIndex) {
		long thisSeed = netSeed.get(netIndex);
		Random rand = new Random(thisSeed);
		
		synapseStrengthMutation(netIndex);
		
		double newSynapseMutationRand = rand.nextDouble() * 100;
		if(newSynapseMutationRand < newSynapseMutationRate) {
			newSynapseMutation(netIndex);
		}
		
		double newNeuronMutationRand = rand.nextDouble() * 100;
		if(newNeuronMutationRand < newNeuronMutationRate) {
			newNeuronMutation(netIndex);
		}
		netSeed.set(netIndex, rand.nextLong());
	}
	
	private void synapseStrengthMutation(int netIndex) {
		long thisSeed = netSeed.get(netIndex);
		Random rand = new Random(thisSeed);
		List<Synapse> synapseList = net.get(netIndex).getNetwork().getFlatSynapseList();
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
		netSeed.set(netIndex, rand.nextLong());
	}
	
	private void newSynapseMutation(int netIndex) {
		long thisSeed = netSeed.get(netIndex);
		Random rand = new Random(thisSeed);
		
		
		int sourceCount = sourceNeuron.get(netIndex).size();
		int targetCount = targetNeuron.get(netIndex).size();
		
		int sourceIndex = rand.nextInt(sourceCount);
		int targetIndex = rand.nextInt(targetCount);
		Neuron source = sourceNeuron.get(netIndex).get(sourceIndex);
		Neuron target = targetNeuron.get(netIndex).get(targetIndex);
		
		
		// TODO: use a configurable bound (remember to replace (range / 2)
		int range = 2;
		
		double strength = (rand.nextDouble() * range) - (range / 2);
		
		if(source != target) {
			net.get(netIndex).connect(source, target, strength);
//			synapseIndex.get(netIndex).add(
//					new SynapseIndex(sourceIndex, targetIndex, strength)
//			);
		}
		
		netSeed.set(netIndex, rand.nextLong());
	}
	

	
	private void newNeuronMutation(int netIndex) {
		long thisSeed = netSeed.get(netIndex);
		Random rand = new Random(thisSeed);
		
		int x = rand.nextInt(outputX - inputX) + inputX;
		int y = rand.nextInt(outputY - inputY) + inputY;
		
		
		int synapseCount = net.get(netIndex).getNetwork().getSynapseCount();
		int synapseIndex = rand.nextInt(synapseCount);
		
		Synapse replacingSynapse = net.get(netIndex).getNetwork().getFlatSynapseList().get(synapseIndex);
		double replacingSynapseStrength = replacingSynapse.getStrength();
		double replacingSynapseUpperBound = replacingSynapse.getUpperBound();
		double replacingSynapseLowerBound = replacingSynapse.getLowerBound();
		
		double range = (Math.abs(replacingSynapseUpperBound) + Math.abs(replacingSynapseLowerBound));
		
		double leftDelta  = ((rand.nextDouble() * range) + replacingSynapseLowerBound) * (mutationRate / 100);
		double rightDelta = ((rand.nextDouble() * range) + replacingSynapseLowerBound) * (mutationRate / 100);
		
		
		Neuron source = replacingSynapse.getSource();
		Neuron target = replacingSynapse.getTarget();
		Neuron newNeuron = net.get(netIndex).addNeuron(x, y);
		hidden.get(netIndex).add(newNeuron);
		
		net.get(netIndex).getNetwork().removeSynapse(replacingSynapse);
		
		// TODO: fix potential strength out of bound
		net.get(netIndex).connect(source, newNeuron, replacingSynapseStrength + leftDelta);
		net.get(netIndex).connect(newNeuron, target, replacingSynapseStrength + rightDelta);
		
		sourceNeuron.get(netIndex).add(newNeuron);
		targetNeuron.get(netIndex).add(newNeuron);
		
		netSeed.set(netIndex, rand.nextLong());
	}
	
	
	@Override
	public String getName() {
		return "Simple NeuroEvolution";
	}

	@Override
	public void run() {
        // Clear workspace
        sim.getWorkspace().clearWorkspace();
        seed = System.nanoTime();
        Random rand = new Random(seed);
        seed = rand.nextLong();
        globalRand = new Random(seed);
        System.out.println(seed);
        
        NetWorldPair.sim = sim;
        
        setUpControlPanel();
        setUpDebugPanel();

	}

	@Override
	public RegisteredSimulation instantiate(SimbrainDesktop desktop) {
		return new SimpleNeuroevolution(desktop);
	}

}
