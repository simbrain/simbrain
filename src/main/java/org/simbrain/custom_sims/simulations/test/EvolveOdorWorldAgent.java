package org.simbrain.custom_sims.simulations.test;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.OdorWorldWrapper;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.util.Pair;
import org.simbrain.util.geneticalgorithm.Agent;
import org.simbrain.util.geneticalgorithm.Population;
import org.simbrain.util.geneticalgorithm.odorworld.NetworkEntityGenome;
import org.simbrain.util.math.SimbrainRandomizer;
import org.simbrain.util.neat.NetworkGenome;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.world.odorworld.effectors.StraightMovement;
import org.simbrain.world.odorworld.effectors.Turning;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.ObjectSensor;

public class EvolveOdorWorldAgent extends RegisteredSimulation {

    /**
     * Default population size at each generation.
     */
    private int populationSize = 100;

    /**
     * The maximum number of generation.
     */
    private int maxIterations = 200;

    /**
     * If fitness rises above this threshold before maxiterations is reached, simulation terminates.
     */
    private double fitnessThreshold = 10;

    /**
     * How many times to iterate the simulation of the network in an environment
     */
    public static int maxMoves = 500;

    /**
     * Population of xor networks to evolve
     */
    private Population<NetworkEntityGenome, Pair<Network, OdorWorldEntity>> population;


    // Odor world stuff
    OdorWorldEntity mouse;
    OdorWorldEntity cheese, flower, fish;
    OdorWorldWrapper worldBuilder;

    /**
     * Construct sim
     */
    public EvolveOdorWorldAgent() {
        super();
    }

    /**
     * @param desktop
     */
    public EvolveOdorWorldAgent(SimbrainDesktop desktop) {
        super(desktop);
    }


    /**
     * Initialize the population of networks.
     */
    public void init() {
        population = new Population<>(this.populationSize);
        //population.setEliminationRatio(.8);// TODO: causes problems
        NetworkGenome.Configuration configuration = new NetworkGenome.Configuration();
        configuration.setNumInputs(3);
        configuration.setNumOutputs(3);
        configuration.setAllowSelfConnection(true);
        configuration.setMaxNodes(10);
        configuration.setMaxConnectionStrength(1);
        configuration.setNodeMaxBias(2);
        configuration.setMaxNeuronActivation(10);

        Agent<NetworkEntityGenome, Pair<Network, OdorWorldEntity>> prototype =
                new Agent<>(new NetworkEntityGenome(configuration),
                        EvolveOdorWorldAgent::eval);
        population.populate(prototype);
    }

    @Override
    public void run() {

        // Evolve the pursuer
        init();
        evolve();

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Add winning network
        Network winner = population.getFittestAgent().getPhenotype().getKey();
        sim.addNetwork(new NetworkComponent("Evolved Pursuer", winner), 10, 491, 534, 10);

        // Add odor world
        createOdorWorld();

        // Find the winning network / odorworld entity pair
        worldBuilder.getWorld().addEntity(population.getFittestAgent().getPhenotype().getValue());
        Agent<NetworkEntityGenome, Pair<Network, OdorWorldEntity>> agent = population.getFittestAgent();

        // Get the mouse from the network / odor world pair
        mouse = agent.getPhenotype().getValue();
        mouse.setParentWorld(worldBuilder.getWorld());

        //mouse.addEffector(new StraightMovement(mouse));
        //mouse.addEffector(new Turning(mouse, Turning.LEFT));
        //mouse.addEffector(new Turning(mouse, Turning.RIGHT));

        // Create couplings
        NeuronGroup outputs = (NeuronGroup) winner.getGroupByLabel("outputs");
        sim.couple(outputs.getNeuron(0), mouse.getEffector("Move straight"));
        sim.couple(outputs.getNeuron(1), mouse.getEffector("Turn left"));
        sim.couple(outputs.getNeuron(2), mouse.getEffector("Turn right"));
        outputs.setClamped(false);
        NeuronGroup inputs = (NeuronGroup) winner.getGroupByLabel("inputs");
        sim.couple((ObjectSensor) mouse.getSensors().get(0), inputs.getNeuron(0));
        sim.couple((ObjectSensor) mouse.getSensors().get(1), inputs.getNeuron(1));
        sim.couple((ObjectSensor) mouse.getSensors().get(2), inputs.getNeuron(2));
        inputs.setClamped(false);

        // TODO: When the mouse gets the cheese, respawn to a new location

    }

    private void createOdorWorld() {

        worldBuilder = sim.addOdorWorldTMX(486, 14, 472, 516, "empty.tmx");
        worldBuilder.getWorld().setObjectsBlockMovement(false);

        cheese = worldBuilder.addEntity(100, 100, EntityType.SWISS);
        cheese.getSmellSource().setDispersion(300);
        worldBuilder.getWorld().update();

    }


    public static Double eval(Agent<NetworkEntityGenome, Pair<Network, OdorWorldEntity>> agent) {

        // How many cheeses the agent eats
        double score = 0;

        // Set up the odor world
        OdorWorldComponent odorWorldComponent = new OdorWorldComponent("agent world");
        OdorWorld odorWorld = odorWorldComponent.getWorld();
        OdorWorldEntity mouse = agent.getPhenotype().getValue();
        mouse.setParentWorld(odorWorld);
        mouse.addEffector(new StraightMovement(mouse)); // To be move to odorworldentitygenome
        mouse.addEffector(new Turning(mouse, Turning.LEFT));
        mouse.addEffector(new Turning(mouse, Turning.RIGHT));
        mouse.setLocation(odorWorld.getWidth() / 2, odorWorld.getHeight() / 2);

        // Add cheese
        OdorWorldEntity cheese = odorWorld.addEntity();
        cheese.getSmellSource().setDispersion(300);

        // Randomize location of cheese.
        respawnCheese(odorWorld, mouse, cheese);

        // Run the simulation
        for (int i = 0; i < maxMoves; i++) {

            // Update the sensors
            NeuronGroup inputs = (NeuronGroup) agent.getPhenotype().getKey().getGroupByLabel("inputs");
            for (int j = 0; j < inputs.size() && j < mouse.getSensors().size(); j++) {
                ObjectSensor os = (ObjectSensor) mouse.getSensors().get(j);
                inputs.getNeuronList().get(j).forceSetActivation(os.getCurrentValue());
            }

            // Update the network
            agent.getPhenotype().getKey().update();

            // Move the mouse
            NeuronGroup outputs = (NeuronGroup) agent.getPhenotype().getKey().getGroupByLabel("outputs");
            ((StraightMovement) mouse.getEffector("Move Straight")).setAmount(outputs.getActivations()[0]);
            ((Turning) mouse.getEffector("Turn Left")).setAmount(outputs.getActivations()[1]);
            ((Turning) mouse.getEffector("Turn Right")).setAmount(outputs.getActivations()[2]);

            odorWorld.update();

            // Update score if the mouse is close enough to the cheese
            //System.out.println(mouse.getHeading());
            if (mouse.isInRadius(cheese, 28)) {
                score += 1;
                respawnCheese(odorWorld, mouse, cheese);
            }

        }

        // Partial score if mouse never gets cheese but gets closer
        double distanceToCheese = mouse.getRadiusTo(cheese);
        score += distanceToCheese < 48 ? (48 - distanceToCheese) / 48 : 0;

        return score;
    }

    private static void respawnCheese(OdorWorld odorWorld,  OdorWorldEntity mouse,  OdorWorldEntity cheese) {
        double x;
        double y;
        x = mouse.getCenterX() +  SimbrainRandomizer.rand.nextDouble(-64, 64);
        x *=  SimbrainRandomizer.rand.nextBoolean() ? 1 : -1;
        if (x < 0) {
            x = 0;
        } else if (x > odorWorld.getWidth() - cheese.getEntityType().getImageWidth()) {
            x = odorWorld.getWidth() - cheese.getEntityType().getImageWidth();
        }

        y = mouse.getCenterY() +  SimbrainRandomizer.rand.nextDouble(-64, 64);
        y *=  SimbrainRandomizer.rand.nextBoolean() ? 1 : -1;
        if (y < 0) {
            y = 0;
        } else if (y > odorWorld.getHeight() - cheese.getEntityType().getImageHeight()) {
            y = odorWorld.getHeight() - cheese.getEntityType().getImageHeight();
        }
        cheese.setLocation(x, y);
    }

    /**
     * Run the simulation.
     */
    public void evolve() {
        for (int i = 0; i < maxIterations; i++) {
            double bestFitness = population.computeNewFitness();
            System.out.println(i + ", fitness = " + bestFitness);
            if (bestFitness > fitnessThreshold) {
                break;
            }
            population.replenish();
        }
    }

    @Override
    public String getName() {
        return "Evolve Mouse";
    }

    @Override
    public RegisteredSimulation instantiate(SimbrainDesktop desktop) {
        return new EvolveOdorWorldAgent(desktop);
    }
}
