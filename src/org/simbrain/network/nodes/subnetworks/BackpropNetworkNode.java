package org.simbrain.network.nodes.subnetworks;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.dialog.network.BackpropPropertiesDialog;
import org.simbrain.network.dialog.network.BackpropTrainingDialog;
import org.simbrain.network.nodes.CustomOutline;
import org.simbrain.network.nodes.NeuronNode;
import org.simbrain.network.nodes.SubnetworkNode;
import org.simnet.interfaces.Neuron;
import org.simnet.networks.Backprop;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.util.PBounds;

/**
 * <b>BackpropNetworkNode</b> is the graphical representation of a Backprop network.
 */
public class BackpropNetworkNode extends SubnetworkNode {

    /** Randomize network action. */
    private Action randomizeAction;

    /** Train network action. */
    private Action trainAction;

    /** Dash style. */
    private static final float[] DASH = {3.0f};

    /** Dash Stroke. */
    private static final BasicStroke DASHED = new BasicStroke(.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 2.0f, DASH, 0.0f);

    /** Layer outline inset. */
    private static final Color LAYER_COLOR = Color.GRAY;

    /** Outline for input layer. */
    private CustomOutline inputLayerOutline = new CustomOutline();

    /** Outline for hidden layer. */
    private CustomOutline hiddenLayerOutline = new CustomOutline();

    /** Outline for output layer. */
    private CustomOutline outputLayerOutline = new CustomOutline();

    /**
     * Create a new CompetitiveNetworkNode.
     *
     * @param networkPanel reference to network panel
     * @param subnetwork reference to subnetwork
     * @param x initial x position
     * @param y initial y position
     */
    public BackpropNetworkNode(final NetworkPanel networkPanel,
                                     final Backprop subnetwork,
                                     final double x,
                                     final double y) {

        super(networkPanel, subnetwork, x, y);

        inputLayerOutline.setStroke(DASHED);
        inputLayerOutline.setStrokePaint(LAYER_COLOR);
        hiddenLayerOutline.setStroke(DASHED);
        hiddenLayerOutline.setStrokePaint(LAYER_COLOR);
        outputLayerOutline.setStroke(DASHED);
        outputLayerOutline.setStrokePaint(LAYER_COLOR);

        addChild(inputLayerOutline);
        addChild(hiddenLayerOutline);
        addChild(outputLayerOutline);

        randomizeAction = new AbstractAction("Randomize Backprop network") {
            public void actionPerformed(final ActionEvent event) {
                subnetwork.randomize();
                subnetwork.getRootNetwork().fireNetworkChanged();
            }
        };

        trainAction = new AbstractAction("Train Backprop network") {
            public void actionPerformed(final ActionEvent event) {
                JDialog propertyDialog = new BackpropTrainingDialog((Backprop) subnetwork);
                propertyDialog.pack();
                propertyDialog.setLocationRelativeTo(null);
                propertyDialog.setVisible(true);
                subnetwork.getRootNetwork().fireNetworkChanged();
            }
        };
    }

    /**
     * Set references to layers.
     */
    public void init() {

        ArrayList<PNode> inputNodes = new ArrayList<PNode>();
        ArrayList<PNode> hiddenNodes = new ArrayList<PNode>();
        ArrayList<PNode> outputNodes = new ArrayList<PNode>();

        Backprop subnetwork = (Backprop) this.getSubnetwork();
        for (Iterator i = this.getChildrenIterator(); i.hasNext(); ) {
            PNode node = (PNode) i.next();
            if (node instanceof NeuronNode) {
                Neuron neuron = ((NeuronNode) node).getNeuron();
                if (subnetwork.getNetworkList().get(0).getNeuronList().contains(neuron)) {
                    inputNodes.add((NeuronNode) node);
                } else if (subnetwork.getNetworkList().get(1).getNeuronList().contains(neuron)) {
                    hiddenNodes.add((NeuronNode) node);
                } else if (subnetwork.getNetworkList().get(2).getNeuronList().contains(neuron)) {
                    outputNodes.add((NeuronNode) node);
                }
            }
        }
        inputLayerOutline.setOutlinedObjects(inputNodes);
        hiddenLayerOutline.setOutlinedObjects(hiddenNodes);
        outputLayerOutline.setOutlinedObjects(outputNodes);
    }

    /** @see SubnetworkNode. */
    protected void updateOutlineBoundsAndPath() {
        super.updateOutlineBoundsAndPath();

        inputLayerOutline.updateBounds();
        hiddenLayerOutline.updateBounds();
        outputLayerOutline.updateBounds();
    }
    /** @see org.simbrain.network.nodes.ScreenElement */
    protected boolean hasToolTipText() {
        return true;
    }

    /** @see org.simbrain.network.nodes.ScreenElement */
    protected String getToolTipText() {
        return "Backprop Network";
    }

    /** @see org.simbrain.network.nodes.ScreenElement */
    protected boolean hasContextMenu() {
        return true;
    }

    /** @see org.simbrain.network.nodes.ScreenElement */
    protected JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = super.getContextMenu();
        contextMenu.add(randomizeAction);
        contextMenu.addSeparator();
        contextMenu.add(trainAction);
        contextMenu.addSeparator();
        contextMenu.add(super.getSetPropertiesAction());
        return contextMenu;

    }

    /** @see org.simbrain.network.nodes.ScreenElement */
    protected boolean hasPropertyDialog() {
        return true;
    }

    /** @see org.simbrain.network.nodes.ScreenElement */
    protected JDialog getPropertyDialog() {
        return new BackpropPropertiesDialog(getBackpropSubnetwork()); }

    /** @see org.simbrain.network.nodes.ScreenElement */
    public Backprop getBackpropSubnetwork() {
        return ((Backprop) getSubnetwork());
    }

    /** @see PNode */
    public PNode removeChild(final PNode child) {
        PNode ret = super.removeChild(child);

        if (inputLayerOutline.getOutlinedObjects().contains(child)) {
            inputLayerOutline.removeOutlinedObject(child);
        } else if (hiddenLayerOutline.getOutlinedObjects().contains(child)) {
            hiddenLayerOutline.removeOutlinedObject(child);
        } else if (outputLayerOutline.getOutlinedObjects().contains(child)) {
            outputLayerOutline.removeOutlinedObject(child);
        }

        updateOutlineBoundsAndPath();
        return ret;
    }


}
