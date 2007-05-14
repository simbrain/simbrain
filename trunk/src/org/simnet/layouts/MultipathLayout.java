package org.simnet.layouts;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import org.simnet.interfaces.Network;
import org.simnet.interfaces.Neuron;

/**
 * MultipathLayout arranges the layers of the network in multiple paths
 * 
 */
public class MultipathLayout implements Layout {

    /** Lay neurons out vertically. */
    public static final int VERTICAL = 0;

    /** Lay neurons out horizontally. */
    public static final int HORIZONTAL = 1;

    /** Initial x position of line of neurons. */
    private double initialX;

    /** Initial y position of line of neuorns. */
    private double initialY;
    
    /** Number of paths */
    private int numberOfPaths = 1;

    /** Horizontal spacing between neurons. */
    private double hSpacing;

    /** Vertical spacing between neurons. */
    private double vSpacing;

    /** What layout to use: vertical or horizontal. */
    private int layout;

    /**
     * Lay out a complex network into layers.
     *
     * @param hspacing
     * @param vspacing
     * @param layout
     */
    public MultipathLayout(final double hspacing, final double vspacing, final int numberOfPaths, final int layout) {
        this.vSpacing = vspacing;
        this.hSpacing = hspacing;
        this.numberOfPaths = numberOfPaths;
        this.layout = layout;
    }

    /** @see Layout */
    public void layoutNeurons(final Network network) {

        ArrayList layers = network.getNetworkList();

        int baseCount = ((Network) layers.get(0)).getNeuronCount();
        double y = initialY;

        // set the first layer
        Network currentLayer = (Network) layers.get(0);
        ArrayList neurons = currentLayer.getFlatNeuronList();
        for (int j = 0; j < neurons.size(); j++) {
            //double hOffset  =  ((baseCount - neurons.size()) * hSpacing) / 2;
            Neuron neuron = (Neuron) neurons.get(j);
            neuron.setX(initialX + (j * hSpacing));
            neuron.setY(y);
        }
        
        double x = initialX;
        int nodes;
        // set the rest of the layers
        for (int i = 1; i <  layers.size(); i++) {
            if((i-1)%numberOfPaths == 0){
        	// setting the initial x position
        	nodes = 0;
        	for(int l = i; l <layers.size() && l-i < numberOfPaths; l++){
        	    nodes += ((Network) layers.get(l)).getNeuronCount();
        	}
        	
        	x = initialX + ((baseCount - nodes - numberOfPaths + 1) * hSpacing)/2;
            }
            currentLayer = (Network) layers.get(i);
            neurons = currentLayer.getFlatNeuronList();
            for (int j = 0; j < neurons.size(); j++) {
        	Neuron neuron = (Neuron) neurons.get(j);
        	neuron.setX(x + (j * hSpacing));
                neuron.setY(y - ((((i-1)/numberOfPaths)+1) * vSpacing));
            }
            x += (neurons.size()+1) * hSpacing;
        }
    }

    /** @see Layout */
    public void setInitialLocation(final Point2D initialPoint) {
        initialX = initialPoint.getX();
        initialY = initialPoint.getY();
    }

    /** @see Layout */
    public String getLayoutName() {
        return "Multipath";
    }
}
