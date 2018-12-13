package org.simbrain.custom_sims.simulations.neat;

import org.simbrain.custom_sims.simulations.neat.util.NEATRandomizer;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.trainers.TrainingSet;
import org.simbrain.network.util.NetworkLayoutManager;
import org.simbrain.network.util.NetworkLayoutManager.*;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.math.DecayFunctions.GaussianDecayFunction;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.effectors.StraightMovement;
import org.simbrain.world.odorworld.effectors.Turning;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.SmellSensor;

import javax.swing.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class Test {

    public static void evaluationMethod(Agent agent) {
        // initializing fitness score
        // TODO: it is easy to forget to initialize fitness. make default fitness configurable in pool.
        agent.setFitness(0);

        Network n = agent.getNet();

        // Get MSE
        NeuronGroup inputGroup = agent.getGenome().getInputNg();
        NeuronGroup outputGroup = agent.getGenome().getOutputNg();

        TrainingSet ts = new TrainingSet(
            new double[][] {{0, 0}, {0, 1}, {1, 0}, {1, 1}},
            new double[][] {{0}, {1}, {1}, {0}});

        // Create table of output values, one for each input
        double outputs[][] = new double[4][1];
        int i = 0;
        for (double[] input : ts.getInputData()) {
            inputGroup.forceSetActivations(input);
            for (int l = 0; l < 100; l++) {
                n.bufferedUpdateAllNeurons();
            }
            n.update();
            outputs[i] = outputGroup.getActivations();
        }


        //TODO Below not working
//        // Compute MSE and use it to set agent's fitness
//        double err = TrainingSet.getMSE(ts, outputs);
//        double fitness = agent.getFitness() - (err * err);
//        agent.setFitness(fitness);
//        System.out.println(TrainingSet.getMSE(ts, outputs));
//        agent.setFitness(TrainingSet.getMSE(ts, outputs));
//

        for (int j = 0; j < 2; j++) {
            for (int k = 0; k < 2; k++) {
                inputGroup.getNeuron(0).forceSetActivation(j);
                inputGroup.getNeuron(1).forceSetActivation(k);
                for (int l = 0; l < 100; l++) {
                    n.bufferedUpdateAllNeurons();
                }
                n.update();

                // calculating sse
                for (int l = 0; l < 5; l++) {
                    double err = (j ^ k) - outputGroup.getNeuron(0).getActivation();
                    double fitness = agent.getFitness() - (err * err);
                    agent.setFitness(fitness);
                    n.update();
                }
            }
        }
    }

    public static void worldTestingIteration(Agent agent, OdorWorldEntity cheese, OdorWorldEntity newEntity) {
        Network n = agent.getNet();
        OdorWorld w = agent.getWorld();

        // set properties of output neurons
        for (Neuron nr : n.getNeuronGroups().get(1).getNeuronList()) {
            nr.setUpdateRule(new LinearRule());
            nr.setUpperBound(3);
            nr.setLowerBound(0);
        }

        // 8 is the stimulus vector dimension this simulation will be using

//        // Theses for loops are substitutes to coupling.
//        // input neuron 0 - 7 receive coupling from mouse 1 left sensor
//        for (int j = 0; j < 8; j++) {
//            n.getNeuronGroups().get(0).getNeuron(j).forceSetActivation(
//                    ((SmellSensor) newEntity.getSensor("Smell-Left")).getCurrentValue(j)
//            );
//        }
//
//        // input neuron 8 - 15 receive coupling from mouse 1 left sensor
//        for (int j = 0; j < 8; j++) {
//            n.getNeuronGroups().get(0).getNeuron(j + 8).forceSetActivation(
//                    ((SmellSensor) newEntity.getSensor("Smell-Center")).getCurrentValue(j)
//            );
//        }
//
//        // input neuron 16 - 24 receive coupling from mouse 1 left sensor
//        for (int j = 0; j < 8; j++) {
//            n.getNeuronGroups().get(0).getNeuron(j + 16).forceSetActivation(
//                    ((SmellSensor) newEntity.getSensor("Smell-Right")).getCurrentValue(j)
//            );
//        }

        // "coupling" output node 3 - 8 to the smell stimulus vector of mouse 1
        double[] newStim = new double[8];
        for (int i = 3; i < 9; i++) {
            newStim[i - 1] = n.getNeuronGroups().get(1).getNeuron(i).getActivation();
        }
        newEntity.getSmellSource().setStimulusVector(newStim);

        // update network
        n.bufferedUpdateAllNeurons();
        n.update();

        // coupling output node 0 - 2 to move straight, turn left (maybe, not sure, check later), and turn right effector
        ((StraightMovement) newEntity.getEffectors().get(0)).setAmount(n.getNeuronGroups().get(1).getNeuron(0).getActivation());
        ((Turning) newEntity.getEffectors().get(1)).addAmount(n.getNeuronGroups().get(1).getNeuron(1).getActivation());
        ((Turning) newEntity.getEffectors().get(2)).addAmount(n.getNeuronGroups().get(1).getNeuron(2).getActivation());

        // update world
        w.update();

        // compute fitness score

        // find distance between mouse 1 and cheese
        double dx = newEntity.getCenterX() - cheese.getCenterX();
        double dy = newEntity.getCenterY() - cheese.getCenterY();

        // if distance is less than \sqrt(1000) (about 31), which means the mouse got the cheese
        if (dx * dx + dy * dy < 1000) {
//            Random rand = new Random(1L);
//            cheese.setLocation(
//                    Math.abs(450 - (cheese.getCenterX() + (rand.nextBoolean() ? 1 : -1) * (rand.nextDouble() + 0.2) * 20)),
//                    Math.abs(450 - (cheese.getCenterY() + (rand.nextBoolean() ? 1 : -1) * (rand.nextDouble() + 0.2) * 20)));
//            agent.setFitness(agent.getFitness() + 1);

            // move the cheese to a new location. in this case, base on 2 sin function
            cheese.setLocation(450 / 2 + 450 / 2 * Math.sin(6 * agent.getFitness() * Math.PI / 47),
                    450 / 2 + 60 * Math.sin((1 + agent.getFitness()) * Math.PI / 4));
            agent.setFitness(agent.getFitness() + 1);
        }

        // deduct energy penalty from fitness score
        double energyPenalty = n.getNeuronGroups().get(1).getNeuron(0).getActivation() / 128 / 400  // movement
                + n.getNeuronGroups().get(1).getNeuron(0).getActivation() / 1 / 400  // rotating
                + n.getNeuronGroups().get(1).getNeuron(2).getActivation() / 8 / 400; // rotating
        agent.setFitness(agent.getFitness() - energyPenalty);
//        agent.setFitness(agent.getFitness() + (1000 - dx * dx + dy * dy) / 10000 / 400);
    }

    // same as the previous one but with 2 mice
    public static void worldTestingIteration2(Agent agent, OdorWorldEntity cheese, OdorWorldEntity newEntity, OdorWorldEntity pinnedMouse) {
        Network n = agent.getNet();
//        Network n2 = n.copy();
        Network n2 = agent.getGenome().buildNetwork();  // ask genome to build another identical network...
        OdorWorld w = agent.getWorld();

        for (Neuron nr : n.getNeuronGroups().get(1).getNeuronList()) {
            nr.setUpdateRule(new LinearRule());
            nr.setUpperBound(3);
            nr.setLowerBound(0);
        }

        for (Neuron nr : n2.getNeuronGroups().get(1).getNeuronList()) {
            nr.setUpdateRule(new LinearRule());
            nr.setUpperBound(3);
            nr.setLowerBound(0);
        }

//        // 8 is the stimulus vector dimension this simulation will be using
//
//        for (int j = 0; j < 8; j++) {
//            n.getNeuronGroups().get(0).getNeuron(j).forceSetActivation(
//                    ((SmellSensor) newEntity.getSensor("Smell-Left")).getCurrentValue(j)
//            );
//        }
//
//        for (int j = 0; j < 8; j++) {
//            n.getNeuronGroups().get(0).getNeuron(j + 8).forceSetActivation(
//                    ((SmellSensor) newEntity.getSensor("Smell-Center")).getCurrentValue(j)
//            );
//        }
//
//        for (int j = 0; j < 8; j++) {
//            n.getNeuronGroups().get(0).getNeuron(j + 16).forceSetActivation(
//                    ((SmellSensor) newEntity.getSensor("Smell-Right")).getCurrentValue(j)
//            );
//        }
//
//        // n2
//
//        for (int j = 0; j < 8; j++) {
//            n2.getNeuronGroups().get(0).getNeuron(j).forceSetActivation(
//                    ((SmellSensor) pinnedMouse.getSensor("Smell-Left")).getCurrentValue(j)
//            );
//        }
//
//        for (int j = 0; j < 8; j++) {
//            n2.getNeuronGroups().get(0).getNeuron(j + 8).forceSetActivation(
//                    ((SmellSensor) pinnedMouse.getSensor("Smell-Center")).getCurrentValue(j)
//            );
//        }
//
//        for (int j = 0; j < 8; j++) {
//            n2.getNeuronGroups().get(0).getNeuron(j + 16).forceSetActivation(
//                    ((SmellSensor) pinnedMouse.getSensor("Smell-Right")).getCurrentValue(j)
//            );
//        }

        n.bufferedUpdateAllNeurons();
        n.update();
        n2.bufferedUpdateAllNeurons();
        n2.update();
        ((StraightMovement) newEntity.getEffectors().get(0)).setAmount(n.getNeuronGroups().get(1).getNeuron(0).getActivation());
        ((Turning) newEntity.getEffectors().get(1)).addAmount(n.getNeuronGroups().get(1).getNeuron(1).getActivation());
        ((Turning) newEntity.getEffectors().get(2)).addAmount(n.getNeuronGroups().get(1).getNeuron(2).getActivation());

        double[] newStim = new double[8];
        for (int i = 3; i < 9; i++) {
            newStim[i - 1] = n.getNeuronGroups().get(1).getNeuron(i).getActivation();
        }
        newEntity.getSmellSource().setStimulusVector(newStim);

        newStim = new double[8];
        for (int i = 3; i < 9; i++) {
            newStim[i - 1] = n2.getNeuronGroups().get(1).getNeuron(i).getActivation();
        }
        pinnedMouse.getSmellSource().setStimulusVector(newStim);

        // pinned mouse cant move
        ((Turning) pinnedMouse.getEffectors().get(1)).addAmount(n2.getNeuronGroups().get(1).getNeuron(1).getActivation());
        ((Turning) pinnedMouse.getEffectors().get(2)).addAmount(n2.getNeuronGroups().get(1).getNeuron(2).getActivation());

        w.update();

        if (w.containsEntity(cheese)) {
            // fitness counting and making cheese move when touch
            double dx = newEntity.getCenterX() - cheese.getCenterX();
            double dy = newEntity.getCenterY() - cheese.getCenterY();
            if (dx * dx + dy * dy < 1000) {
//                NEATRandomizer rand = agent.getRandomizer();
//                cheese.setLocation(
//                        Math.abs(450 - (cheese.getCenterX() + (rand.nextBoolean() ? 1 : -1) * (rand.nextDouble() + 0.2) * 20)),
//                        Math.abs(450 - (cheese.getCenterY() + (rand.nextBoolean() ? 1 : -1) * (rand.nextDouble() + 0.2) * 20)));
//                agent.setFitness(agent.getFitness() + 1);

//                cheese.setLocation(450 / 2 + 450 / 2 * Math.sin(6 * agent.getFitness() * Math.PI / 47),
//                        450 / 2 + 60 * Math.sin((1 + agent.getFitness()) * Math.PI / 4));
                w.deleteEntity(cheese);
                agent.setFitness(agent.getFitness() + 3);
            }
            double energyPenalty = n.getNeuronGroups().get(1).getNeuron(0).getActivation() / 32 / 400  // movement
                    + n.getNeuronGroups().get(1).getNeuron(1).getActivation() / 12 / 400               // rotation
                    + n.getNeuronGroups().get(1).getNeuron(2).getActivation() / 12 / 400               // rotation
                    + n.getNeuronGroups().get(1).getNeuron(3).getActivation() / 32 / 400               // "articulation"
                    + n.getNeuronGroups().get(1).getNeuron(4).getActivation() / 32 / 400               // "articulation"
                    + n.getNeuronGroups().get(1).getNeuron(5).getActivation() / 32 / 400               // "articulation"
                    + n.getNeuronGroups().get(1).getNeuron(6).getActivation() / 32 / 400               // "articulation"
                    + n.getNeuronGroups().get(1).getNeuron(7).getActivation() / 32 / 400;              // "articulation"
            agent.setFitness(agent.getFitness() - energyPenalty);
        }
//        agent.setFitness(agent.getFitness() + (1000 - dx * dx + dy * dy) / 10000 / 400);
    }

    public static void worldTestingMethod(Agent agent) {
        agent.setFitness(0);

        Network n = agent.getNet();
        OdorWorld w = agent.getWorld();

        // create cheese with smell
        OdorWorldEntity cheese = new OdorWorldEntity(w);
        double[] smellVector = {1, 0.2};
        SmellSource smell = new SmellSource(smellVector);
        smell.setDispersion(240);
        smell.setDecayFunction(GaussianDecayFunction.create());
        cheese.setSmellSource(smell);

        // create mouse with smell vector
        SmellSource mouse1Smell = new SmellSource();
        mouse1Smell.setDispersion(450);
        mouse1Smell.setDecayFunction(GaussianDecayFunction.create());
        mouse1Smell.setStimulusVector(new double[8]);
        OdorWorldEntity newEntity = new OdorWorldEntity(w, EntityType.MOUSE);
        newEntity.setSmellSource(mouse1Smell);

        w.addEntity(newEntity);
        w.addEntity(cheese);

        // set location of entities
        newEntity.setLocation(450 / 8, 450 / 2);
        cheese.setLocation(450 / 8 + 40, 450 / 2);

        // run the actual evaluation
        for (int i = 0; i < 400; i++) {
            worldTestingIteration(agent, cheese, newEntity);
        }
//        agent.setFitness(agent.getFitness() + ((200 - Math.abs(newEntity.getCenterX() - cheese.getCenterX())) / 200));

    }

    // same as above, but with 2 mice and networks
    public static void worldTestingMethod2(Agent agent) {
        agent.setFitness(0);

        Network n = agent.getNet();
        OdorWorld w = agent.getWorld();
        OdorWorldEntity cheese = new OdorWorldEntity(w);
        NEATRandomizer rand = agent.getRandomizer();

        double[] smellVector = {1, 0.2};
        SmellSource smell = new SmellSource(smellVector);
        smell.setDispersion(240);
        smell.setDecayFunction(GaussianDecayFunction.create());
        cheese.setSmellSource(smell);

        SmellSource mouse1Smell = new SmellSource();
        mouse1Smell.setDispersion(450);
        mouse1Smell.setDecayFunction(GaussianDecayFunction.create());
        mouse1Smell.setStimulusVector(new double[8]);
        OdorWorldEntity newEntity = new OdorWorldEntity(w, EntityType.MOUSE);
        newEntity.setSmellSource(mouse1Smell);

        SmellSource mouse2Smell = new SmellSource();
        mouse2Smell.setDispersion(450);
        mouse2Smell.setDecayFunction(GaussianDecayFunction.create());
        mouse2Smell.setStimulusVector(new double[8]);
        OdorWorldEntity pinnedMouse = new OdorWorldEntity(w, EntityType.MOUSE);
        pinnedMouse.setSmellSource(mouse2Smell);

        w.addEntity(newEntity);
        w.addEntity(pinnedMouse);
        w.addEntity(cheese);

        newEntity.setLocation(450 / 2, 450 / 8 * 7);

        // The cheese will be at one of two position, left of the pinned mouse and right of the pinned mouse
        boolean moveCheeseToRight = rand.nextBoolean();
        cheese.setLocation(450 / 4 + (moveCheeseToRight ? 450 / 2 : 0), 450 / 8);
        pinnedMouse.setLocation(450 / 2 + 40, 450 / 8);

        for (int i = 0; i < 400; i++) {
            worldTestingIteration2(agent, cheese, newEntity, pinnedMouse);
        }
//        agent.setFitness(agent.getFitness() + ((200 - Math.abs(newEntity.getCenterX() - cheese.getCenterX())) / 200));

    }

    public static void main(String arg0[]) {

        long startTime = System.currentTimeMillis();

        // construct a pool of genomes with 2 inputs and 1 output
        Pool pool = new Pool(2, 1, 500, Test::evaluationMethod);

        // Run the evolutionary algorithm
        Genome topGenome = pool.evolve(1000, -.01);
        Agent topAgent = new Agent(topGenome);

        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime);

        System.out.println("Elapsed time:" + duration / 1000.0 + " seconds.");

        // TODO: Should be able to set these in Genome
//        topAgent.getGenome().getInputNg().setLocation(0, 225);
//        topAgent.getGenome().getOutputNg().setLocation(0, 0);

        NetworkPanel np = NetworkPanel.createNetworkPanel(topAgent.getNet());

        System.out.println(np.debugString());
        JDialog dialog = np.displayPanelInWindow(np, "NEAT-XOR");
        dialog.setSize(500, 500);
        // TODO: Pack should work. Override preferred size in netpanel?
        // dialog.pack();

    }

}
