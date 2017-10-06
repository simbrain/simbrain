package org.simbrain.custom_sims.simulations.creatures;

import java.util.ArrayList;
import java.util.List;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.custom_sims.helper_classes.OdorWorldBuilder;
import org.simbrain.custom_sims.helper_classes.Simulation;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.workspace.updater.UpdateActionAdapter;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;

/**
 * A simulation of an A-Life agent based off of the Creatures entertainment
 * software by Cyberlife Technology & Steve Grand, as seen in "Creatures:
 * Entertainment software agents with artificial life" (D. Cliff & S. Grand,
 * 1998)
 *
 * @author Sharai
 *
 */
public class CreaturesSim extends RegisteredSimulation {

	/**
	 * A list of creatures. Good for updating and maintaining multiple creatures.
	 */
	private List<Creature> creatureList = new ArrayList<Creature>();

	private OdorWorldBuilder world;
	
	// TODO: Temp!
	public OdorWorldEntity cheese;

	@Override
	public void run() {

		// Clear workspace
		sim.getWorkspace().clearWorkspace();

		// Add doc viewer
		sim.addDocViewer(0, 0, 450, 600, "Doc", "src/org/simbrain/custom_sims/simulations/creatures/CreaturesDoc.html");

		// Create odor world
		world = sim.addOdorWorld(1052, 0, 600, 600, "World");
		cheese = world.addEntity(200, 200, "Swiss.gif");

		// Create starting creature.
		Creature ron = createCreature(451, 0, 600, 600, "Ron");

		// Create update action
		// TODO: Possibly clear all update actions and then custom populate
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
	// TODO: Should we have each world agent update after everyone's brains are
	// updated (as we do now), or should we update each agent right after their
	// brain does?
	void updateCreaturesSim() {
		for (Creature c : creatureList) {
			c.update();
		}

		world.getOdorWorldComponent().update();
	}

	/**
	 * Creates a new creature.
	 *
	 * @param x
	 *            X position of brain network.
	 * @param y
	 *            Y position of brain network.
	 * @param width
	 *            Width of brain network.
	 * @param height
	 *            Height of brain network.
	 * @param name
	 *            Name of creature.
	 * @return A new creature.
	 */
	public Creature createCreature(int x, int y, int width, int height, String name) {
		NetBuilder net = sim.addNetwork(x, y, 600, 600, name + "'s Brain");
		RotatingEntity agent = world.addAgent(250, 250, "Mouse");
		Creature creature = new Creature(this, name, net, agent);
        creatureList.add(creature);
        //agent.setHeading(90);
        
        return creature;
	}

	/**
	 * Constructor.
	 *
	 * @param desktop
	 */
	public CreaturesSim(SimbrainDesktop desktop) {
		super(desktop);
	}

	public CreaturesSim() {
		super();
	}

	/**
	 * Runs the constructor for the simulation.
	 */
	@Override
	public CreaturesSim instantiate(SimbrainDesktop desktop) {
		return new CreaturesSim(desktop);
	}

	// Accessor methods below this point
	@Override
	public String getName() {
		return "Creatures";
	}

	public List<Creature> getCreatureList() {
		return creatureList;
	}
	
	public Simulation getSim() {
		return sim;
	}

}
