package org.simbrain.network.nodes.subnetworks;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.dialog.network.LMSPropertiesDialog;
import org.simbrain.network.dialog.network.LMSTrainingDialog;
import org.simbrain.network.nodes.SubnetworkNode;
import org.simnet.networks.LMSNetwork;

/**
 * <b>BackpropNetworkNode</b> is the graphical representation of a Backprop network.
 */
public class LMSNetworkNode extends SubnetworkNode {

    /** Randomize network action. */
    private Action randomizeAction;

    /** Train network action. */
    private Action trainAction;

    /**
     * Create a new LMSNetworkNode.
     *
     * @param networkPanel reference to network panel
     * @param subnetwork reference to subnetwork
     * @param x initial x position
     * @param y initial y position
     */
    public LMSNetworkNode(final NetworkPanel networkPanel,
                                     final LMSNetwork subnetwork,
                                     final double x,
                                     final double y) {

        super(networkPanel, subnetwork, x, y);

        randomizeAction = new AbstractAction("Randomize LMS network") {
            public void actionPerformed(final ActionEvent event) {
                subnetwork.randomizeWeights();
                subnetwork.getRootNetwork().fireNetworkChanged();
            }
        };

        trainAction = new AbstractAction("Train LMS network") {
            public void actionPerformed(final ActionEvent event) {
                JDialog propertyDialog = new LMSTrainingDialog((LMSNetwork) subnetwork);
                propertyDialog.pack();
                propertyDialog.setLocationRelativeTo(null);
                propertyDialog.setVisible(true);
                subnetwork.getRootNetwork().fireNetworkChanged();
            }
        };
    }

    /** @see org.simbrain.network.nodes.ScreenElement */
    protected boolean hasToolTipText() {
        return true;
    }

    /** @see org.simbrain.network.nodes.ScreenElement */
    protected String getToolTipText() {
        return "LMS Network";
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
        contextMenu.add(trainAction);
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
        return new LMSPropertiesDialog(getLMSSubnetwork()); }

    /** @see org.simbrain.network.nodes.ScreenElement */
    public LMSNetwork getLMSSubnetwork() {
        return ((LMSNetwork) getSubnetwork());
    }

}
