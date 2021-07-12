package org.simbrain.network.matrix;


import org.simbrain.network.connectors.Layer;
import org.simbrain.network.core.Network;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import org.tensorflow.Tensor;
import smile.math.matrix.Matrix;

import java.awt.geom.Rectangle2D;

/**
 * Template for Zoe Layer
 */
public class ZoeLayer extends Layer implements EditableObject, AttributeContainer {

    /**
     * Reference to network this array is part of.
     */
    private final Network parent;

    /**
     * Construct a neuron array.
     *
     * @param net  parent net
     * @param size number of components in the array
     */
    public ZoeLayer(Network net, int size) {
        parent = net;
        setLabel(net.getIdManager().getProposedId(this.getClass()));
    }

    /**
     * Make a deep copy of this array.
     *
     * @param newParent the new parent network
     * @param orig      the array to copy
     * @return the deep copy
     */
    public ZoeLayer deepCopy(Network newParent, ZoeLayer orig) {
        ZoeLayer copy = new ZoeLayer(newParent, orig.size());
        copy.setLocation(orig.getLocation());
        return copy;
    }

    @Override
    public void update() {
        System.out.println("Zoë Layer updated");
        getEvents().fireUpdated();
    }

    public Tensor tensor;

    @Override
    public Matrix getOutputs() {
        return null;
    }

    @Override
    public int size() {
        return 10;
    }

    @Override
    public Network getNetwork() {
        return parent;
    }

    @Override
    public Rectangle2D getBound() {
        return new Rectangle2D.Double(getX() - 150 / 2, getY() - 50 / 2, 150, 50);
    }

    @Override
    public Matrix getInputs() {
        return null;
    }

    @Override
    public void addInputs(Matrix inputs) {
    }

}
