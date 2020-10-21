package org.simbrain.util.neat;

import org.simbrain.network.core.Synapse;
import org.simbrain.util.geneticalgorithms.Chromosome;
import org.simbrain.util.math.SimbrainRandomizer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A set of connection genes and machinery for crossing them over properly.
 */
public class ConnectionChromosome extends Chromosome<Synapse, ConnectionChromosome> {

    /**
     * The set of connection genes, indexed by innovation number.
     */
    private Map<Integer, ConnectionGene> connectionGenes = new TreeMap<>();

    @Override
    public ConnectionChromosome crossOver(ConnectionChromosome other) {

        ConnectionChromosome ret = new ConnectionChromosome();

        // Make a union of innovation numbers
        Set<Integer> allInnovationNumbers = new TreeSet<>(this.connectionGenes.keySet());
        allInnovationNumbers.addAll(other.connectionGenes.keySet());

        for (Integer i : allInnovationNumbers) {
            if (this.connectionGenes.containsKey(i) && other.connectionGenes.containsKey(i)) {
                // Both parents have the innovation number, so choose it randomly.
                // It will connect the same nodes but the connection properties may differ
                double decision =  SimbrainRandomizer.rand.nextDouble(0, 1);
                // TODO: Make the .5 configurable
                if (decision < 0.5) {
                    ret.connectionGenes.put(i, this.connectionGenes.get(i));
                } else {
                    ret.connectionGenes.put(i, other.connectionGenes.get(i));
                }
            } else if (this.connectionGenes.containsKey(i)) {
                // If only one parent has the connection gene, use it
                ret.connectionGenes.put(i, this.connectionGenes.get(i));
            } else {
                // If only one parent has the connection gene, use it
                ret.connectionGenes.put(i, other.connectionGenes.get(i));
            }
        }

        return ret;
    }

    public void addGene(int innovationNumber, ConnectionGene newGene) {
        connectionGenes.put(innovationNumber, newGene);
    }

    @Override
    public List<ConnectionGene> getGenes() {
        return new ArrayList<>(connectionGenes.values());
    }

    public ConnectionChromosome copy() {
        ConnectionChromosome ret = new ConnectionChromosome();
        ret.connectionGenes = connectionGenes.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().copy()));
        return ret;
    }
}
