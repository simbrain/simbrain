package org.simbrain.network.gui.nodes.subnetworks;

import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.simbrain.network.groups.subnetworks.KWTA;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.network.KwtaPropertiesDialog;
import org.simbrain.network.gui.nodes.SubnetworkNode;

/**
 * <b>KwtaNetworkNode</b> takes care of initialization of a Kwta network.
 */
public class KwtaNetworkNode extends SubnetworkNode {


    /**
     * Create a new KwtaNetworkNode.
     *
     * @param networkPanel reference to network panel
     * @param network reference to subnetwork
     * @param x initial x position
     * @param y initial y position
     */
    public KwtaNetworkNode(final NetworkPanel networkPanel,
                                     final KWTA network,
                                     final double x,
                                     final double y) {

        super(networkPanel, null, x, y);
    }

    /** @inheritDoc org.simbrain.network.nodes.ScreenElement */
    protected boolean hasToolTipText() {
        return true;
    }

    /** @inheritDoc org.simbrain.network.nodes.ScreenElement */
    protected String getToolTipText() {
        return "" + getKwtaSubnetwork().getK() + " Winner Take All Network";
    }

    /** @inheritDoc org.simbrain.network.nodes.ScreenElement */
    protected boolean hasContextMenu() {
        return true;
    }

    /** @inheritDoc org.simbrain.network.nodes.ScreenElement */
    protected JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = super.getContextMenu();
        contextMenu.add(super.getSetPropertiesAction());
        return contextMenu;

    }

    /** @inheritDoc org.simbrain.network.nodes.ScreenElement */
    protected JDialog getPropertyDialog() {
        return new KwtaPropertiesDialog(getKwtaSubnetwork()); 
    }

    /** @inheritDoc org.simbrain.network.nodes.ScreenElement */
    public KWTA getKwtaSubnetwork() {
        return null; 
    }

    /** @inheritDoc org.simbrain.network.nodes.ScreenElement */
    protected boolean hasPropertyDialog() {
        return true;
    }



}
