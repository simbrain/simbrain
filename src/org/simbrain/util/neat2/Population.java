package org.simbrain.util.neat2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A agentList has multiple agents
 *   Each agent has a genome, as well as a fitness function
 *     The genome has chromosomes (which enforce compatability of gene in cross-over)
 *     The chromosomes have genes
 */
public class Population<P extends Agent> {

    private int size;

    private List<Agent> agentList;

    public Population(int size) {
        this.size = size;
    }

    public void populate(P prototype) {
        agentList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            agentList.add(prototype.copy());
        }
    }

    public Double computeNewFitness() {
        agentList.forEach(Agent::computeFitness);
        Collections.reverse(agentList);
        agentList = agentList.stream().limit(agentList.size() / 2).collect(Collectors.toList());
        return agentList.get(0).getCurrentFitness();
    }

}
