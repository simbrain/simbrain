package org.simbrain.util.geneticalgorithm.odorworld;

import org.simbrain.network.core.Network;
import org.simbrain.util.Pair;
import org.simbrain.util.geneticalgorithm.Genome;
import org.simbrain.util.neat.NetworkGenome;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

public class NetworkEntityGenome extends Genome<NetworkEntityGenome, Pair<Network, OdorWorldEntity>> {

    NetworkGenome networkGenome;

    OdorWorldEntityGenome entityGenome;

    private NetworkEntityGenome() {

    }

    public NetworkEntityGenome(NetworkGenome.Configuration configuration) {

        networkGenome = new NetworkGenome(configuration);

        entityGenome = new OdorWorldEntityGenome();
        entityGenome.getConfig().setMaxSensorCount(3);
    }

    public NetworkGenome.Configuration configNetworkGenome() {
        return networkGenome.getConfiguration();
    }

    public OdorWorldEntityGenome.Config configEntityGenome() {
        return entityGenome.getConfig();
    }

    @Override
    public NetworkEntityGenome crossOver(NetworkEntityGenome other) {
        NetworkEntityGenome ret = new NetworkEntityGenome();
        ret.networkGenome = this.networkGenome.crossOver(other.networkGenome);
        ret.entityGenome = this.entityGenome.crossOver(other.entityGenome);
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
        return ret;
    }

    @Override
    public Pair<Network, OdorWorldEntity> express() {
        Network network = networkGenome.express();
        OdorWorldEntity entity = entityGenome.express();
        return new Pair<>(network, entity);
    }
}
