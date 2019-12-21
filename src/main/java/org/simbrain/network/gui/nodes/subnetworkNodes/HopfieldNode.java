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
import org.simbrain.network.gui.nodes.SubnetworkNode;
import org.simbrain.network.subnetworks.Hopfield;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * PNode representation of Hopfield Network.
 *
 * @author jyoshimi
 */
public class HopfieldNode extends SubnetworkNode {

    /**
     * Create a Hopfield Network PNode.
     *
     * @param networkPanel parent panel
     * @param group        the Hopfield network
     */
    public HopfieldNode(NetworkPanel networkPanel, Hopfield group) {
        super(networkPanel, group);
        // setStrokePaint(Color.green);
        setContextMenu();
        // setOutlinePadding(15f);
    }

    @Override
    protected StandardDialog getPropertyDialog() {
        return AnnotatedPropertyEditor.getDialog(getSubnetwork());
    }

    /**
     * Sets custom menu for Hopfield node.
     */
    private void setContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        editAction.putValue("Name", "Edit / Train Hopfield...");
        menu.add(editAction);
        menu.add(renameAction);
        menu.add(removeAction);
        menu.addSeparator();
        menu.add(addInputRowAction);
        Action trainNet = new AbstractAction("Train on current pattern") {
            public void actionPerformed(final ActionEvent event) {
                ((Hopfield) getSubnetwork()).trainOnCurrentPattern();
            }
        };
        menu.add(trainNet);
        menu.addSeparator();
        Action randomizeNet = new AbstractAction("Randomize synapses symmetrically") {
            public void actionPerformed(final ActionEvent event) {
                ((Hopfield) getSubnetwork()).randomize();
            }
        };
        menu.add(randomizeNet);
        Action clearWeights = new AbstractAction("Set weights to zero") {
            public void actionPerformed(final ActionEvent event) {
                getSubnetwork().getSynapseGroup().setStrength(0, Polarity.BOTH);
            }
        };
        menu.add(clearWeights);
        setContextMenu(menu);
    }

}
