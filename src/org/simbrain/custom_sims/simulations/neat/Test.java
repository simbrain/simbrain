package org.simbrain.custom_sims.simulations.neat;

import org.simbrain.network.core.Network;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.trainers.TrainingSet;
import org.simbrain.network.util.NetworkLayoutManager;
import org.simbrain.network.util.NetworkLayoutManager.*;
import org.simbrain.util.genericframe.GenericFrame;

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
            new double[][]{{0,0},{0,1},{1,0},{1,1}},
            new double[][]{{0},{1},{1},{0}});

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


    public static void main(String arg0[]) {
        long startTime = System.currentTimeMillis();

        // To confirm evolution is done before opening network panel.
        final CountDownLatch latch = new CountDownLatch(1);

        // construct a pool of genomes with 2 inputs and 1 output
        Pool pool = new Pool(latch, 2, 1, 500, Test::evaluationMethod);

        // Run the evolutionary algorithm
        Agent topAgent = pool.evolve(1000, -.01);

        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime);

        System.out.println("Elapsed time:" + duration / 1000.0 + " seconds.");


        // Once evolution is finished, view the winning network
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {

            // TODO: Should be able to set these in Genome
            topAgent.getGenome().getInputNg().setLocation(0,225);
            topAgent.getGenome().getOutputNg().setLocation(0,0);

            NetworkPanel np = NetworkPanel.createNetworkPanel(topAgent.getNet());
            JDialog dialog = np.displayPanelInWindow(np, "NEAT-XOR");
            dialog.setSize(500, 500);
            // TODO: Pack should work. Override preferred size in netpanel?
            // dialog.pack();

        }

    }

}
