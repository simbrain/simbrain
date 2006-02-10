
package org.simbrain.network.nodes.subnets;

import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.simbrain.network.NetworkPanel;

import org.simbrain.network.dialog.network.CompetitivePropertiesDialog;
import org.simbrain.network.nodes.SubnetworkNode;

import org.simnet.networks.Competitive;

/**
 * Competitive subnetwork node.
 */
public final class CompetitiveSubnetworkNode
    extends SubnetworkNode {

    /**
     * Create a new competitive subnetwork node.
     *
     * @param networkPanel network panel
     * @param subnetwork competitive subnetwork
     * @param x x
     * @param y y
     */
    public CompetitiveSubnetworkNode(final NetworkPanel networkPanel,
                                     final Competitive subnetwork,
                                     final double x, final double y) {

        super(networkPanel, subnetwork, x, y);

        // TODO:
        // create some actions
    }


    /** @see SubnetworkNode */
    protected boolean hasContextMenu() {
        return true;
    }

    /** @see SubnetworkNode */
    protected JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu("Implement me!");
        return contextMenu;
    }

    /** @see SubnetworkNode */
    protected boolean hasPropertyDialog() {
        return true;
    }

    /** @see SubnetworkNode */
    protected JDialog getPropertyDialog() {
        return new CompetitivePropertiesDialog((Competitive) getSubnetwork());
    }
}

