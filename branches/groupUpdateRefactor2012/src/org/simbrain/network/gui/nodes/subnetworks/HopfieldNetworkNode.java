package org.simbrain.network.gui.nodes.subnetworks;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.simbrain.network.groups.Hopfield;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.network.HopfieldPropertiesDialog;
import org.simbrain.network.gui.nodes.SubnetworkNode;

/**
 * <b>BackpropNetworkNode</b> is the graphical representation of a Backprop network.
 */
public class HopfieldNetworkNode extends SubnetworkNode {

    /** Randomize network action. */
    private Action randomizeAction;

    /** Train network action. */
    private Action trainAction;

    /**
     * Create a new CompetitiveNetworkNode.
     *
     * @param networkPanel reference to network panel
     * @param subnetwork reference to subnetwork
     * @param x initial x position
     * @param y initial y position
     */
    public HopfieldNetworkNode(final NetworkPanel networkPanel,
                                     final Hopfield subnetwork,
                                     final double x,
                                     final double y) {

        //REDO
        super(networkPanel, null, x, y);

        randomizeAction = new AbstractAction("Randomize Discrete Hopfield network") {
            public void actionPerformed(final ActionEvent event) {
                subnetwork.randomizeWeights();
            }
        };

        trainAction = new AbstractAction("Train Discrete Hopfield network") {
            public void actionPerformed(final ActionEvent event) {
                subnetwork.train();
            }
        };
    }

    /** @see org.simbrain.network.gui.nodes.ScreenElement */
    protected boolean hasToolTipText() {
        return true;
    }

    /** @see org.simbrain.network.gui.nodes.ScreenElement */
    protected String getToolTipText() {
        return "Backprop Network";
    }

    /** @see org.simbrain.network.gui.nodes.ScreenElement */
    protected boolean hasContextMenu() {
        return true;
    }

    /** @see org.simbrain.network.gui.nodes.ScreenElement */
    protected JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = super.getContextMenu();
        contextMenu.add(randomizeAction);
        contextMenu.addSeparator();
        contextMenu.add(trainAction);
        contextMenu.addSeparator();
        contextMenu.add(super.getSetPropertiesAction());
        return contextMenu;

    }

    /** @see org.simbrain.network.gui.nodes.ScreenElement */
    protected boolean hasPropertyDialog() {
        return true;
    }

    /** @see org.simbrain.network.gui.nodes.ScreenElement */
    protected JDialog getPropertyDialog() {
        return new HopfieldPropertiesDialog(getHopfieldSubnetwork()); }

    /** @see org.simbrain.network.gui.nodes.ScreenElement */
    public Hopfield getHopfieldSubnetwork() {
        //REDO
        return null;
        //return ((Hopfield) getSubnetwork());
    }

}
