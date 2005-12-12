
package org.simbrain.network.nodes;

import java.awt.geom.Point2D;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import edu.umd.cs.piccolo.PNode;

import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;

import edu.umd.cs.piccolo.util.PDimension;

import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;

import org.simbrain.coupling.MotorCoupling;
import org.simbrain.coupling.SensoryCoupling;

import org.simbrain.network.NetworkPanel;

import org.simnet.interfaces.Neuron;

/**
 * <b>NeuronNode</b> is a Piccolo PNode corresponding to a Neuron in the neural network model
 */
public final class NeuronNode
    extends ScreenElement {

    /** The logical neuron this screen element represents. */
    private Neuron neuron;
    
    /** Represents a coupling between this neuron and an external source of "sensory" input. */
    private SensoryCoupling sensoryCoupling;
    
    /** Represents a coupling between this neuron and an external source of "motor" output. */
    private MotorCoupling motorCoupling;

    /** Diameter of neuron. */
    private final static int DIAMETER = 24;


    /**
     * Create a new debug node.
     */
    public NeuronNode(final double x, final double y) {

        super();
        offset(x, y);

        PNode circle = PPath.createEllipse(0, 0, DIAMETER, DIAMETER);

        addChild(circle);

        setPickable(true);
        setChildrenPickable(false);
    }


    /** @see ScreenElement */
    protected String getToolTipText() {
        return "neuron";
    }

    /** @see ScreenElement */
    protected JPopupMenu createContextMenu() {

        JPopupMenu contextMenu = new JPopupMenu();
        // add actions
        contextMenu.add(new JMenuItem("Neuron node"));
        contextMenu.add(new JMenuItem("Node specific context menu item"));
        contextMenu.add(new JMenuItem("Node specific context menu item"));

        return contextMenu;
    }


    /**
     * Return true if this neuron has a sensory coupling attached.
     *
     * @return true if this neuron has a sensory coupling attached
     */
    public boolean isInput() {
        return (sensoryCoupling != null);
    }

    /**
     * Return true if this neuron has a motor coupling attached.
     *
     * @return true if this neuron has a motor coupling attached
     */
    public boolean isOutput() {
        return (motorCoupling != null);
    }


    //
    // bound properties

    /**
     * Return the neuron for this neuron node.
     *
     * @return the neuron for this neuron node
     */
    public Neuron getNeuron() {
        return neuron;
    }

    /**
     * Set the neuron for this neuron node to <code>neuron</code>.
     *
     * <p>This is a bound property.</p>
     *
     * @param neuron neuron for this neuron node
     */
    public void setNeuron(final Neuron neuron) {

        Neuron oldNeuron = this.neuron;
        this.neuron = neuron;
        firePropertyChange("neuron", oldNeuron, neuron);
    }    
}