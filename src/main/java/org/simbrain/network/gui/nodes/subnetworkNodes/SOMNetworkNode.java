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
import org.simbrain.network.gui.dialogs.network.SOMTrainingDialog;
import org.simbrain.network.gui.nodes.SubnetworkNode;
import org.simbrain.network.subnetworks.SOMNetwork;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * PNode representation of SOM Network.
 *
 * @author jyoshimi
 */
public class SOMNetworkNode extends SubnetworkNode {

    /**
     * Create a SOM Network PNode.
     *
     * @param networkPanel parent panel
     * @param group        the SOM network
     */
    public SOMNetworkNode(NetworkPanel networkPanel, SOMNetwork group) {
        super(networkPanel, group);
        setContextMenu();
    }

    @Override
    public StandardDialog getPropertyDialog() {
        return
                new SOMTrainingDialog(getNetworkPanel(),
                        (SOMNetwork) getSubnetwork());
    }

    /**
     * Sets custom menu for SOM Network node.
     */
    private void setContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        editAction.putValue("Name", "Edit / Train SOM...");
        menu.add(editAction);
        menu.add(renameAction);
        menu.add(removeAction);
        menu.addSeparator();
        menu.add(addInputRowAction);
        Action trainNet = new AbstractAction("Train on current pattern") {
            public void actionPerformed(final ActionEvent event) {
                SOMNetwork net = ((SOMNetwork) getSubnetwork());
                net.update();
                //TODO
                // net.getParentNetwork().fireGroupUpdated(net);
            }
        };
        menu.add(trainNet);
        menu.addSeparator();
        Action randomizeNet = new AbstractAction("Randomize synapses") {
            public void actionPerformed(final ActionEvent event) {
                SOMNetwork net = ((SOMNetwork) getSubnetwork());
                net.getSom().randomizeIncomingWeights();
                //TODO
                // net.getParentNetwork().fireGroupUpdated(net);
            }
        };
        menu.add(randomizeNet);
        setContextMenu(menu);
    }

}
