
package org.simbrain.network.nodes.subnets;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.AbstractAction;
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

    /** Specific action 0. */
    private final Action specificAction0;

    /** Specific action 1. */
    private final Action specificAction1;


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

        specificAction0 = new AbstractAction("Specific action 0") {
                /** @see AbstractAction */
                public void actionPerformed(final ActionEvent e) {
                    // empty
                }
            };

        specificAction1 = new AbstractAction("Specific action 1") {
                /** @see AbstractAction */
                public void actionPerformed(final ActionEvent e) {
                    // empty
                }
            };
    }


    /** @see SubnetworkNode */
    protected boolean hasContextMenu() {
        return true;
    }

    /** @see SubnetworkNode */
    protected JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        contextMenu.add(getShowOutlineAction());
        contextMenu.add(getHideOutlineAction());
        contextMenu.addSeparator();
        contextMenu.add(specificAction0);
        contextMenu.add(specificAction1);
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

