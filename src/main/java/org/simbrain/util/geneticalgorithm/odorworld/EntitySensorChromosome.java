package org.simbrain.util.geneticalgorithm.odorworld;

import org.simbrain.util.geneticalgorithm.Chromosome;
import org.simbrain.util.geneticalgorithm.numerical.NumericalGeneticAlgUtils;
import org.simbrain.util.math.SimbrainRandomizer;
import org.simbrain.world.odorworld.sensors.Sensor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EntitySensorChromosome extends Chromosome<Sensor, EntitySensorChromosome> {

    private List<SensorGene> sensorGenes = new ArrayList<>();

    private OdorWorldEntityGenome.Config config;

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
            sensorGenes.add(sensorGene);
        }
    }

    @Override
    public EntitySensorChromosome crossOver(EntitySensorChromosome other) {
        EntitySensorChromosome ret = new EntitySensorChromosome();
        ret.setConfig(config);
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

    public OdorWorldEntityGenome.Config getConfig() {
        return config;
    }

    public void setConfig(OdorWorldEntityGenome.Config config) {
        this.config = config;
    }
}
