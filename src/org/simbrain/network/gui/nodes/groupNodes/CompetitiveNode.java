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
import org.simbrain.network.gui.dialogs.network.CompetitivePropertiesDialog;
import org.simbrain.network.gui.nodes.InteractionBox;
import org.simbrain.network.subnetworks.Competitive;

/**
 * PNode representation of Competitive network.
 *
 * @author jyoshimi
 */
public class CompetitiveNode extends SubnetworkNode {

    /**
     * Create a Competitive Network PNode.
     *
     * @param networkPanel parent panel
     * @param group the competitive network
     */
    public CompetitiveNode(final NetworkPanel networkPanel, final Competitive group) {
        super(networkPanel, group);
        // setStrokePaint(Color.green);
        setInteractionBox(new CompetitiveInteractionBox(networkPanel));
        setContextMenu();
        //setOutlinePadding(15f);
//        networkPanel.getNetwork().addNetworkListener(new NetworkListener() {
//
//            public void networkChanged() {
//                group.setLabel("SOM - Learning rate:"
//                        + Utils.round(group.getAlpha(), 2) + " N-size:"
//                        + Utils.round(group.getNeighborhoodSize(), 2));
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
    }

    /**
     * Custom interaction box for Comeptitive network node.
     */
    private class CompetitiveInteractionBox extends InteractionBox {
        public CompetitiveInteractionBox(NetworkPanel net) {
            super(net, CompetitiveNode.this);
        }

//        @Override
//        protected String getToolTipText() {
//            return "Current learning rate: "
//                    + Utils.round(((Competitive) getGroup()).getAlpha(), 2)
//                    + "  Current neighborhood size: "
//                    + Utils.round(((SOM) getGroup()).getNeighborhoodSize(), 2);
//        }

        @Override
        protected boolean hasPropertyDialog() {
            return true;
        }

        @Override
        protected JDialog getPropertyDialog() {
            return new CompetitivePropertiesDialog((Competitive) getGroup());
        }

        @Override
        protected boolean hasToolTipText() {
            return false;
        }
    };

    /**
     * Sets custom menu for competitive node.
     */
    private void setContextMenu() {
        JPopupMenu menu = super.getDefaultContextMenu();
        menu.addSeparator();
        menu.add(new JMenuItem(new AbstractAction("Randomize Weights") {
            public void actionPerformed(final ActionEvent event) {
                ((Competitive) getGroup()).randomize();
                ((Competitive) getGroup()).getParentNetwork()
                        .fireNetworkChanged();
            }
        }));
//        menu.add(new JMenuItem(new AbstractAction("Train SOM Network...") {
//            // TODO: Integrate below in to training framework?
//            public void actionPerformed(final ActionEvent event) {
//                JDialog propertyDialog = new SOMTrainingDialog((SOM) getGroup());
//                propertyDialog.pack();
//                propertyDialog.setLocationRelativeTo(null);
//                propertyDialog.setVisible(true);
//                ((SOM) getGroup()).getParentNetwork().fireNetworkChanged();
//            }
//        }));

        setContextMenu(menu);
    }

}
