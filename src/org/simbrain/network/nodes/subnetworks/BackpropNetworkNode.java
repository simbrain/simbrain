package org.simbrain.network.nodes.subnetworks;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.dialog.network.BackpropPropertiesDialog;
import org.simbrain.network.dialog.network.BackpropTrainingDialog;
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

    /** Tab height. */
    public static final double LAYER_INSET = 2.0d;

    /** Outline for input layer. */
    private PPath inputLayerOutline = new PPath();

    /** Outline for hidden layer. */
    private PPath hiddenLayerOutline = new PPath();

    /** Outline for output layer. */
    private PPath outputLayerOutline = new PPath();

    /** Reference to input nodes. */
    private ArrayList<NeuronNode> inputNodes = new ArrayList<NeuronNode>();

    /** Reference to hidde nodes. */
    private ArrayList<NeuronNode> hiddenNodes = new ArrayList<NeuronNode>();

    /** Refrence to output nodes. */
    private ArrayList<NeuronNode> outputNodes = new ArrayList<NeuronNode>();

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
    }

    /** @see SubnetworkNode. */
    protected void updateOutlineBoundsAndPath() {
        super.updateOutlineBoundsAndPath();

        PBounds inputBounds = new PBounds();
        PBounds hiddenBounds = new PBounds();
        PBounds outputBounds = new PBounds();

        for (NeuronNode node : inputNodes) {
            PBounds childBounds = node.getBounds();
            node.localToParent(childBounds);
            inputBounds.add(childBounds);
        }
        for (NeuronNode node : hiddenNodes) {
            PBounds childBounds = node.getBounds();
            node.localToParent(childBounds);
            hiddenBounds.add(childBounds);
        }
        for (NeuronNode node : outputNodes) {
            PBounds childBounds = node.getBounds();
            node.localToParent(childBounds);
            outputBounds.add(childBounds);
        }

        inputBounds.setRect(inputBounds.getX() - LAYER_INSET,
                inputBounds.getY() - LAYER_INSET,
                inputBounds.getWidth() + (2 * LAYER_INSET),
                inputBounds.getHeight() + (2 * LAYER_INSET));

        inputLayerOutline.setPathToRectangle((float) inputBounds.getX(), (float) inputBounds.getY(),
                            (float) inputBounds.getWidth(), (float) inputBounds.getHeight());

        hiddenBounds.setRect(hiddenBounds.getX() - LAYER_INSET,
                hiddenBounds.getY() - LAYER_INSET,
                hiddenBounds.getWidth() + (2 * LAYER_INSET),
                hiddenBounds.getHeight() + (2 * LAYER_INSET));

        hiddenLayerOutline.setPathToRectangle((float) hiddenBounds.getX(), (float) hiddenBounds.getY(),
                            (float) hiddenBounds.getWidth(), (float) hiddenBounds.getHeight());

        outputBounds.setRect(outputBounds.getX() - LAYER_INSET,
                outputBounds.getY() - LAYER_INSET,
                outputBounds.getWidth() + (2 * LAYER_INSET),
                outputBounds.getHeight() + (2 * LAYER_INSET));

        outputLayerOutline.setPathToRectangle((float) outputBounds.getX(), (float) outputBounds.getY(),
                            (float) outputBounds.getWidth(), (float) outputBounds.getHeight());

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

}
