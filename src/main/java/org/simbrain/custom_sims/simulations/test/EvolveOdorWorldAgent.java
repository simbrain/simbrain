package org.simbrain.custom_sims.simulations.test;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.OdorWorldWrapper;
import org.simbrain.custom_sims.helper_classes.Simulation;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.neuron_update_rules.*;
import org.simbrain.util.Pair;
import org.simbrain.util.geneticalgorithm.Agent;
import org.simbrain.util.geneticalgorithm.Population;
import org.simbrain.util.geneticalgorithm.odorworld.NetworkEntityGenome;
import org.simbrain.util.math.SimbrainRandomizer;
import org.simbrain.util.neat.NetworkGenome;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.effectors.StraightMovement;
import org.simbrain.world.odorworld.effectors.Turning;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.ObjectSensor;
import org.simbrain.world.odorworld.sensors.Sensor;
import org.simbrain.world.odorworld.sensors.SmellSensor;

import java.util.List;

public class EvolveOdorWorldAgent extends RegisteredSimulation {

    /**
     * Default population size at each generation.
     */
    private int populationSize = 50;

    /**
     * The maximum number of generation.
     */
    private int maxGeneration = 150;

    /**
     * If fitness rises above this threshold before maxiterations is reached, simulation terminates.
     */
    private double fitnessThreshold = maxMoves/50;

    /**
     * How many times to iterate the simulation of the network in an environment
     */
    public static int maxMoves = 500;

    /**
     * Population of xor networks to evolve
     */
    private Population<NetworkEntityGenome, Pair<Network, OdorWorldEntity>> population;

    private int nextInnovationNumber;

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
        configuration.setNumInputs(1); // Must match the sensor count in base entity
        configuration.setNumOutputs(3);
        configuration.setAllowSelfConnection(true);
        configuration.setMaxNodes(10);
        configuration.setMinConnectionStrength(-.5);
        configuration.setMaxConnectionStrength(2);
        configuration.setNodeMaxBias(2);
        configuration.setMinNeuronActivation(-1);
        configuration.setMaxNeuronActivation(4);
        configuration.setRules(List.of(
               LinearRule.class, DecayRule.class, NakaRushtonRule.class,
               BinaryRule.class, ThreeValueRule.class));

        nextInnovationNumber = configuration.getNumInputs() + configuration.getNumOutputs();

        NetworkEntityGenome networkEntityGenome = new NetworkEntityGenome(configuration);
        networkEntityGenome.setNodeGeneInnovationNumberBaseSupplier(this::getNextInnovationNumber);
        OdorWorldEntity baseEntity = new OdorWorldEntity(null, EntityType.MOUSE);
        ObjectSensor defaultSensor = new ObjectSensor();
        defaultSensor.setId("default");
        baseEntity.addSensor(defaultSensor);
        networkEntityGenome.getEntityGenome().setBaseEntity(baseEntity);

        Agent<NetworkEntityGenome, Pair<Network, OdorWorldEntity>> prototype =
                new Agent<>(networkEntityGenome,
                        EvolveOdorWorldAgent::eval);
        population.populate(prototype);
    }

    public Integer getNextInnovationNumber() {
        return nextInnovationNumber;
    }

    @Override
    public void run() {

        // Evolve the pursuer
        init();
        evolve();

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Add winning network
        Network network = population.getFittestAgent().getPhenotype().getFirst();
        Agent<NetworkEntityGenome, Pair<Network, OdorWorldEntity>> agent = population.getFittestAgent();

        // Get the mouse from the network / odor world pair
        OdorWorldEntity mouse = agent.getPhenotype().getSecond();
        setUpWorkspace(sim, network, mouse);

        // TODO: When the mouse gets the cheese, respawn to a new location

    }

    static OdorWorldEntity setUpWorkspace(Simulation theSim, Network network, OdorWorldEntity mouse) {

        theSim.addNetwork(new NetworkComponent("Evolved Pursuer", network), 10, 491, 534, 10);

        // Set up odor world
        OdorWorldEntity cheese, flower, fish;
        OdorWorldWrapper worldBuilder;
        worldBuilder = theSim.addOdorWorldTMX(486, 14, "empty.tmx");
        worldBuilder.getWorld().setObjectsBlockMovement(false);
        mouse.setParentWorld(worldBuilder.getWorld());
        mouse.setLocation(300,300);
        cheese = worldBuilder.addEntity(100, 100, EntityType.SWISS);
        //cheese.getSmellSource().setDispersion(500);
        worldBuilder.getWorld().update();
        // Find the winning network / odor-world entity pair
        worldBuilder.getWorld().addEntity(mouse);

        //TODO: Why do the mice come here with effectors already?
        if(mouse.getEffectors().size() == 0){
            StraightMovement sm = new StraightMovement(mouse);
            sm.setAmount(2);
            mouse.addEffector(sm);
            mouse.addEffector(new Turning(mouse, Turning.LEFT));
            mouse.addEffector(new Turning(mouse, Turning.RIGHT));
        }

        // TODO
        // Create couplings
        //NeuronGroup outputs = (NeuronGroup) network.getGroupByLabel("outputs");
        //theSim.couple(outputs.getNeuron(0), mouse.getEffector("Move straight"));
        //theSim.couple(outputs.getNeuron(1), mouse.getEffector("Turn left"));
        //theSim.couple(outputs.getNeuron(2), mouse.getEffector("Turn right"));
        //outputs.setClamped(false);
        //NeuronGroup inputs = (NeuronGroup) network.getGroupByLabel("inputs");
        // TODO: Why not always at least 2 sensors?

        for (int i = 0; i < mouse.getSensors().size(); i++) {
            Sensor sensor = mouse.getSensors().get(i);
            if (sensor instanceof SmellSensor) {
                SmellSensor smellSensor = (SmellSensor) sensor;
            } else {

                // TODO
                //theSim.couple((ObjectSensor) sensor, inputs.getNeuron(i));
            }
        }

        return cheese;
    }


    public static Double eval(Agent<NetworkEntityGenome, Pair<Network, OdorWorldEntity>> agent) {

        // Set up the odor world
        Workspace workspace = new Workspace();
        Simulation sim = new Simulation(workspace);

        // Get current network and mouse
        Network network = agent.getPhenotype().getFirst();
        OdorWorldEntity mouse = agent.getPhenotype().getSecond();

        // Set up the sim
        OdorWorldEntity cheese = setUpWorkspace(sim,network, mouse);
        OdorWorld odorWorld = mouse.getParentWorld();

        // How many times the rat gets cheese!
        double score = 0;

        // Run the simulation
        for (int i = 0; i < maxMoves; i++) {

            workspace.simpleIterate();

            // Update score if the mouse is close enough to the cheese
            if (mouse.isInRadius(cheese, 28)) {
                score += 1;
                respawnCheese(odorWorld, mouse, cheese);
            }

        }

        // Partial score if mouse never gets cheese but gets closer
        //double distanceToCheese = mouse.getRadiusTo(cheese);
        //score += distanceToCheese < 48 ? (48 - distanceToCheese) / 48 : 0;

        return score;
    }

    private static void respawnCheese(OdorWorld world, OdorWorldEntity mouse, OdorWorldEntity cheese) {
        double x = SimbrainRandomizer.rand.nextDouble(0, world.getWidth());
        double y = SimbrainRandomizer.rand.nextDouble(0, world.getHeight());
        //x = mouse.getCenterX() + SimbrainRandomizer.rand.nextDouble(-64, 64);
        //x *= SimbrainRandomizer.rand.nextBoolean() ? 1 : -1;
        //if (x < 0) {
        //    x = 0;
        //} else if (x > odorWorld.getWidth() - cheese.getEntityType().getImageWidth()) {
        //    x = odorWorld.getWidth() - cheese.getEntityType().getImageWidth();
        //}
        //
        //y = mouse.getCenterY() + SimbrainRandomizer.rand.nextDouble(-64, 64);
        //y *= SimbrainRandomizer.rand.nextBoolean() ? 1 : -1;
        //if (y < 0) {
        //    y = 0;
        //} else if (y > odorWorld.getHeight() - cheese.getEntityType().getImageHeight()) {
        //    y = odorWorld.getHeight() - cheese.getEntityType().getImageHeight();
        //}
        cheese.setLocation(x, y);
    }

    /**
     * Run the simulation.
     */
    public void evolve() {
        for (int i = 0; i < maxGeneration; i++) {
            double bestFitness = population.computeNewFitness();
            nextInnovationNumber = 1 +
                    population.getAgentList()
                            .stream()
                            .map(Agent::getGenome)
                            .map(NetworkEntityGenome::getLastInnovationNumber)
                            .reduce(Integer::max)
                            .orElse(nextInnovationNumber);
            System.out.println(i + ", fitness = " + bestFitness);
            if (bestFitness > fitnessThreshold) {
                break;
            }
            population.replenish();
        }
    }

    @Override
    public String getSubmenuName() {
        return "Evolution";
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
