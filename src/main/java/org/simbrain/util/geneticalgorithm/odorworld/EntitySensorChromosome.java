package org.simbrain.util.geneticalgorithm.odorworld;

import org.simbrain.util.geneticalgorithm.Chromosome;
import org.simbrain.util.geneticalgorithm.Gene;
import org.simbrain.util.geneticalgorithm.numerical.NumericalGeneticAlgUtils;
import org.simbrain.util.math.SimbrainRandomizer;
import org.simbrain.world.odorworld.sensors.Sensor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EntitySensorChromosome extends Chromosome<Sensor, EntitySensorChromosome> {

    private List<SensorGene> sensorGenes = new ArrayList<>();

    private Config config = new Config();

    @Override
    public void mutate() {
        super.mutate();

        if (SimbrainRandomizer.rand.nextDouble(0, 1) > config.newSensorMutationProbability) {
            int select = SimbrainRandomizer.rand.nextInteger(0, 1);
            SensorGene sensorGene;
            switch (select) {
                case 0:
                    sensorGene = new ObjectSensorGene();
                    break;
                default:
                    sensorGene = new SmellSensorGene();
                    break;
            }
            sensorGene.mutate();
            sensorGenes.add(sensorGene);
        }
    }

    @Override
    public EntitySensorChromosome crossOver(EntitySensorChromosome other) {
        EntitySensorChromosome ret = new EntitySensorChromosome();
        NumericalGeneticAlgUtils.singlePointCrossover(this, other, ret);
        return ret;
    }

    public EntitySensorChromosome copy() {
        EntitySensorChromosome ret = new EntitySensorChromosome();

        ret.config = this.config;
        ret.sensorGenes = sensorGenes.stream()
                .map(SensorGene::copy)
                .map(SmellSensorGene.class::cast)
                .collect(Collectors.toList());

        return ret;
    }

    @Override
    public List<? extends SensorGene> getGenes() {
        return sensorGenes;
    }

    public class Config {
        private double newSensorMutationProbability = 0.05;

        private double maxSensorCount = 5;

        public Config newSensorMutationProbability(double newSensorMutationProbability) {
            this.newSensorMutationProbability = newSensorMutationProbability;
            return this;
        }

        public Config maxSensorCount(double maxSensorCount) {
            this.maxSensorCount = maxSensorCount;
            return this;
        }

        public EntitySensorChromosome done() {
            return EntitySensorChromosome.this;
        }
    }
}
