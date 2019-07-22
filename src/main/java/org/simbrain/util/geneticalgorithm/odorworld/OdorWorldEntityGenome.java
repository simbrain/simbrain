package org.simbrain.util.geneticalgorithm.odorworld;

import org.simbrain.util.geneticalgorithm.Genome;
import org.simbrain.util.neat.NodeChromosome;
import org.simbrain.util.neat.NodeGene;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.Sensor;

import java.util.function.Supplier;

/**
 * Represents and OdorWorld entity.
 */
public class OdorWorldEntityGenome extends Genome<OdorWorldEntityGenome, OdorWorldEntity> {

    private EntitySensorChromosome sensors;

    // Todo: EntityEffectorChromosome

    private OdorWorldEntity baseEntity = new OdorWorldEntity(null, EntityType.MOUSE);

    private Config config = new Config();

    private Supplier<Integer> nodeGeneInnovationNumberSupplier;

    public OdorWorldEntityGenome() {
        sensors = new EntitySensorChromosome();
        sensors.setConfig(config);
    }

    @Override
    public OdorWorldEntityGenome crossOver(OdorWorldEntityGenome other) {
        OdorWorldEntityGenome ret = new OdorWorldEntityGenome();
        ret.config = this.config;
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
        ret.config = this.config;
        return ret;
    }

    @Override
    public OdorWorldEntity express() {
        OdorWorldEntity ret = new OdorWorldEntity(null, EntityType.MOUSE);
        // Express the sensors and add them to the entity
        sensors.getGenes().stream()
                .map(SensorGene::getPrototype)
                .map(Sensor::copy)
                .map(Sensor.class::cast)
                .peek(s -> s.setId(""))
                .peek(s -> s.setParent(ret))
                .forEach(ret::addSensor);
        return ret;
    }

    public Supplier<Integer> getNodeGeneInnovationNumberSupplier() {
        return nodeGeneInnovationNumberSupplier;
    }

    public void setNodeGeneInnovationNumberSupplier(Supplier<Integer> nodeGeneInnovationNumberSupplier) {
        this.nodeGeneInnovationNumberSupplier = nodeGeneInnovationNumberSupplier;
        this.sensors.setNodeGeneInnovationNumberSupplier(nodeGeneInnovationNumberSupplier);
    }

    public NodeChromosome getSensorEffectorNodeChromosome() {
        NodeChromosome nodeChromosome = new NodeChromosome();
        for (Integer innovationNumber : sensors.getGeneMap().keySet()) {
            NodeGene nodeGene = new NodeGene();
            nodeGene.setType(NodeGene.NodeType.input);
            nodeChromosome.addGene(nodeGene, innovationNumber);
        }
        return nodeChromosome;
    }

    public OdorWorldEntity getBaseEntity() {
        return baseEntity;
    }

    public void setBaseEntity(OdorWorldEntity baseEntity) {
        this.baseEntity = baseEntity;
    }

    public Config getConfig() {
        return config;
    }

    /**
     * Configuration for odor world entity, sensors, and effectors.
     */
    public class Config {

        private double thetaMaxMutation = 0.314;

        private double thetaMutationProbability = 0.5;

        private double radiusMaxMutation = 5;

        private double radiusMax = 50;

        private double radiusMin = 0;

        private double radiusMutationProbability = 0.5;

        private double baseValueMutationProbability = 0.2;

        private double baseValueMaxMutation = 0.1;

        private double baseValueMin = 0.1;

        private double baseValueMax = 2;

        private double newSensorMutationProbability = 0.05;

        private double maxSensorCount = 5;

        public double getThetaMaxMutation() {
            return thetaMaxMutation;
        }

        public void setThetaMaxMutation(double thetaMaxMutation) {
            this.thetaMaxMutation = thetaMaxMutation;
        }

        public double getThetaMutationProbability() {
            return thetaMutationProbability;
        }

        public void setThetaMutationProbability(double thetaMutationProbability) {
            this.thetaMutationProbability = thetaMutationProbability;
        }

        public double getRadiusMaxMutation() {
            return radiusMaxMutation;
        }

        public void setRadiusMaxMutation(double radiusMaxMutation) {
            this.radiusMaxMutation = radiusMaxMutation;
        }

        public double getRadiusMax() {
            return radiusMax;
        }

        public void setRadiusMax(double radiusMax) {
            this.radiusMax = radiusMax;
        }

        public double getRadiusMin() {
            return radiusMin;
        }

        public void setRadiusMin(double radiusMin) {
            this.radiusMin = radiusMin;
        }

        public double getRadiusMutationProbability() {
            return radiusMutationProbability;
        }

        public void setRadiusMutationProbability(double radiusMutationProbability) {
            this.radiusMutationProbability = radiusMutationProbability;
        }

        public double getBaseValueMutationProbability() {
            return baseValueMutationProbability;
        }

        public void setBaseValueMutationProbability(double baseValueMutationProbability) {
            this.baseValueMutationProbability = baseValueMutationProbability;
        }

        public double getBaseValueMaxMutation() {
            return baseValueMaxMutation;
        }

        public void setBaseValueMaxMutation(double baseValueMaxMutation) {
            this.baseValueMaxMutation = baseValueMaxMutation;
        }

        public double getBaseValueMin() {
            return baseValueMin;
        }

        public void setBaseValueMin(double baseValueMin) {
            this.baseValueMin = baseValueMin;
        }

        public double getBaseValueMax() {
            return baseValueMax;
        }

        public void setBaseValueMax(double baseValueMax) {
            this.baseValueMax = baseValueMax;
        }

        public double getNewSensorMutationProbability() {
            return newSensorMutationProbability;
        }

        public void setNewSensorMutationProbability(double newSensorMutationProbability) {
            this.newSensorMutationProbability = newSensorMutationProbability;
        }

        public double getMaxSensorCount() {
            return maxSensorCount;
        }

        public void setMaxSensorCount(double maxSensorCount) {
            this.maxSensorCount = maxSensorCount;
        }
    }
}
