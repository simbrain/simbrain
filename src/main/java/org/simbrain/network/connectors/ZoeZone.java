package org.simbrain.network.connectors;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.network.synapse_update_rules.StaticSynapseRule;
import org.simbrain.util.DataHolder;
import org.simbrain.util.UserParameter;
import smile.math.matrix.Matrix;
import smile.stat.distribution.GaussianDistribution;

/**
 * A template connector object for Zoë to edit. Needs data structures, update method, etc.
 * Compare to {@link WeightMatrix}
 */
public class ZoeZone extends Connector {

    @UserParameter(label = "Increment amount", increment = .1, order = 20)
    private double increment = .1;

    @UserParameter(label = "Learning Rule", useSetter = true, isObjectType = true, order = 100)
    SynapseUpdateRule prototypeRule = new StaticSynapseRule();

    /**
     * Holds data for prototype rule.
     */
    private DataHolder dataHolder = new DataHolder.EmptyDataHolder();

    /**
     * Construct the matrix.
     *
     * @param net parent network
     * @param source source layer
     * @param target target layer
     */
    public ZoeZone(Network net, Connectable source, Connectable target) {
        super(source, target, net);
        source.addOutgoingConnector(this);
        target.addIncomingConnector(this);
    }


    @Override
    public Matrix getOutput() {
        // For now return output a random matrix
        return Matrix.rand(getTarget().size(), 1,  new GaussianDistribution(0, 1));
    }

    @Override
    public void update() {
        // TODO: Do stuff!
    }

    public SynapseUpdateRule getPrototypeRule() {
        return prototypeRule;
    }

    public void setPrototypeRule(SynapseUpdateRule prototypeRule) {
        this.prototypeRule = prototypeRule;
    }

    // Zoe the functions below respond to "r", "up button" and "down button".
    // See WeightMatrix.java for examples

    @Override
    public void randomize() {
        getEvents().fireUpdated();
    }

    @Override
    public void increment() {
        getEvents().fireUpdated();
    }

    @Override
    public void decrement() {
        getEvents().fireUpdated();
    }

    /**
     * Set all entries to 0.
     */
    public void hardClear() {
        getEvents().fireUpdated();
    }

    @Override
    public String toString() {
        return getId() + " todo!";
        // return getId()
        //         + " (" + weightMatrix.nrows() + "x" + weightMatrix.ncols() + ") "
        //         + "connecting " + source.getId() + " to " + target.getId();
    }
}
