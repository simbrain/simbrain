package org.simbrain.util.neat2;


import java.util.List;
import java.util.function.Function;

public class DoubleAgent extends Agent<DoubleGenome, DoubleAgent> {

    /**
     * Phenotype
     */
    private List<Double> agent;

    public DoubleAgent(DoubleGenome genome, Function<DoubleAgent, Double> fitnessFunction) {
        super(genome, fitnessFunction);
        agent = genome.build();
    }

    @Override
    public DoubleAgent crossover(DoubleAgent other) {
        return new DoubleAgent(this.getGenome().crossOver(other.getGenome()), getFitnessFunction());
    }

    @Override
    public void mutate() {
        getGenome().mutate();
    }

    @Override
    public void computeFitness() {
        computeFitness(this);
    }

    @Override
    public DoubleAgent copy() {
        return new DoubleAgent(getGenome().copy(), getFitnessFunction());
    }

    public List<Double> getAgent() {
        return agent;
    }
}
