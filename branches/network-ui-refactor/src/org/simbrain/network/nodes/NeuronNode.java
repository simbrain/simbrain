
package org.simbrain.network.nodes;

import java.awt.geom.GeneralPath;
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
import org.simnet.neurons.BinaryNeuron;

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
    private static final int DIAMETER = 24;

    /** Length of arrow. */
    private static final int ARROW_LINE = 20;

    /** Arrow associated with output node. */
    private PPath outArrow = new PPath();


    /**
     * Create a new neuron node.
     *
     * @param x initial x location of neuron
     * @param y initial y location of neuron
     */
    public NeuronNode(final double x, final double y) {

        super();
        offset(x, y);

        PNode circle = PPath.createEllipse(0, 0, DIAMETER, DIAMETER);

        neuron = new BinaryNeuron();

        addChild(circle);
        
        // Testing output arrow
        motorCoupling = new MotorCoupling();
        this.addChild(outArrow);
        updateOutArrow();
        
        setPickable(true);
        setChildrenPickable(false);
        
        setBounds(circle.getBounds());
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
        contextMenu.add(new JMenuItem("Neuron specific context menu item"));
        contextMenu.add(new JMenuItem("Neuron specific context menu item"));

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
     * Creates an arrow which designates an on-screen neuron as an output node, which sends signals to an external
     * environment (the world object)
     *
     * @return an object representing the input arrow of a PNodeNeuron
     *
     * @see org.simbrain.sim.world
     */
    private GeneralPath createOutArrow() {
        GeneralPath arrow = new GeneralPath();
        float cx = (float) getX() + DIAMETER/2;
        float cy = (float) getY() + DIAMETER/2;

        arrow.moveTo(cx, cy - DIAMETER/2);
        arrow.lineTo(cx, cy - DIAMETER/2 - ARROW_LINE);

        arrow.moveTo(cx, cy - DIAMETER/2 - ARROW_LINE);
        arrow.lineTo(cx - DIAMETER/4, cy - DIAMETER);

        arrow.moveTo(cx, cy - DIAMETER/2 - ARROW_LINE);
        arrow.lineTo(cx + DIAMETER/4, cy - DIAMETER);

        return arrow;
    }
    
    /**
     * Updates graphics depending on whether this is an output node or not
     */
    public void updateOutArrow() {
        if (isOutput()) {
            GeneralPath ia = createOutArrow();
            outArrow.reset();
            outArrow.append(ia, false);
        } else {
            outArrow.reset();
        }
    }


    /**
     * Returns String representation of this NeuronNode.
     *
     * @return String representation of this node.
     */
    public String toString() {
        String ret = new String();
        ret += "NeuronNode: (" + this.getGlobalFullBounds().x + ")(" + getGlobalFullBounds().y + ")\n";
        return ret;
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


    /**
     * @return Returns the motorCoupling.
     */
    public MotorCoupling getMotorCoupling() {
        return motorCoupling;
    }


    /**
     * @param motorCoupling The motorCoupling to set.
     */
    public void setMotorCoupling(MotorCoupling motorCoupling) {
        this.motorCoupling = motorCoupling;
    }


    /**
     * @return Returns the sensoryCoupling.
     */
    public SensoryCoupling getSensoryCoupling() {
        return sensoryCoupling;
    }


    /**
     * @param sensoryCoupling The sensoryCoupling to set.
     */
    public void setSensoryCoupling(SensoryCoupling sensoryCoupling) {
        this.sensoryCoupling = sensoryCoupling;
    }    
}