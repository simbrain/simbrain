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
import javax.swing.Action;
import javax.swing.JPopupMenu;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.network.SOMTrainingDialog;
import org.simbrain.network.subnetworks.SOMNetwork;
import org.simbrain.util.StandardDialog;

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
     * @param group the SOM network
     */
    public SOMNetworkNode(NetworkPanel networkPanel, SOMNetwork group) {
        super(networkPanel, group);
        setContextMenu();
    }

    @Override
    protected StandardDialog getPropertyDialog() {
        return new SOMTrainingDialog(getNetworkPanel(), (SOMNetwork) getGroup());
    }


    /**
     * Sets custom menu for SOM Network node.
     */
    private void setContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(editGroup);
        menu.add(renameGroup);
        menu.add(removeGroup);
        menu.addSeparator();
        Action trainNet = new AbstractAction("Train on current pattern") {
            public void actionPerformed(final ActionEvent event) {
                ((SOMNetwork) getGroup()).update();
                ((SOMNetwork) getGroup()).getParentNetwork()
                        .fireNetworkChanged();
            }
        };
        menu.add(trainNet);
        Action randomizeNet = new AbstractAction(
                "Randomize synapses") {
            public void actionPerformed(final ActionEvent event) {
                ((SOMNetwork) getGroup()).getSom().randomizeIncomingWeights();
                ((SOMNetwork) getGroup()).getParentNetwork()
                        .fireNetworkChanged();
            }
        };
        menu.add(randomizeNet);
        setContextMenu(menu);
    }

}
