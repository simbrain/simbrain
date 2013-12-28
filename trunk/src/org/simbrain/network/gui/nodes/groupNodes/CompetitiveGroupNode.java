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
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.subnetworks.CompetitiveGroup;

/**
 * PNode representation of Competitive network.
 *
 * @author jyoshimi
 */
public class CompetitiveGroupNode extends NeuronGroupNode {

    /**
     * Create a Competitive Network PNode.
     *
     * @param networkPanel parent panel
     * @param group the competitive network
     */
    public CompetitiveGroupNode(final NetworkPanel networkPanel,
            final CompetitiveGroup group) {
        super(networkPanel, group);
        setCustomMenu();

        // setOutlinePadding(15f);
        // networkPanel.getNetwork().addNetworkListener(new NetworkListener() {
        //
        // public void networkChanged() {
        // group.setLabel("SOM - Learning rate:"
        // + Utils.round(group.getAlpha(), 2) + " N-size:"
        // + Utils.round(group.getNeighborhoodSize(), 2));
        // }
        //
        // public void networkUpdateMethodChanged() {
        // }
        //
        // public void neuronClampToggled() {
        // }
        //
        // public void synapseClampToggled() {
        // }
        //
        // });
    }

    /**
     * Sets the custom menu for competitive networks.
     */
    private void setCustomMenu() {
        JMenu customMenu = new JMenu("Competitive Net");
        customMenu.add(new JMenuItem(new AbstractAction("Randomize Weights") {
            public void actionPerformed(final ActionEvent event) {
                ((CompetitiveGroup) getGroup()).randomize();
                ((CompetitiveGroup) getGroup()).getParentNetwork()
                        .fireNetworkChanged();
            }
        }));
        setCustomMenu(customMenu);
    }

}
