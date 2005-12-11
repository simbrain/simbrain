
package org.simbrain.network.nodes;

import java.awt.geom.Point2D;

import javax.swing.JPopupMenu;

import edu.umd.cs.piccolo.PNode;

import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;

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

    /**
     * Create a new debug node.
     */
    public NeuronNode(final double x, final double y) {

        super();
        offset(x, y);

        PText label = new PText("Debug");
        PNode rect = PPath.createRectangle(0.0f, 0.0f, 50.0f, 50.0f);

        label.offset(7.5d, 21.0d);

        rect.addChild(label);
        addChild(rect);

        setPickable(true);
        setChildrenPickable(false);

        // add tool tip text updater
        addInputEventListener(new ToolTipTextUpdater() {
 
               /** @see ToolTipTextUpdater */
                protected String getToolTipText() {
                    return "debug";
                }
            });

        addInputEventListener(new ContextMenuEventHandler());
    }

    /**
     * Return the context menu specific to this debug node.
     *
     * @return the context menu specific to this debug node
     */
    private JPopupMenu getContextMenu() {

        JPopupMenu contextMenu = new JPopupMenu();
        // add actions
        contextMenu.add(new javax.swing.JMenuItem("Debug node"));
        contextMenu.add(new javax.swing.JMenuItem("Node specific context menu item"));
        contextMenu.add(new javax.swing.JMenuItem("Node specific context menu item"));
        return contextMenu;
    }


    /**
     * Debug node-specific context menu handler.
     */
    private class ContextMenuEventHandler
        extends PBasicInputEventHandler {

        /**
         * Show the context menu.
         *
         * @param event event
         */
        private void showContextMenu(final PInputEvent event) {

            // mark this event as handled
            // so the general context menu handler does not show
            event.setHandled(true);

            // show debug node-specific context menu
            NetworkPanel networkPanel = (NetworkPanel) event.getComponent();
            JPopupMenu contextMenu = getContextMenu();
            Point2D canvasPosition = event.getCanvasPosition();
            contextMenu.show(networkPanel, (int) canvasPosition.getX(), (int) canvasPosition.getY());
        }

        /** @see PBasicInputEventHandler */
        public void mousePressed(final PInputEvent event) {
            if (event.isPopupTrigger()) {
                showContextMenu(event);
            }
        }

        /** @see PBasicInputEventHandler */
        public void mouseReleased(final PInputEvent event) {
            if (event.isPopupTrigger()) {
                showContextMenu(event);
            }
        }
    }

    /**
     * @return true if this is neuron has a sensory coupling attached.
     */
    public boolean isInput() {
        if (sensoryCoupling == null) {
            return false;
        } else {
            return true;
        }
    }
    /**
     * @return true if this is neuron has a motor coupling attached.
     */
    public boolean isOutput() {
        if (motorCoupling == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * @return Returns the neuron.
     */
    public Neuron getNeuron() {
        return neuron;
    }

    /**
     * @param neuron The neuron to set.
     */
    public void setNeuron(Neuron neuron) {
        this.neuron = neuron;
    }
    
    
}