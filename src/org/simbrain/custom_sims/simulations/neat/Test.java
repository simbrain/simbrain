package org.simbrain.custom_sims.simulations.neat;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.trainers.TrainingSet;
import org.simbrain.network.util.NetworkLayoutManager;
import org.simbrain.network.util.NetworkLayoutManager.*;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.util.environment.SmellSource.DecayFunction;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.effectors.StraightMovement;
import org.simbrain.world.odorworld.effectors.Turning;
import org.simbrain.world.odorworld.entities.BasicEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;
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
    
    public static void worldTestingIteration(Agent agent, BasicEntity cheese, RotatingEntity newEntity) {
        Network n = agent.getNet();
        OdorWorld w = agent.getWorld();

        for (Neuron nr : n.getNeuronGroups().get(1).getNeuronList()) {
            nr.setUpdateRule(new LinearRule());
            nr.setUpperBound(3);
            nr.setLowerBound(0);
        }

        // 8 is the stimulus vector dimension this simulation will be using

        for (int j = 0; j < 8; j++) {
            n.getNeuronGroups().get(0).getNeuron(j).forceSetActivation(
                    ((SmellSensor) newEntity.getSensor("Smell-Left")).getCurrentValue(j)
            );
        }

        for (int j = 0; j < 8; j++) {
            n.getNeuronGroups().get(0).getNeuron(j + 8).forceSetActivation(
                    ((SmellSensor) newEntity.getSensor("Smell-Center")).getCurrentValue(j)
            );
        }

        for (int j = 0; j < 8; j++) {
            n.getNeuronGroups().get(0).getNeuron(j + 16).forceSetActivation(
                    ((SmellSensor) newEntity.getSensor("Smell-Right")).getCurrentValue(j)
            );
        }

        n.bufferedUpdateAllNeurons();
        n.update();
        ((StraightMovement) newEntity.getEffectors().get(0)).setAmount(n.getNeuronGroups().get(1).getNeuron(0).getActivation());
        ((Turning) newEntity.getEffectors().get(1)).setAmount(n.getNeuronGroups().get(1).getNeuron(1).getActivation());
        ((Turning) newEntity.getEffectors().get(2)).setAmount(n.getNeuronGroups().get(1).getNeuron(2).getActivation());
        w.update(1);
        double dx = newEntity.getCenterX() - cheese.getCenterX();
        double dy = newEntity.getCenterY() - cheese.getCenterY();
        if (dx * dx + dy * dy < 1000) {
//            Random rand = new Random(1L);
//            cheese.setLocation(
//                    Math.abs(450 - (cheese.getCenterX() + (rand.nextBoolean() ? 1 : -1) * (rand.nextDouble() + 0.2) * 20)),
//                    Math.abs(450 - (cheese.getCenterY() + (rand.nextBoolean() ? 1 : -1) * (rand.nextDouble() + 0.2) * 20)));
//            agent.setFitness(agent.getFitness() + 1);
            
            cheese.setLocation(450 / 2 + 450 / 2 * Math.sin(6 * agent.getFitness() * Math.PI / 47),
                    450 / 2 + 60 * Math.sin((1 + agent.getFitness()) * Math.PI / 4));
            agent.setFitness(agent.getFitness() + 1);
        }
        double thing = n.getNeuronGroups().get(1).getNeuron(0).getActivation() / 128 / 400
                + n.getNeuronGroups().get(1).getNeuron(0).getActivation() / 1 / 400
                + n.getNeuronGroups().get(1).getNeuron(2).getActivation() / 8 / 400;
        agent.setFitness(agent.getFitness() - thing);
//        agent.setFitness(agent.getFitness() + (1000 - dx * dx + dy * dy) / 10000 / 400);
    }

    public static void worldTestingMethod(Agent agent) {
        agent.setFitness(0);

        Network n = agent.getNet();
        OdorWorld w = agent.getWorld();
        BasicEntity cheese = new BasicEntity(w);
        double[] smellVector = {1, 0.2};
        SmellSource smell = new SmellSource(smellVector);
        smell.setDispersion(240);
        smell.setDecayFunction(DecayFunction.GAUSSIAN);
        cheese.setSmellSource(smell);
        RotatingEntity newEntity = new RotatingEntity(w);
        w.addAgent(newEntity);
        w.addEntity(cheese);
        newEntity.setLocation(450 / 8, 450 / 2);
        cheese.setLocation(450 / 8 + 40, 450 / 2);

        for (int i = 0; i < 400; i++) {
            worldTestingIteration(agent, cheese, newEntity);
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
