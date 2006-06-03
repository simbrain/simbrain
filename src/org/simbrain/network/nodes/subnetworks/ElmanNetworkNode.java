package org.simbrain.network.nodes.subnetworks;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.nodes.SubnetworkNode;
import org.simnet.networks.Elman;

/**
 * <b>BackpropNetworkNode</b> is the graphical representation of a Backprop network.
 */
public class ElmanNetworkNode extends SubnetworkNode {

    /** Randomize network action. */
    private Action randomizeAction;

    /**
     * Create a new CompetitiveNetworkNode.
     *
     * @param networkPanel reference to network panel
     * @param subnetwork reference to subnetwork
     * @param x initial x position
     * @param y initial y position
     */
    public ElmanNetworkNode(final NetworkPanel networkPanel,
                                     final Elman subnetwork,
                                     final double x,
                                     final double y) {

        super(networkPanel, subnetwork, x, y);

        randomizeAction = new AbstractAction("Randomize Elman network") {
            public void actionPerformed(final ActionEvent event) {
                subnetwork.randomize();
                subnetwork.fireNetworkChanged();
            }
        };
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
        contextMenu.add(super.getSetPropertiesAction());
        return contextMenu;

    }

    /** @see org.simbrain.network.nodes.ScreenElement */
    protected boolean hasPropertyDialog() {
        return true;
    }

    /** @see org.simbrain.network.nodes.ScreenElement */
    protected JDialog getPropertyDialog() {
        return null;
    }

    /** @see org.simbrain.network.nodes.ScreenElement */
    public Elman getBackpropSubnetwork() {
        return ((Elman) getSubnetwork());
    }

}
