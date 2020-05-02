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
package org.simbrain.network.gui.nodes.subnetworkNodes;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.network.CompetitiveTrainingDialog;
import org.simbrain.network.gui.nodes.SubnetworkNode;
import org.simbrain.network.subnetworks.CompetitiveNetwork;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * PNode representation of competitive network.
 *
 * @author Jeff Yoshimi
 */
public class CompetitiveNetworkNode extends SubnetworkNode {

    /**
     * Create a competitive Network PNode.
     *
     * @param networkPanel parent panel
     * @param group        the competitive network
     */
    public CompetitiveNetworkNode(NetworkPanel networkPanel, CompetitiveNetwork group) {
        super(networkPanel, group);
        setContextMenu();
    }

    @Override
    public StandardDialog getPropertyDialog() {
        return new CompetitiveTrainingDialog(getNetworkPanel(),
                (CompetitiveNetwork) getSubnetwork());
    }

    /**
     * Sets custom menu for Competitive Network node.
     */
    private void setContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        editAction.putValue("Name", "Edit / Train Competitive...");
        menu.add(editAction);
        menu.add(renameAction);
        menu.add(removeAction);
        menu.addSeparator();
        menu.add(addInputRowAction);
        Action trainNet = new AbstractAction("Train on current pattern") {
            public void actionPerformed(final ActionEvent event) {
                CompetitiveNetwork net = ((CompetitiveNetwork) getSubnetwork());
                net.update();
                //TODO
                // net.getParentNetwork().fireGroupUpdated(net);
            }
        };
        menu.add(trainNet);
        menu.addSeparator();
        Action randomizeNet = new AbstractAction("Randomize synapses") {
            public void actionPerformed(final ActionEvent event) {
                CompetitiveNetwork net = ((CompetitiveNetwork) getSubnetwork());
                net.getCompetitive().randomize();
                //TODO
                // net.getParentNetwork().fireGroupUpdated(net.getSynapseGroup());
            }
        };
        menu.add(randomizeNet);
        setContextMenu(menu);
    }

}
