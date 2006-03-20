package org.simbrain.network.nodes.subnetworks;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.dialog.network.BackpropPropertiesDialog;
import org.simbrain.network.dialog.network.BackpropTrainingDialog;
import org.simbrain.network.nodes.SubnetworkNode;
import org.simnet.networks.Backprop;

/**
 * <b>BackpropNetworkNode</b> is the graphical representation of a Backprop network.
 */
public class BackpropNetworkNode extends SubnetworkNode {

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
    public BackpropNetworkNode(final NetworkPanel networkPanel,
                                     final Backprop subnetwork,
                                     final double x,
                                     final double y) {

        super(networkPanel, subnetwork, x, y);

        randomizeAction = new AbstractAction("Randomize Backprop network") {
            public void actionPerformed(final ActionEvent event) {
                subnetwork.randomize();
                subnetwork.fireNetworkChanged();
            }
        };

        trainAction = new AbstractAction("Train Backprop network") {
            public void actionPerformed(final ActionEvent event) {
                JDialog propertyDialog = new BackpropTrainingDialog((Backprop) subnetwork);
                propertyDialog.pack();
                propertyDialog.setLocationRelativeTo(null);
                propertyDialog.setVisible(true);
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
        return "Backprop Network";
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
        contextMenu.add(trainAction);
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
        return new BackpropPropertiesDialog(getBackpropSubnetwork()); }

    /** @see ScreenElement. */
    public Backprop getBackpropSubnetwork() {
        return ((Backprop) getSubnetwork());
    }

}
