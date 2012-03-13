package org.simbrain.network.gui.nodes.subnetworks;

import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.simbrain.network.subnetworks.WinnerTakeAll;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.network.WTAPropertiesDialog;
import org.simbrain.network.gui.nodes.SubnetworkNode;

/**
 * <b>BackpropNetworkNode</b> is the graphical representation of a Backprop network.
 */
public class WTANetworkNode extends SubnetworkNode {


    /**
     * Create a new CompetitiveNetworkNode.
     *
     * @param networkPanel reference to network panel
     * @param subnetwork reference to subnetwork
     * @param x initial x position
     * @param y initial y position
     */
    public WTANetworkNode(final NetworkPanel networkPanel,
                                     final WinnerTakeAll subnetwork,
                                     final double x,
                                     final double y) {

        //REDO
        super(networkPanel, null, x, y);
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
        contextMenu.add(super.getSetPropertiesAction());
        return contextMenu;

    }

    /** @see org.simbrain.network.gui.nodes.ScreenElement */
    protected boolean hasPropertyDialog() {
        return true;
    }

    /** @see org.simbrain.network.gui.nodes.ScreenElement */
    protected JDialog getPropertyDialog() {
        return new WTAPropertiesDialog(getWTASubnetwork()); }

    /** @see org.simbrain.network.gui.nodes.ScreenElement */
    public WinnerTakeAll getWTASubnetwork() {
        // REDO
        return null;
        //return ((WinnerTakeAll) getSubnetwork());
    }

}
