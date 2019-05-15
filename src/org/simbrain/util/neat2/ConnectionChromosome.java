package org.simbrain.util.neat2;

import org.simbrain.network.core.Synapse;
import org.simbrain.util.neat.NEATRandomizer;

import java.util.*;
import java.util.stream.Collectors;

public class ConnectionChromosome extends Chromosome<Synapse, ConnectionChromosome> {

    private NEATRandomizer randomizer;

    private Map<Integer, ConnectionGene> connectionGenes = new TreeMap<>();

    @Override
    public ConnectionChromosome crossOver(ConnectionChromosome other) {

        ConnectionChromosome ret = new ConnectionChromosome();

        Set<Integer> allInnovationNumbers = new TreeSet<>(this.connectionGenes.keySet());
        allInnovationNumbers.addAll(other.connectionGenes.keySet());

        for (Integer i : allInnovationNumbers) {
            if (this.connectionGenes.containsKey(i) && other.connectionGenes.containsKey(i)) {
                double decision = randomizer.nextDouble(0, 1);
                if (decision < 0.5) {
                    ret.connectionGenes.put(i, this.connectionGenes.get(i));
                } else {
                    ret.connectionGenes.put(i, other.connectionGenes.get(i));
                }
            } else if (this.connectionGenes.containsKey(i)) {
                ret.connectionGenes.put(i, this.connectionGenes.get(i));
            } else {
                ret.connectionGenes.put(i, other.connectionGenes.get(i));
            }
        }

        return ret;
    }

    @Override
    public Collection<ConnectionGene> getGenes() {
        return connectionGenes.values();
    }

    public ConnectionChromosome copy() {
        ConnectionChromosome ret = new ConnectionChromosome();
        ret.connectionGenes = connectionGenes.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().copy()));
        return ret;
    }
}
