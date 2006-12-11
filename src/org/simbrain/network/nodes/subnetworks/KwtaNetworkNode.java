package org.simbrain.network.nodes.subnetworks;

import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.dialog.network.HopfieldPropertiesDialog;
import org.simbrain.network.dialog.network.KwtaNetworkDialog;
import org.simbrain.network.dialog.network.KwtaPropertiesDialog;
import org.simbrain.network.dialog.network.WTAPropertiesDialog;
import org.simbrain.network.nodes.SubnetworkNode;
import org.simnet.networks.Hopfield;
import org.simnet.networks.KwtaNetwork;
import org.simnet.networks.StandardNetwork;
import org.simnet.networks.WinnerTakeAll;

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
                                     final KwtaNetwork network,
                                     final double x,
                                     final double y) {

        super(networkPanel, network, x, y);
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
    public KwtaNetwork getKwtaSubnetwork() {
        return ((KwtaNetwork) getSubnetwork());
    }

    /** @inheritDoc org.simbrain.network.nodes.ScreenElement */
    protected boolean hasPropertyDialog() {
        return true;
    }



}
