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
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.network.SOMPropertiesDialog;
import org.simbrain.network.gui.dialogs.network.SOMTrainingDialog;
import org.simbrain.network.gui.nodes.InteractionBox;
import org.simbrain.network.listeners.NetworkListener;
import org.simbrain.network.subnetworks.SOM;
import org.simbrain.util.Utils;

/**
 * PNode representation of Self-Organizing Map.
 *
 * @author jyoshimi
 */
public class SOMNode extends SubnetworkNode {

    /**
     * Create a SOM Network PNode.
     *
     * @param networkPanel parent panel
     * @param group the SOM network
     */
    public SOMNode(final NetworkPanel networkPanel, final SOM group) {
        super(networkPanel, group);
        // setStrokePaint(Color.green);
        setInteractionBox(new SOMInteractionBox(networkPanel));
        setContextMenu();
        setOutlinePadding(15f);
        networkPanel.getNetwork().addNetworkListener(new NetworkListener() {

            public void networkChanged() {
                group.setLabel("SOM - Learning rate:"
                        + Utils.round(group.getAlpha(), 2) + " N-size:"
                        + Utils.round(group.getNeighborhoodSize(), 2));
            }

            public void networkUpdateMethodChanged() {
            }

            public void neuronClampToggled() {
            }

            public void synapseClampToggled() {
            }

        });
    }

    /**
     * Custom interaction box for SOM group node.
     */
    private class SOMInteractionBox extends InteractionBox {
        public SOMInteractionBox(NetworkPanel net) {
            super(net, SOMNode.this);
        }

        @Override
        protected String getToolTipText() {
            return "Current learning rate: "
                    + Utils.round(((SOM) getGroup()).getAlpha(), 2)
                    + "  Current neighborhood size: "
                    + Utils.round(((SOM) getGroup()).getNeighborhoodSize(), 2);
        }

        @Override
        protected boolean hasPropertyDialog() {
            return true;
        }

        @Override
        protected JDialog getPropertyDialog() {
            return new SOMPropertiesDialog((SOM) getGroup());
        }

        @Override
        protected boolean hasToolTipText() {
            return true;
        }
    };

    /**
     * Sets custom menu for SOM node.
     */
    private void setContextMenu() {
        JPopupMenu menu = super.getDefaultContextMenu();
        menu.addSeparator();
        menu.add(new JMenuItem(new AbstractAction("Reset Network") {
            public void actionPerformed(final ActionEvent event) {
                ((SOM) getGroup()).reset();
                ((SOM) getGroup()).getParentNetwork().fireNetworkChanged();
            }
        }));
        menu.add(new JMenuItem(new AbstractAction("Recall") {
            public void actionPerformed(final ActionEvent event) {
                ((SOM) getGroup()).recall();
                ((SOM) getGroup()).getParentNetwork().fireNetworkChanged();
            }
        }));
        menu.add(new JMenuItem(new AbstractAction("Randomize SOM Weights") {
            public void actionPerformed(final ActionEvent event) {
                ((SOM) getGroup()).randomizeIncomingWeights();
                ((SOM) getGroup()).getParentNetwork().fireNetworkChanged();
            }
        }));
        menu.add(new JMenuItem(new AbstractAction("Train SOM Network") {
            // TODO: Integrate below in to training framework?
            public void actionPerformed(final ActionEvent event) {
                JDialog propertyDialog = new SOMTrainingDialog((SOM) getGroup());
                propertyDialog.pack();
                propertyDialog.setLocationRelativeTo(null);
                propertyDialog.setVisible(true);
                ((SOM) getGroup()).getParentNetwork().fireNetworkChanged();
            }
        }));

        setContextMenu(menu);
    }

}
