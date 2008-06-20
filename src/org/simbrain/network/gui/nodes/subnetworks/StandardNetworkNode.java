package org.simbrain.network.gui.nodes.subnetworks;

import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.SubnetworkNode;
import org.simbrain.network.networks.StandardNetwork;

/**
 * <b>StandardNetworkNode</b> is the graphical representation of a standard network.
 */
public class StandardNetworkNode extends SubnetworkNode {


    /**
     * Create a new StandardNetworkNode.
     *
     * @param networkPanel reference to network panel
     * @param subnetwork reference to subnetwork
     * @param x initial x position
     * @param y initial y position
     */
    public StandardNetworkNode(final NetworkPanel networkPanel,
                                     final StandardNetwork subnetwork,
                                     final double x,
                                     final double y) {

        super(networkPanel, subnetwork, x, y);
    }

    /** @see org.simbrain.network.gui.nodes.ScreenElement */
    protected boolean hasToolTipText() {
        return true;
    }

    /** @see org.simbrain.network.gui.nodes.ScreenElement */
    protected String getToolTipText() {
        return "Standard Network";
    }

    /** @see org.simbrain.network.gui.nodes.ScreenElement */
    protected boolean hasContextMenu() {
        return true;
    }

    /** @see org.simbrain.network.gui.nodes.ScreenElement */
    protected JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = super.getContextMenu();
        contextMenu.add(super.getSetPropertiesAction());
        return contextMenu;

    }

    /** @see org.simbrain.network.gui.nodes.ScreenElement */
    protected boolean hasPropertyDialog() {
        return false;
    }

    protected JDialog getPropertyDialog() {
        // TODO Auto-generated method stub
        return null;
    }


}
