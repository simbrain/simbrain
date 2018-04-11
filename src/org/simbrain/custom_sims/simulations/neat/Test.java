package org.simbrain.custom_sims.simulations.neat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.simbrain.custom_sims.simulations.neat.procedureActions.InstanceProcedureAction;
import org.simbrain.custom_sims.simulations.neat.procedureActions.instance.IterateNetworkAction;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;

public class Test {

    public static void main(String arg0[]) throws InterruptedException {
        long startTime = System.currentTimeMillis();

        List<InstanceProcedureAction> evaluationMethods = new ArrayList<>();
        evaluationMethods.add(agent -> {
            // initializing fitness score
            // TODO: it is easy to forget initialize fitness. make default fitness configurable in pool.
            agent.setFitness(0);

            Network n = agent.getNet();

            // inputing 00, 01, 10, 11 to input nodes.
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 2; k++) {
                    agent.getNet().getNeuron(0).forceSetActivation(j);
                    agent.getNet().getNeuron(1).forceSetActivation(k);
                    for (int l = 0; l < 100; l++) {
                        n.bufferedUpdateAllNeurons();
                    }
                    n.update();

                    // calculating sse
                    double err = (j ^ k) - agent.getNet().getNeuron(2).getActivation();
                    double fitness = agent.getFitness() - (err * err);
                    agent.setFitness(fitness);
                }
            }
        });

        // construct a pool of 100 genomes with 2 inputs 1 outputs
        Pool pool = new Pool(2, 1, 1L, 100, evaluationMethods);

        // Run the evolutionary algorithm
        pool.evolve(1000, -.01);


        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime);

        System.out.println("Took " + duration / 1000.0 + " seconds.");
    }

}
