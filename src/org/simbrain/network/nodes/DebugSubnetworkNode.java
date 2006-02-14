
package org.simbrain.network.nodes;

import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import org.simbrain.network.NetworkPanel;

import org.simnet.interfaces.Network;

/**
 * Debug subnetwork node.
 */
public final class DebugSubnetworkNode
    extends SubnetworkNode2 {

    /**
     * Create a new debug subnetwork node.
     */
    public DebugSubnetworkNode(final NetworkPanel networkPanel,
                               final Network subnetwork,
                               final double x, final double y) {

        super(networkPanel, subnetwork, x, y);
    }


    /** @see ScreenElement */
    protected boolean hasToolTipText() {
        return true;
    }

    /** @see ScreenElement */
    protected String getToolTipText() {
        return getLabel();
    }

    /** @see ScreenElement */
    protected boolean hasContextMenu() {
        return true;
    }

    /** @see ScreenElement */
    protected JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        contextMenu.add(new JMenu("Debug 0"));
        contextMenu.add(new JMenu("Debug 1"));
        return contextMenu;
    }

    /** @see ScreenElement */
    protected boolean hasPropertyDialog() {
        return false;
    }

    /** @see ScreenElement */
    protected JDialog getPropertyDialog() {
        return null;
    }
}