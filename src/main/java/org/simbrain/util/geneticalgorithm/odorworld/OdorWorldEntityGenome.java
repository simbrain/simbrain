package org.simbrain.util.geneticalgorithm.odorworld;

import org.simbrain.util.geneticalgorithm.Genome;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

public class OdorWorldEntityGenome extends Genome<OdorWorldEntityGenome, OdorWorldEntity> {

    private EntitySensorChromosome sensors = new EntitySensorChromosome();

    @Override
    public OdorWorldEntityGenome crossOver(OdorWorldEntityGenome other) {
        OdorWorldEntityGenome ret = new OdorWorldEntityGenome();
        ret.sensors = this.sensors.crossOver(other.sensors);
        return ret;
    }

    @Override
    public void mutate() {
        sensors.mutate();
    }

    @Override
    public OdorWorldEntityGenome copy() {
        OdorWorldEntityGenome ret = new OdorWorldEntityGenome();
        ret.sensors = sensors.copy();
        return ret;
    }

    @Override
    public OdorWorldEntity express() {
        OdorWorldEntity ret = new OdorWorldEntity(null);
        sensors.getGenes().stream()
                .map(SensorGene::getPrototype)
                .peek(s -> s.setId(""))
                .forEach(ret::addSensor);
        return ret;
    }
}
