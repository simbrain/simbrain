package org.simbrain.custom_sims.simulations.creatures;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.workspace.updater.UpdateActionAdapter;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.OdorWorldComponent;

/**
 * A simulation of an A-Life agent based off of the Creatures entertainment
 * software by Cyberlife Technology & Steve Grand, as seen in "Creatures:
 * Entertainment software agents with artificial life" (D. Cliff & S. Grand,
 * 1998)
 * 
 * @author Sharai
 *
 */
public class Creatures extends RegisteredSimulation {

	/**
	 * Stolen from the Simulation class: Associate networks and worlds with their
	 * respective components. Entries are added when networks or worlds are added
	 * using the sim object. Facilitates making couplings using methods with fewer
	 * arguments.
	 */
	Hashtable<Network, NetworkComponent> netMap = new Hashtable();
	Hashtable<OdorWorld, OdorWorldComponent> odorMap = new Hashtable();

	/**
	 * A list of brains. Good for updating and maintaining multiple brains
	 */
	List<CreaturesBrain> brainList = new ArrayList(); // TODO: lobelist?

	@Override
	public void run() {
		// Clear workspace
		sim.getWorkspace().clearWorkspace();

		// Add doc viewer // TODO: Rename to creaturesDocs.html or something
		sim.addDocViewer(0, 0, 450, 600, "Doc",
				"src/org/simbrain/custom_sims/simulations/creatures/CreaturesMain.html");

		// Create starting brain
		CreaturesBrain brain1 = createBrain(451, 0, 550, 600, "Ron");
		brainList.add(brain1);
	
		
		// Assign a neuron a label then access the neuron using that label 
        brain1.lobes.get(0).getNeuronList().get(0).setLabel("Hungry");
        brain1.lobes.get(0).getNeuronList().get(1).setLabel("Happy");
        Neuron hungerNeuron = brain1.lobes.get(0).getNeuronByLabel("Hungry");
        Neuron happyNeuron = brain1.lobes.get(0).getNeuronByLabel("Happy");
		hungerNeuron.setActivation(4);
		brain1.getNetwork().fireNeuronsUpdated(); // So the activation is visible

		// Adding a custom synapse.   Prob. not necessary to give them names in most cases...
        Synapse happyToHungry = brain1.builder.connect(hungerNeuron,
                happyNeuron, new CreaturesSynapseRule(), 1);
		brain1.getNetwork().fireSynapseAdded(happyToHungry);
		
		// Create update action
		sim.getWorkspace().addUpdateAction(new UpdateActionAdapter("Update Creatures Sim") {
			@Override
			public void invoke() {
				updateCreaturesSim();
			}
		});

	}

	/**
	 * Update function for the Creatures simulation.
	 */
	void updateCreaturesSim() {
		// Update all brains
		for (CreaturesBrain b : brainList) {
			b.getNetwork().update();
		}

	}
	//TODO: Refactor below.
	
	/**
	 * Creates a brain network and displays it in the workspace.
	 * 
	 * @param name
	 * @return a CreaturesBrain object
	 */
	public CreaturesBrain createBrain(int x, int y, int width, int height, String name) {
		NetworkComponent networkComponent = new NetworkComponent(name + "'s Brain");
		sim.getWorkspace().addWorkspaceComponent(networkComponent);
		sim.getDesktop().getDesktopComponent(networkComponent).getParentFrame().setBounds(x, y, width, height);
		netMap.put(networkComponent.getNetwork(), networkComponent);

		CreaturesBrain brain = new CreaturesBrain(networkComponent);

		brain.setUp();
		return brain;
	}

	public CreaturesBrain createBrain(int x, int y, int width, int height) {
		NetworkComponent networkComponent = new NetworkComponent("Brain");
		sim.getWorkspace().addWorkspaceComponent(networkComponent);
		sim.getDesktop().getDesktopComponent(networkComponent).getParentFrame().setBounds(x, y, width, height);
		netMap.put(networkComponent.getNetwork(), networkComponent);

		CreaturesBrain brain = new CreaturesBrain(networkComponent);

		brain.setUp();
		return brain;
	}

	
    public Creatures(SimbrainDesktop desktop) {
        super(desktop);
    }

    public Creatures() {
        super();
    }

    /**
     * Runs the constructor for the simulation.
     */
    @Override
    public Creatures instantiate(SimbrainDesktop desktop) {
        return new Creatures(desktop);
    }

    /**
     * Supplies the name of the simulation.
     */
    @Override
    public String getName() {
        return "Creatures";
    }

}
