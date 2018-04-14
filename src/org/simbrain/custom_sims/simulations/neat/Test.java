package org.simbrain.custom_sims.simulations.neat;

import org.simbrain.network.core.Network;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.util.genericframe.GenericFrame;

import javax.swing.*;

public class Test {

    public static void evaluationMethod(Agent agent) {
        // initializing fitness score
        // TODO: it is easy to forget to initialize fitness. make default fitness configurable in pool.
        agent.setFitness(0);

        Network n = agent.getNet();

        // TODO: Add neuron group
        // getNet().getInputGroup().set([0,0]);..
        // network.update()
        // Utils.getMSE(trainingSet).


        //TODO: get named groups agent
        // inputing 00, 01, 10, 11 to input nodes.
        NeuronGroup inputGroup = (NeuronGroup) agent.getNet().getGroupList().get(0);
        NeuronGroup outputGroup = (NeuronGroup) agent.getNet().getGroupList().get(1);

        if(inputGroup.size() == 0 || outputGroup.size() == 0) {
            agent.setFitness(0);
            return;
        }

        //TODO: Use existing Simbrain utils or add them.
        //  TrainingSet ts = new TrainingSet(new Double[][]{{0,0},{1,0}....}
        //  TrainingUtils.getMSE(network, ts)

        for (int j = 0; j < 2; j++) {
            for (int k = 0; k < 2; k++) {
                inputGroup.getNeuron(0).forceSetActivation(j);
                inputGroup.getNeuron(1).forceSetActivation(k);
                for (int l = 0; l < 100; l++) {
                    n.bufferedUpdateAllNeurons();
                }
                n.update();

                // calculating sse
                double err = (j ^ k) - outputGroup.getNeuron(0).getActivation();
                double fitness = agent.getFitness() - (err * err);
                agent.setFitness(fitness);
            }
        }
    }

    public static void main(String arg0[]) {
        long startTime = System.currentTimeMillis();

        // construct a pool of genomes with 2 inputs and 1 output
        Pool pool = new Pool(2, 1, 500, Test::evaluationMethod);

        // Run the evolutionary algorithm
        Agent topAgent = pool.evolve(100, -.01);
        System.out.println(topAgent.getGenome());

        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime);

        System.out.println("Elapsed time:" + duration / 1000.0 + " seconds.");

        NetworkPanel np = new NetworkPanel(topAgent.getNet());
        // TODO: Not sure why calls below are needed. Issue in netpanel init
        ((NeuronGroup)topAgent.getNet().getGroupList().get(0)).setLocation(10,100);
        ((NeuronGroup)topAgent.getNet().getGroupList().get(1)).setLocation(10,0);
        JDialog dialog = np.displayPanelInWindow(np, "NEAT Test");
        np.syncToModel();
        // TODO: Pack should work. Override preferred size in netpanel?
        dialog.setBounds(10,10,400,400);
        // TODO: NetworkPanel inits to bad state. Can't zoom.
    }

}
