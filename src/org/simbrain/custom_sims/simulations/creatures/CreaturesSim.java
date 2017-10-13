package org.simbrain.custom_sims.simulations.creatures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.custom_sims.helper_classes.OdorWorldBuilder;
import org.simbrain.custom_sims.helper_classes.Simulation;
import org.simbrain.util.environment.SmellSource;
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
	public OdorWorldEntity toy;
	public OdorWorldEntity fish;
	public OdorWorldEntity cheese;
	public OdorWorldEntity poison;
	public OdorWorldEntity hazard;
	public OdorWorldEntity flower;
	
	//TODO: Is the best place to put this?   Rename / cleanup as needed
	List<String> nounList = Arrays.asList("Cheese", "Fish");
    float talkProb = .05f;
    Random talkRandomizer = new Random();

	@Override
	public void run() {

		// Clear workspace
		sim.getWorkspace().clearWorkspace();

		// Add doc viewer
		// sim.addDocViewer(0, 0, 450, 600, "Doc",
		// "src/org/simbrain/custom_sims/simulations/creatures/CreaturesDoc.html");

		// Create odor world
		world = sim.addOdorWorld(601, 10, 456, 597, "World");
		world.getWorld().setObjectsBlockMovement(false);

		// Create static odor world entities
		toy = world.addEntity(395, 590, "Bell.gif");
		toy.setSmellSource(new SmellSource(new double[] { 5, 0, 0, 0, 0, 0, 0 }));

		fish = world.addEntity(140, 165, "Fish.gif");
		fish.setSmellSource(new SmellSource(new double[] { 0, 5, 0, 0, 0, 0, 0 }));

		cheese = world.addEntity(200, 200, "Swiss.gif");
		cheese.setSmellSource(new SmellSource(new double[] { 0, 0, 5, 0, 0, 0, 0 }));

		poison = world.addEntity(320, 20, "Poison.gif");
		poison.setSmellSource(new SmellSource(new double[] { 0, 0, 0, 5, 0, 0, 0 }));

		hazard = world.addEntity(25, 200, "Candle.png");
		hazard.setSmellSource(new SmellSource(new double[] { 0, 0, 0, 0, 5, 0, 0 }));

		flower = world.addEntity(200, 100, "Pansy.gif");
		flower.setSmellSource(new SmellSource(new double[] { 0, 0, 0, 0, 0, 5, 0 }));

		// Create starting creature.
		Creature ron = createCreature(0, 0, 833, 629, "Ron");
		
		// Create a "non-player character' that talks randomly

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
		
		updateNPC();		

		world.getOdorWorldComponent().update();
	}

    /**
     * Update the "non-player character".
     */
    private void updateNPC() {
        if (Math.random() < talkProb) {
            //TODO: Make this be the npc talking using its speech effector
            System.out.println(nounList.get(talkRandomizer.nextInt(nounList.size())));
        }
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
		
	    //TODO: Below not working quite right because the network has not finished
	    // being created when the next two calls are made
	    net.getNetworkPanel(sim).setAutoZoomMode(false);
        //net.getNetworkPanel(sim).zoomToFitPage(true); 

		RotatingEntity agent = world.addAgent(250, 250, "Mouse");
		Creature creature = new Creature(this, name, net, agent);
		creatureList.add(creature);
		// agent.setHeading(90);

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
