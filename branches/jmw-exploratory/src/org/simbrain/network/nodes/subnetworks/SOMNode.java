package org.simbrain.network.nodes.subnetworks;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.dialog.network.SOMPropertiesDialog;
import org.simbrain.network.dialog.network.SOMTrainingDialog;
import org.simbrain.network.nodes.SubnetworkNode;
import org.simnet.networks.SOM;

/**
 * <b>SOMNode</b> is the graphical representation of SOM Networks.
 */
public class SOMNode extends SubnetworkNode {

    /** Reset network action. */
    private Action resetAction;

    /** Randomize network action. */
    private Action randomizeAction;

    /** Train SOM network action. */
    private Action trainAction;

    /** Recall SOM network action. */
    private Action recallAction;

    /**
     * Create a new SOMNode.
     *
     * @param networkPanel reference to network panel
     * @param subnetwork reference to subnetwork
     * @param x initial x position
     * @param y initial y position
     */
    public SOMNode(final NetworkPanel networkPanel,
                                     final SOM subnetwork,
                                     final double x,
                                     final double y) {

        super(networkPanel, subnetwork, x, y);

        resetAction = new AbstractAction("Reset Network") {
            public void actionPerformed(final ActionEvent event) {
                subnetwork.reset();
                subnetwork.getRootNetwork().fireNetworkChanged();
            }
        };

        recallAction = new AbstractAction("Recall") {
            public void actionPerformed(final ActionEvent event) {
                subnetwork.recall();
                subnetwork.getRootNetwork().fireNetworkChanged();
            }
        };

        randomizeAction = new AbstractAction("Randomize SOM Weights") {
            public void actionPerformed(final ActionEvent event) {
                subnetwork.randomizeIncomingWeights();
                subnetwork.getRootNetwork().fireNetworkChanged();
            }
        };

        trainAction = new AbstractAction("Train SOM Network") {
            public void actionPerformed(final ActionEvent event) {
                JDialog propertyDialog = new SOMTrainingDialog((SOM) subnetwork);
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
        return "Current learning rate: " + getSOMSubnetwork().getAlpha() + "  Current neighborhood size: "
            + getSOMSubnetwork().getNeighborhoodSize();
    }

    /** @see org.simbrain.network.nodes.ScreenElement */
    protected boolean hasContextMenu() {
        return true;
    }

    /** @see org.simbrain.network.nodes.ScreenElement */
    protected JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = super.getContextMenu();
        contextMenu.add(randomizeAction);
        contextMenu.add(resetAction);
        contextMenu.add(recallAction);
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
        return new SOMPropertiesDialog(getSOMSubnetwork()); }

    /** @see org.simbrain.network.nodes.ScreenElement */
    public SOM getSOMSubnetwork() {
        return ((SOM) getSubnetwork());
    }

}
