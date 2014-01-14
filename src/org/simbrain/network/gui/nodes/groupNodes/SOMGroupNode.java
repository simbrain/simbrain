/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.gui.nodes.groupNodes;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.network.SOMTrainingDialog;
import org.simbrain.network.gui.nodes.InteractionBox;
import org.simbrain.network.listeners.NetworkListener;
import org.simbrain.network.subnetworks.SOMGroup;
import org.simbrain.util.Utils;

/**
 * PNode representation of Self-Organizing Map.
 *
 * @author jyoshimi
 */
public class SOMGroupNode extends NeuronGroupNode {

    /**
     * Create a SOM Network PNode.
     *
     * @param networkPanel parent panel
     * @param group the SOM network
     */
    public SOMGroupNode(final NetworkPanel networkPanel, final SOMGroup group) {
        super(networkPanel, group);
        // setStrokePaint(Color.green);
        setCustomMenuItems();
        setInteractionBox(new SOMInteractionBox(networkPanel));
        // setOutlinePadding(15f);
        networkPanel.getNetwork().addNetworkListener(new NetworkListener() {

            public void networkChanged() {
                group.setStateInfo("Learning rate ("
                        + Utils.round(group.getAlpha(), 2) + ") N-size ("
                        + Utils.round(group.getNeighborhoodSize(), 2) + ")");
            }

        });
    }

    /**
     * Custom interaction box for SOM group node.
     */
    private class SOMInteractionBox extends NeuronGroupInteractionBox {
        public SOMInteractionBox(NetworkPanel net) {
            super(net);
        }

        @Override
        protected String getToolTipText() {
            return "Current learning rate: "
                    + Utils.round(((SOMGroup) getNeuronGroup()).getAlpha(), 2)
                    + "  Current neighborhood size: "
                    + Utils.round(
                            ((SOMGroup) getNeuronGroup()).getNeighborhoodSize(),
                            2);
        }

        @Override
        protected boolean hasToolTipText() {
            return true;
        }
    };

    /**
     * Sets custom menu for SOM node.
     */
    protected void setCustomMenuItems() {
        super.addCustomMenuItem(new JMenuItem(new AbstractAction(
                "Reset SOM Network") {
            public void actionPerformed(final ActionEvent event) {
                ((SOMGroup) getNeuronGroup()).reset();
                ((SOMGroup) getNeuronGroup()).getParentNetwork()
                        .fireNetworkChanged();
            }
        }));
        super.addCustomMenuItem(new JMenuItem(new AbstractAction(
                "Recall SOM Memory") {
            public void actionPerformed(final ActionEvent event) {
                ((SOMGroup) getNeuronGroup()).recall();
                ((SOMGroup) getNeuronGroup()).getParentNetwork()
                        .fireNetworkChanged();
            }
        }));
        super.addCustomMenuItem(new JMenuItem(new AbstractAction(
                "Randomize SOM Weights") {
            public void actionPerformed(final ActionEvent event) {
                ((SOMGroup) getNeuronGroup()).randomizeIncomingWeights();
                ((SOMGroup) getNeuronGroup()).getParentNetwork()
                        .fireNetworkChanged();
            }
        }));
    }

}
