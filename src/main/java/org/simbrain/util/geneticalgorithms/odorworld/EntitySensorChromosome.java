package org.simbrain.util.geneticalgorithms.odorworld;

import org.simbrain.util.geneticalgorithms.Chromosome;
import org.simbrain.util.math.SimbrainRandomizer;
import org.simbrain.world.odorworld.sensors.Sensor;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class EntitySensorChromosome extends Chromosome<Sensor, EntitySensorChromosome> {

    private Map<Integer, SensorGene> sensorGenes = new TreeMap<>();

    private OdorWorldEntityGenome.Config config;

    private Supplier<Integer> nodeGeneInnovationNumberSupplier;

    @Override
    public void mutate() {
        super.mutate();

        if (sensorGenes.size() >= config.getMaxSensorCount()) {
            return;
        }

        if (SimbrainRandomizer.rand.nextDouble(0, 1) > config.getNewSensorMutationProbability()) {
            int select = SimbrainRandomizer.rand.nextInteger(0, 1);
            SensorGene sensorGene;
            switch (select) {
                // case 0:
                //     sensorGene = new SmellSensorGene();
                //     break;
                default:
                    sensorGene = new ObjectSensorGene();
                    break;
            }
            sensorGene.setConfig(config);
            sensorGene.mutate();
            sensorGenes.put(nodeGeneInnovationNumberSupplier.get(), sensorGene);
        }
    }

    @Override
    public EntitySensorChromosome crossOver(EntitySensorChromosome other) {
        EntitySensorChromosome ret = new EntitySensorChromosome();
        ret.setConfig(config);
        Set<Integer> nodeIDUnionSet = new HashSet<>();
        nodeIDUnionSet.addAll(sensorGenes.keySet());
        nodeIDUnionSet.addAll(other.sensorGenes.keySet());
        ret.sensorGenes = nodeIDUnionSet.stream()
                .limit((long) config.getMaxSensorCount()) // find a better way
                .collect(Collectors.toMap(
                        Function.identity(),
                        id -> {
                            if (sensorGenes.containsKey(id)) {
                                SensorGene gene = sensorGenes.get(id).copy();
                                gene.setConfig(config);
                                return gene;
                            } else {
                                SensorGene gene = other.sensorGenes.get(id).copy();
                                gene.setConfig(config);
                                return gene;
                            }
                        }
                ));

        return ret;
    }

    public EntitySensorChromosome copy() {
        EntitySensorChromosome ret = new EntitySensorChromosome();

        ret.config = this.config;
        ret.sensorGenes = sensorGenes.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().copy()
                ));

        return ret;
    }

    @Override
    public List<? extends SensorGene> getGenes() {
        return new ArrayList<>(sensorGenes.values());
    }

    public Map<Integer, SensorGene> getGeneMap() {
        return Collections.unmodifiableMap(sensorGenes);
    }

    public OdorWorldEntityGenome.Config getConfig() {
        return config;
    }

    public void setConfig(OdorWorldEntityGenome.Config config) {
        this.config = config;
    }

    public void setNodeGeneInnovationNumberSupplier(Supplier<Integer> nodeGeneInnovationNumberSupplier) {
        this.nodeGeneInnovationNumberSupplier = nodeGeneInnovationNumberSupplier;
    }
}
