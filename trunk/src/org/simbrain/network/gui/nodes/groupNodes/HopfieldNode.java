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
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.simbrain.network.subnetworks.Hopfield;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.network.HopfieldPropertiesDialog;
import org.simbrain.network.gui.nodes.InteractionBox;

/**
 * PNode representation of Hopfield Network.
 * 
 * TODO:
 *   - Location of top interaction box
 *   - Dialog
 * 
 * @author jyoshimi
 */
public class HopfieldNode extends SubnetworkNode {
    
    /**
     * Create a Hopfield Network PNode.
     *
     * @param networkPanel parent panel
     * @param group the Hopfield network
     */
    public HopfieldNode(NetworkPanel networkPanel, Hopfield group) {
        super(networkPanel, group);
        //setStrokePaint(Color.green);
        setInteractionBox(new HopfieldInteractionBox(networkPanel));
        setContextMenu();
        setOutlinePadding(15f);
    }
    
    /**
     * Custom interaction box for Hopfield group node.
     */
    private class HopfieldInteractionBox extends InteractionBox {
        public HopfieldInteractionBox(NetworkPanel net) {
            super(net, HopfieldNode.this);
        }

        @Override
        protected JDialog getPropertyDialog() {
            return new HopfieldPropertiesDialog((Hopfield) getGroup());
        }

        @Override
        protected String getToolTipText() {
            return "Hopfield network.";
        }

        @Override
        protected boolean hasPropertyDialog() {
            return true;
        }

        @Override
        protected boolean hasToolTipText() {
            return true;
        }
    };

    /**
     * Sets custom menu for Hopfield node.
     */
    private void setContextMenu() {
        JPopupMenu menu = super.getDefaultContextMenu();
        menu.addSeparator();
        Action trainNet = new AbstractAction("Train Hopfield Network...") {
            public void actionPerformed(final ActionEvent event) {
                ((Hopfield) getGroup()).train();
            }
        };
        menu.add(new JMenuItem(trainNet));
        Action randWeights = new AbstractAction("Randomize Weights...") {
            public void actionPerformed(final ActionEvent event) {
                ((Hopfield) getGroup()).randomizeWeights();
            }
        };
        menu.add(new JMenuItem(randWeights));
        setContextMenu(menu);
    }
    

}
