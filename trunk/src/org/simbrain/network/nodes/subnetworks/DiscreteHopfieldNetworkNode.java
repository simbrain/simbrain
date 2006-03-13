package org.simbrain.network.nodes.subnetworks;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.dialog.network.DiscreteHopfieldPropertiesDialog;
import org.simbrain.network.nodes.SubnetworkNode;
import org.simnet.networks.DiscreteHopfield;

/**
 * <b>BackpropNetworkNode</b> is the graphical representation of a Backprop network.
 */
public class DiscreteHopfieldNetworkNode extends SubnetworkNode {

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
    public DiscreteHopfieldNetworkNode(final NetworkPanel networkPanel,
                                     final DiscreteHopfield subnetwork,
                                     final double x,
                                     final double y) {

        super(networkPanel, subnetwork, x, y);

        randomizeAction = new AbstractAction("Randomize Discrete Hopfield network") {
            public void actionPerformed(final ActionEvent event) {
                subnetwork.randomizeWeights();
            }
        };

        trainAction = new AbstractAction("Train Discrete Hopfield network") {
            public void actionPerformed(final ActionEvent event) {
                subnetwork.train();
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
        JPopupMenu contextMenu = new JPopupMenu();
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
        return new DiscreteHopfieldPropertiesDialog(getDiscreteHopfieldSubnetwork()); }

    /** @see ScreenElement. */
    public DiscreteHopfield getDiscreteHopfieldSubnetwork() {
        return ((DiscreteHopfield) getSubnetwork());
    }

}
