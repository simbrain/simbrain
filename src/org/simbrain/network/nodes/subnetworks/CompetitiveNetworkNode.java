package org.simbrain.network.nodes.subnetworks;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.dialog.network.CompetitivePropertiesDialog;
import org.simbrain.network.nodes.SubnetworkNode;
import org.simnet.networks.Competitive;

/**
 * <b>CompetitiveNetworkNode</b> is the graphical representation of Competitive Networks.
 */
public class CompetitiveNetworkNode extends SubnetworkNode {

    /** Normalize network action. */
    private Action normalizeAction;

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
    public CompetitiveNetworkNode(final NetworkPanel networkPanel,
                                     final Competitive subnetwork,
                                     final double x,
                                     final double y) {

        super(networkPanel, subnetwork, x, y);

        normalizeAction = new AbstractAction("Normalize network") {
            public void actionPerformed(final ActionEvent event) {
                subnetwork.normalizeIncomingWeights();
                subnetwork.fireNetworkChanged();
            }
        };

        randomizeAction = new AbstractAction("Randomize Competitive network") {
            public void actionPerformed(final ActionEvent event) {
                subnetwork.randomize();
                subnetwork.fireNetworkChanged();
            }
        };
    }

    /** @see ScreenElement. */
    protected boolean hasToolTipText() {
        return true;
    }

    /** @see ScreenElement. */
    protected String getToolTipText() {
        return "Competitive Network";
    }

    /** @see ScreenElement. */
    protected boolean hasContextMenu() {
        return true;
    }

    /** @see ScreenElement. */
    protected JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = super.getContextMenu();
        contextMenu.add(randomizeAction);
        contextMenu.addSeparator();
        contextMenu.add(normalizeAction);
        contextMenu.addSeparator();
        contextMenu.add(super.getSetPropertiesAction());
        return contextMenu;

    }

    /** @see ScreenElement. */
    protected boolean hasPropertyDialog() {
        return true;
    }

    /** @see ScreenElement. */
    protected JDialog getPropertyDialog() {
        return new CompetitivePropertiesDialog(getCompetitiveSubnetwork()); }

    /** @see ScreenElement. */
    public Competitive getCompetitiveSubnetwork() {
        return ((Competitive) getSubnetwork());
    }

}
