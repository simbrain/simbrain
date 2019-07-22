package org.simbrain.util.geneticalgorithm.odorworld;

import org.simbrain.custom_sims.helper_classes.Simulation;
import org.simbrain.custom_sims.simulations.test.EvolveOdorWorldAgent;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.util.Pair;
import org.simbrain.util.geneticalgorithm.Genome;
import org.simbrain.util.neat.NetworkGenome;
import org.simbrain.util.neat.NodeGene;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.Sensor;
import org.simbrain.world.odorworld.sensors.SmellSensor;

import java.util.function.Supplier;

/**
 * Represents a network attached to an odor world entity.  A literal pair with
 * a network and odor world entity.  Coupling between the two happens in an eval function.
 * We may later evolve the links between them directly.
 * See {@link EvolveOdorWorldAgent}
 */
public class NetworkEntityGenome extends Genome<NetworkEntityGenome, Pair<Network, OdorWorldEntity>> {

    private NetworkGenome networkGenome;

    private OdorWorldEntityGenome entityGenome;

    private int lastInnovationNumber = 0;

    private Supplier<Integer> nodeGeneInnovationNumberBaseSupplier;

    private NetworkEntityGenome() {
    }

    public NetworkEntityGenome(NetworkGenome.Configuration configuration) {

        entityGenome = new OdorWorldEntityGenome();
        entityGenome.getConfig().setMaxSensorCount(3);

        networkGenome = new NetworkGenome(configuration);
        networkGenome.addExternalNodeGenes(entityGenome::getSensorEffectorNodeChromosome);
    }

    public NetworkGenome.Configuration configNetworkGenome() {
        return networkGenome.getConfiguration();
    }

    public OdorWorldEntityGenome.Config configEntityGenome() {
        return entityGenome.getConfig();
    }

    public NetworkGenome getNetworkGenome() {
        return networkGenome;
    }

    public OdorWorldEntityGenome getEntityGenome() {
        return entityGenome;
    }

    @Override
    public NetworkEntityGenome crossOver(NetworkEntityGenome other) {
        NetworkEntityGenome ret = new NetworkEntityGenome();
        ret.networkGenome = this.networkGenome.crossOver(other.networkGenome);
        ret.entityGenome = this.entityGenome.crossOver(other.entityGenome);
        ret.setNodeGeneInnovationNumberBaseSupplier(this.nodeGeneInnovationNumberBaseSupplier);
        ret.networkGenome.addExternalNodeGenes(ret.entityGenome::getSensorEffectorNodeChromosome);
        return ret;
    }

    @Override
    public void mutate() {
        networkGenome.mutate();
        entityGenome.mutate();
    }

    @Override
    public NetworkEntityGenome copy() {
        NetworkEntityGenome ret = new NetworkEntityGenome();
        ret.networkGenome = this.networkGenome.copy();
        ret.entityGenome = this.entityGenome.copy();
        ret.setNodeGeneInnovationNumberBaseSupplier(this.nodeGeneInnovationNumberBaseSupplier);
        ret.networkGenome.addExternalNodeGenes(ret.entityGenome::getSensorEffectorNodeChromosome);
        return ret;
    }

    @Override
    public Pair<Network, OdorWorldEntity> express() {
        OdorWorldEntity entity = entityGenome.express();
        Network network = networkGenome.express();
        return new Pair<>(network, entity);
    }

    public void setNodeGeneInnovationNumberBaseSupplier(Supplier<Integer> nodeGeneInnovationNumberBaseSupplier) {
        this.lastInnovationNumber = nodeGeneInnovationNumberBaseSupplier.get();
        this.nodeGeneInnovationNumberBaseSupplier = nodeGeneInnovationNumberBaseSupplier;
        Supplier<Integer> innovationNumberSupplier = () -> lastInnovationNumber++;
        entityGenome.setNodeGeneInnovationNumberSupplier(innovationNumberSupplier);
        networkGenome.getNodeGenes().setInnovationNumberSupplier(innovationNumberSupplier);
    }

    public Integer getLastInnovationNumber() {
        return lastInnovationNumber;
    }
}
