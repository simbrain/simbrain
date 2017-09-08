package org.simbrain.custom_sims.simulations.creatures;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.NetworkUpdateAction;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.workspace.updater.UpdateActionAdapter;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
//import org.simbrain.custom_sims.helper_classes.Simulation;

import org.simbrain.custom_sims.simulations.creatures.CreaturesBrain;

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
	List<CreaturesBrain> brainList = new ArrayList();

	/**
	 * This is a constructor.
	 * 
	 * @param desktop
	 *            The Simbrain application
	 */
	public Creatures(SimbrainDesktop desktop) {
		super(desktop);
	}

	public Creatures() {
		super();
	}

	@Override
	public void run() {
		// Clear workspace
		sim.getWorkspace().clearWorkspace();

		// Add doc viewer
		sim.addDocViewer(0, 0, 450, 600, "Doc",
				"src/org/simbrain/custom_sims/simulations/creatures/CreaturesMain.html");

		// Create starting brain
		createBrain(451, 0, 550, 600, "Ron");

		// Create update action
		sim.getWorkspace().addUpdateAction(new UpdateActionAdapter("Update Creatures Sim") {
			@Override
			public void invoke() {
				updateCreaturesSim();
			}
		});

	}

	/**
	 * Update function for the Creatures simulation
	 */
	void updateCreaturesSim() {
		// Update all brains
		for (CreaturesBrain b : brainList) {
			b.getNetworkComponent().update();
		}

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

	/**
	 * Creates a brain network and displays it in the workspace
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
		brainList.add(brain);
		return brain;
	}

	public CreaturesBrain createBrain(int x, int y, int width, int height) {
		NetworkComponent networkComponent = new NetworkComponent("Brain");
		sim.getWorkspace().addWorkspaceComponent(networkComponent);
		sim.getDesktop().getDesktopComponent(networkComponent).getParentFrame().setBounds(x, y, width, height);
		netMap.put(networkComponent.getNetwork(), networkComponent);

		CreaturesBrain brain = new CreaturesBrain(networkComponent);

		brain.setUp();
		brainList.add(brain);
		return brain;
	}

}
