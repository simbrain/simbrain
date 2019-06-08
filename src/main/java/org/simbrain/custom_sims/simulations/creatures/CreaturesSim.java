package org.simbrain.custom_sims.simulations.creatures;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.NetworkWrapper;
import org.simbrain.custom_sims.helper_classes.OdorWorldBuilder;
import org.simbrain.custom_sims.helper_classes.Simulation;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.workspace.updater.UpdateActionAdapter;
import org.simbrain.world.odorworld.effectors.Speech;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.threedworld.entities.Entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * A simulation of an A-Life agent based off of the Creatures entertainment
 * software by Cyberlife Technology & Steve Grand, as seen in "Creatures:
 * Entertainment software agents with artificial life" (D. Cliff & S. Grand,
 * 1998)
 *
 * @author Sharai
 */
public class CreaturesSim extends RegisteredSimulation {

    /**
     * A list of creatures. Good for updating and maintaining multiple creatures.
     */
    private List<Creature> creatureList = new ArrayList<Creature>();

    private OdorWorldBuilder world;

    // TODO: Make this more flexible, editable, etc.
    public OdorWorldEntity toy;
    public OdorWorldEntity fish;
    public OdorWorldEntity cheese;
    public OdorWorldEntity poison;
    public OdorWorldEntity hazard;
    public OdorWorldEntity flower;

    // TODO: Is the best place to put this? Rename / cleanup as needed
    List<String> talkList = Arrays.asList("Wait", "Left", "Right", "Forward", "Backward", "Sleep", "Approach", "Ingest", "Look", "Smell", "Attack", "Play", "Mate", "Speak");
    float talkProb = .05f;
    Random talkRandomizer = new Random();
    OdorWorldEntity npc;

    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Add doc viewer
        // sim.addDocViewer(0, 0, 450, 600, "Doc",
        // "src/org/simbrain/custom_sims/simulations/creatures/CreaturesDoc.html");

        setUpWorld();

        // Create starting creatures
        Creature ron = createCreature(0, 0, 833, 629, "Ron");
        Creature eve = createCreature(0, 0, 233, 629, "Eve");
        eve.setAgentLocation(25, 25);

        // Make Eve a lion
        eve.setAgentSkin("Lion");

        // Testing chem reaction update log
        eve.injectChem("Pain", 8);
        eve.injectChem("Endorphin", 10);

        setUpNPC();

        // Create update action
        sim.getWorkspace().addUpdateAction(new UpdateActionAdapter("Update Creatures Sim") {
            @Override
            public void invoke() {
                updateCreaturesSim();
            }
        });

    }

    private void setUpNPC() {
        // Create a 'non-player character' that talks randomly
        npc = world.addEntity(350, 250, EntityType.COW);
        npc.setName("Cow");
        npc.setSmellSource(new SmellSource(new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0}));

        // Give npc speech effectors
        npc.addEffector(new Speech(npc, "Wait", 1));
        npc.addEffector(new Speech(npc, "Left", 1));
        npc.addEffector(new Speech(npc, "Right", 1));
        npc.addEffector(new Speech(npc, "Forward", 1));
        npc.addEffector(new Speech(npc, "Backward", 1));
        npc.addEffector(new Speech(npc, "Sleep", 1));
        npc.addEffector(new Speech(npc, "Approach", 1));
        npc.addEffector(new Speech(npc, "Ingest", 1));
        npc.addEffector(new Speech(npc, "Look", 1));
        npc.addEffector(new Speech(npc, "Smell", 1));
        npc.addEffector(new Speech(npc, "Attack", 1));
        npc.addEffector(new Speech(npc, "Play", 1));
        npc.addEffector(new Speech(npc, "Mate", 1));
        npc.addEffector(new Speech(npc, "Speak", 1));
    }

    private void setUpWorld() {

        // Create odor world
        world = sim.addOdorWorld(601, 0, 456, 597, "World");
        world.getWorld().setObjectsBlockMovement(false);

        // TODO: May be able to remove smell sources below since we are new using object sensors

        // Create static odor world entities
        toy = world.addEntity(395, 590, EntityType.BELL);
        toy.setName("Bell");
        toy.setId("Toy");
        toy.setSmellSource(new SmellSource(new double[]{5, 0, 0, 0, 0, 0, 0}));

        fish = world.addEntity(140, 165, EntityType.FISH);
        fish.setName("Fish");
        fish.setId("Fish");
        fish.setSmellSource(new SmellSource(new double[]{0, 5, 0, 0, 0, 0, 0}));

        cheese = world.addEntity(200, 200, EntityType.SWISS);
        cheese.setName("Fish");
        cheese.setId("Cheese");
        cheese.setSmellSource(new SmellSource(new double[]{0, 0, 5, 0, 0, 0, 0}));

        poison = world.addEntity(320, 20, EntityType.POISON);
        poison.setName("Poison");
        poison.setId("Poison");
        poison.setSmellSource(new SmellSource(new double[]{0, 0, 0, 5, 0, 0, 0}));

        hazard = world.addEntity(25, 200, EntityType.CANDLE);
        hazard.setName("Candle");
        hazard.setId("Hazard");
        hazard.setSmellSource(new SmellSource(new double[]{0, 0, 0, 0, 5, 0, 0}));

        flower = world.createEntity(200, 100, EntityType.PANSY);
        flower.setName("Pansy");
        flower.setId("Flower");
        flower.setSmellSource(new SmellSource(new double[]{0, 0, 0, 0, 0, 5, 0}));
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
            // TODO: The speech bubble disappears too quickly when running the
            // simulation
            // without going step-by-step. How can we make it linger?
            Speech effector = (Speech) npc.getEffector("Say: \"" + talkList.get(talkRandomizer.nextInt(talkList.size())) + "\"");
            effector.setAmount(10);
        }
    }

    /**
     * Creates a new creature.
     *
     * @param x      X position of brain network.
     * @param y      Y position of brain network.
     * @param width  Width of brain network.
     * @param height Height of brain network.
     * @param name   Name of creature.
     * @return A new creature.
     */
    public Creature createCreature(int x, int y, int width, int height, String name) {

        NetworkWrapper net = sim.addNetwork2(x, y, 600, 600, name + "'s Brain");

        // TODO: Below not working quite right because the network has not
        // finished
        // being created when the next two calls are made
        // net.getNetworkPanel().setAutoZoomMode(false);
        // net.getNetworkPanel().zoomToFitPage(true);

        OdorWorldEntity agent = world.createEntity(250, 250, EntityType.MOUSE);
        Creature creature = new Creature(this, name, net, agent);
        creatureList.add(creature);

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
