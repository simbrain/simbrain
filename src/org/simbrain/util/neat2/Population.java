package org.simbrain.util.neat2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Population<P extends Phenotype> {

    private int size;

    private List<Phenotype> population;

    public Population(int size) {
        this.size = size;
    }

    public void populate(P prototype) {
        population = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            population.add(prototype.copy());
        }
        population.forEach(Phenotype::assemble);
    }

    public Double computeNewFitness() {
        population.forEach(Phenotype::computeFitness);
        Collections.reverse(population);
        population = population.stream().limit(population.size() / 2).collect(Collectors.toList());
        return population.get(0).getCurrentFitness();
    }

}
