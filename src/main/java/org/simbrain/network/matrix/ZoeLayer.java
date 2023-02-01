package org.simbrain.network.matrix;


import org.simbrain.network.core.ArrayLayer;
import org.simbrain.network.core.Network;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import smile.math.matrix.Matrix;

import java.awt.geom.Rectangle2D;

/**
 * Template for Zoe Layer
 */
public class ZoeLayer extends ArrayLayer implements EditableObject, AttributeContainer {

    /**
     * Construct a neuron array.
     *
     * @param net  parent net
     * @param inputSize number of components in the array
     */
    public ZoeLayer(Network net, int inputSize) {
        super(net, inputSize);
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
        ZoeLayer copy = new ZoeLayer(newParent, orig.inputSize());
        copy.setLocation(orig.getLocation());
        return copy;
    }

    @Override
    public void update() {
        System.out.println("Zoë Layer updated");
        getEvents().getUpdated().fireAndForget();
    }

    @Override
    public Matrix getOutputs() {
        return null;
    }

    @Override
    public int outputSize() {
        return 10;
    }

    @Override
    public Rectangle2D getBound() {
        return new Rectangle2D.Double(getX() - 150 / 2, getY() - 50 / 2, 150, 50);
    }

}
