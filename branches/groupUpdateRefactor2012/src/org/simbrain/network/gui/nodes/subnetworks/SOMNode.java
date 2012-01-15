///*
// * Part of Simbrain--a java-based neural network kit
// * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
// *
// * This program is free software; you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation; either version 2 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software
// * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// */
//package org.simbrain.network.gui.nodes.subnetworks;
//
//import java.awt.event.ActionEvent;
//
//import javax.swing.AbstractAction;
//import javax.swing.Action;
//import javax.swing.JDialog;
//import javax.swing.JPopupMenu;
//
//import org.simbrain.network.groups.SOM;
//import org.simbrain.network.gui.NetworkPanel;
//import org.simbrain.network.gui.dialogs.network.SOMPropertiesDialog;
//import org.simbrain.network.gui.dialogs.network.SOMTrainingDialog;
//import org.simbrain.network.gui.nodes.SubnetworkNode;
//import org.simbrain.network.listeners.NetworkListener;
//import org.simbrain.util.Utils;
//
///**
// * <b>SOMNode</b> is the graphical representation of SOM Networks.
// */
//public class SOMNode extends SubnetworkNode {
//
//    /** Reset network action. */
//    private Action resetAction;
//
//    /** Randomize network action. */
//    private Action randomizeAction;
//
//    /** Train SOM network action. */
//    private Action trainAction;
//
//    /** Recall SOM network action. */
//    private Action recallAction;
//
//    /**
//     * Create a new SOMNode.
//     *
//     * @param networkPanel reference to network panel
//     * @param subnetwork reference to subnetwork
//     * @param x initial x position
//     * @param y initial y position
//     */
//    public SOMNode(final NetworkPanel networkPanel,
//                                     final SOM subnetwork,
//                                     final double x,
//                                     final double y) {
//
//        super(networkPanel, subnetwork, x, y);
//
//        subnetwork.getRootNetwork().addNetworkListener(new NetworkListener() {
//
//            public void networkChanged() {
//                SOMNode.this.setLabel("SOM - Learning rate:"
//                        + Utils.round(subnetwork.getAlpha(), 2) + " N-size:"
//                        + Utils.round(subnetwork.getNeighborhoodSize(), 2));
//            }
//
//            public void networkUpdateMethodChanged() {
//            }
//
//            public void neuronClampToggled() {
//            }
//
//            public void synapseClampToggled() {
//            }
//
//        });
//
//        resetAction = new AbstractAction("Reset Network") {
//            public void actionPerformed(final ActionEvent event) {
//                subnetwork.reset();
//                subnetwork.getRootNetwork().fireNetworkChanged();
//            }
//        };
//
//        recallAction = new AbstractAction("Recall") {
//            public void actionPerformed(final ActionEvent event) {
//                subnetwork.recall();
//                subnetwork.getRootNetwork().fireNetworkChanged();
//            }
//        };
//
//        randomizeAction = new AbstractAction("Randomize SOM Weights") {
//            public void actionPerformed(final ActionEvent event) {
//                subnetwork.randomizeIncomingWeights();
//                subnetwork.getRootNetwork().fireNetworkChanged();
//            }
//        };
//
//        trainAction = new AbstractAction("Train SOM Network") {
//            public void actionPerformed(final ActionEvent event) {
//                JDialog propertyDialog = new SOMTrainingDialog((SOM) subnetwork);
//                propertyDialog.pack();
//                propertyDialog.setLocationRelativeTo(null);
//                propertyDialog.setVisible(true);
//                subnetwork.getRootNetwork().fireNetworkChanged();
//            }
//        };
//    }
//
//    /** @see org.simbrain.network.gui.nodes.ScreenElement */
//    protected boolean hasToolTipText() {
//        return true;
//    }
//
//    /** @see org.simbrain.network.gui.nodes.ScreenElement */
//    protected String getToolTipText() {
//        return "Current learning rate: "
//                + Utils.round(getSOMSubnetwork().getAlpha(), 2)
//                + "  Current neighborhood size: "
//                + Utils.round(getSOMSubnetwork().getNeighborhoodSize(), 2);
//    }
//
//    /** @see org.simbrain.network.gui.nodes.ScreenElement */
//    protected boolean hasContextMenu() {
//        return true;
//    }
//
//    /** @see org.simbrain.network.gui.nodes.ScreenElement */
//    protected JPopupMenu getContextMenu() {
//        JPopupMenu contextMenu = super.getContextMenu();
//        contextMenu.add(randomizeAction);
//        contextMenu.add(resetAction);
//        contextMenu.add(recallAction);
//        contextMenu.addSeparator();
//        contextMenu.add(trainAction);
//        contextMenu.addSeparator();
//        contextMenu.add(super.getSetPropertiesAction());
//        return contextMenu;
//
//    }
//
//    /** @see org.simbrain.network.gui.nodes.ScreenElement */
//    protected boolean hasPropertyDialog() {
//        return true;
//    }
//
//    /** @see org.simbrain.network.gui.nodes.ScreenElement */
//    protected JDialog getPropertyDialog() {
//        return new SOMPropertiesDialog(getSOMSubnetwork()); }
//
//    /** @see org.simbrain.network.gui.nodes.ScreenElement */
//    public SOM getSOMSubnetwork() {
//        return ((SOM) getSubnetwork());
//    }
//
//}
