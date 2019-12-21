package org.simbrain.custom_sims.simulations.test;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.custom_sims.helper_classes.OdorWorldWrapper;
import org.simbrain.custom_sims.helper_classes.Simulation;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.network.neuron_update_rules.*;
import org.simbrain.util.geneticalgorithm.Agent;
import org.simbrain.util.geneticalgorithm.Population;
import org.simbrain.util.math.SimbrainRandomizer;
import org.simbrain.util.neat.NetworkGenome;
import org.simbrain.util.neat.gui.ProgressWindow;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.ObjectSensor;
import org.simbrain.world.odorworld.sensors.Sensor;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class EvolvePursuer extends RegisteredSimulation {

    /**
     * Default population size at each generation.
     */
    private int populationSize = 500;

    /**
     * The maximum number of generation.
     */
    private int maxGeneration = 50;

    /**
     * If fitness rises above this threshold before maxiterations is reached, simulation terminates.
     */
    private double fitnessThreshold = 20;

    /**
     * How many times to iterate the simulation of the network in an environment
     */
    public static int maxMoves = 400 ;

    /**
     * For progress bar.
     */
    public List<NewGenerationListener> newGenerationListeners = new ArrayList<>();

    /**
     * Population of xor networks to evolve
     */
    private Population<NetworkGenome, Network> population;

    /**
     * Construct sim
     */
    public EvolvePursuer() {
        super();
    }

    /**
     * @param desktop
     */
    public EvolvePursuer(SimbrainDesktop desktop) {
        super(desktop);
    }

    /**
     * Initialize the population of networks.
     */
    public void init() {
        population = new Population<>(this.populationSize);
        population.setEliminationRatio(.8);
        NetworkGenome.Configuration configuration = new NetworkGenome.Configuration();
        configuration.setNumInputs(4); // Must match the mouse's sensor count (see setUpWorkspace)
        configuration.setNumOutputs(3); // Must match the mouse's effector count
        configuration.setAllowSelfConnection(true);
        configuration.setMaxNodes(25);
        configuration.setMinConnectionStrength(-100);
        configuration.setMaxConnectionStrength(100);
        configuration.setNodeMaxBias(1);
        configuration.setMinNeuronActivation(-10);
        configuration.setMaxNeuronActivation(10);
        configuration.setRules(List.of(DecayRule.class, NakaRushtonRule.class, BinaryRule.class,
                LinearRule.class, SigmoidalRule.class, IACRule.class));

        NetworkGenome networkGenome = new NetworkGenome(configuration);

        Agent<NetworkGenome, Network> prototype =
                new Agent<>(networkGenome,
                        EvolvePursuer::eval);
        population.populate(prototype);
    }

    @Override
    public void run() {

        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Evolve the pursuer
        init();

        final ProgressWindow progressWindow = new ProgressWindow(maxGeneration);

        //Where member variables are declared:
        if (!GraphicsEnvironment.isHeadless()) {

            newGenerationListeners.add((generation, score) -> {
                System.out.printf("[%d] Fitness: %.2f\n", generation, score);
                SwingUtilities.invokeLater(() -> {
                    progressWindow.getProgressBar().setValue(generation);
                    progressWindow.getFitnessScore().setText("Fitness Score: " + score);
                });
            });

        }

        CompletableFuture.supplyAsync(this::evolve)
                .thenRun(() -> {
                    SwingUtilities.invokeLater(() -> {
                        // Add winning network
                        Network network = population.getFittestAgent().getPhenotype();

                        // Get the mouse from the network / odor world pair
                        setUpWorkspace(sim, network);

                        if (!GraphicsEnvironment.isHeadless()) {
                            progressWindow.close();
                        }

                        simulationCompleted();


                    });
                });


    }

    /**
     * Set a network up in a workspace and return the mouse
     */
    static OdorWorldEntity setUpWorkspace(Simulation theSim, Network network) {

        theSim.addNetwork(new NetworkComponent("Evolved Pursuer", network), 10, 491, 534, 10);

        // Set up odor world
        OdorWorldEntity cheese, flower, poison;
        OdorWorldWrapper worldBuilder;
        worldBuilder = theSim.addOdorWorldTMX(486, 14, "empty.tmx");
        worldBuilder.getWorld().setObjectsBlockMovement(false);
        OdorWorldEntity mouse = worldBuilder.getWorld().addAgent();

        // mouse.setParentWorld(worldBuilder.getWorld());
        mouse.setLocation(100, 100);
        worldBuilder.getWorld().addEntity(mouse);

        cheese = worldBuilder.addEntity(300, 150, EntityType.SWISS);
        cheese.getSmellSource().setDispersion(300);
        cheese.setEdible(true);

        //flower = worldBuilder.addEntity(150, 200, EntityType.FLOWER);
        //flower.getSmellSource().setDispersion(300);
        //flower.setEdible(true);

        poison = worldBuilder.addEntity(300, 300, EntityType.POISON);
        poison.getSmellSource().setDispersion(300);
        poison.setEdible(true);

        worldBuilder.getWorld().update();
        // Find the winning network

        cheese.randomizeLocationInRange(50);
        //flower.randomizeLocationInRange(50);
        poison.randomizeLocationInRange(50);

        // Create couplings
        NeuronGroup outputs = null; // TODO: (NeuronGroup) network.getGroupByLabel("outputs");
        theSim.couple(outputs.getNeuron(0), mouse.getEffector("Move straight"));
        theSim.couple(outputs.getNeuron(1), mouse.getEffector("Turn left"));
        theSim.couple(outputs.getNeuron(2), mouse.getEffector("Turn right"));
        outputs.getNeuron(0).setLabel("Forward");
        outputs.getNeuron(1).setLabel("Left");
        outputs.getNeuron(2).setLabel("Right");
        outputs.setClamped(false);
        NeuronGroup inputs = null; // todo (NeuronGroup) network.getGroupByLabel("inputs");
        LineLayout layout = (LineLayout) inputs.getLayout();
        layout.setSpacing(100);
        inputs.applyLayout();

        mouse.clearSensors();

        mouse.addLeftRightSensors(EntityType.SWISS, 150);
        //mouse.addLeftRightSensors(EntityType.FLOWER, 150);
        mouse.addLeftRightSensors(EntityType.POISON, 150);


        for (int i = 0; i < mouse.getSensors().size(); i++) {
            Sensor sensor = mouse.getSensors().get(i);
            theSim.couple((ObjectSensor) sensor, inputs.getNeuron(i));
            inputs.getNeuron(i).setLabel(sensor.getLabel().replaceFirst(" Detector", ""));
        }

        return mouse;
    }

    public static Double eval(Agent<NetworkGenome, Network> agent) {

        // Set up the odor world
        Simulation sim = new Simulation(new Workspace());

        // Get current network and mouse
        Network network = agent.getPhenotype();

        // Set up the sim
        OdorWorldEntity mouse = setUpWorkspace(sim,network);

        // How many times the rat gets cheese!
        AtomicInteger score = new AtomicInteger();

        // when the mouse touches other entity
        mouse.onCollide(other -> {
            if (other.getEntityType() == EntityType.SWISS) {
                score.incrementAndGet();
            }
            //else if (other.getEntityType() == EntityType.FLOWER) {
            //    score.accumulateAndGet(-2, Integer::sum); // getting flower lower the score by 2
            else if (other.getEntityType() == EntityType.POISON) {
                score.accumulateAndGet(-100, Integer::sum); // getting flower lower the score by 2
                //agent.kill(); // poison kills the mouse
            }
        });

        // Run the simulation
        for (int i = 0; i < maxMoves && agent.isAlive(); i++) {
            sim.getWorkspace().simpleIterate();
        }

        // double nodeSizePenalty = Integer.max(agent.getGenome().getNodeGenes().getGenes().size() - 16, 0) / 100.0;
        double nodeSizePenalty = 0;

        // extra energy does not count towards fitness, and scale down to fit score better
        double energyPenalty = Double.max(0, mouse.getEnergyLevel())/10_000;

        return score.get() - nodeSizePenalty - energyPenalty;
    }

    /**
     * Run the simulation.
     */
    public double evolve() {
        double finalFitness = 0;
        double bestOverallFitness = Double.MIN_VALUE;
        for (int i = 0; i < maxGeneration; i++) {
            double bestOfGeneration = population.computeNewFitness();
            finalFitness = bestOfGeneration;
            // System.out.println(i + ", fitness = " + bestFitness);
            final int generation = i;
            newGenerationListeners.forEach(f -> f.run(generation, bestOfGeneration));
            if (bestOfGeneration > bestOverallFitness) {
                bestOverallFitness = bestOfGeneration;
                Simulation sim = new Simulation(new Workspace());
                setUpWorkspace(sim, population.getFittestAgent().getPhenotype());
                sim.saveWorkspace("Winner.zip");
            }
            if (bestOfGeneration > fitnessThreshold) {
                break;
            }
            population.replenish();
        }
        System.out.println("Best fitness:" + bestOverallFitness);
        return finalFitness;
    }


    @Override
    public String getSubmenuName() {
        return "Evolution";
    }

    @Override
    public String getName() {
        return "Evolve Mouse Pursuer";
    }

    @Override
    public RegisteredSimulation instantiate(SimbrainDesktop desktop) {
        return new EvolvePursuer(desktop);
    }

    interface NewGenerationListener {
        void run(int Generation, double bestFitness);
    }
}
