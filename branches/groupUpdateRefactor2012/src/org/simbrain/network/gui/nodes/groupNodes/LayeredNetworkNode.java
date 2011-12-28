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

import javax.swing.JPopupMenu;

import org.simbrain.network.groups.LayeredNetwork;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.GroupNode;
import org.simbrain.network.gui.nodes.InteractionBox;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.util.PBounds;

/**
 * PNode representation of a group of a layered network..
 * 
 * @author jyoshimi
 */
public class LayeredNetworkNode extends GroupNode {
    
    /**
     * Create a layered network
     *
     * @param networkPanel parent panel
     * @param group the layered network
     */
    public LayeredNetworkNode(NetworkPanel networkPanel, LayeredNetwork group) {
        super(networkPanel, group);
        setInteractionBox(new LayeredNetworkInteractionBox(networkPanel));
        setContextMenu();
        setTextLabel("Layered network");
    }
    
    /**
     * Custom interaction box for Synapse group node.
     */
    private class LayeredNetworkInteractionBox extends InteractionBox {
        public LayeredNetworkInteractionBox(NetworkPanel net) {
            super(net, LayeredNetworkNode.this);
        }

    };

    
    
    /**
     * Sets custom menu.
     */
    private void setContextMenu() {
        JPopupMenu menu = super.getDefaultContextMenu();
//        menu.addSeparator();
//        Action trainNet = new AbstractAction("Train Hopfield Network...") {
//            public void actionPerformed(final ActionEvent event) {
//                ((Hopfield) getGroup()).train();
//            }
//        };
//        menu.add(new JMenuItem(trainNet));
//        Action randWeights = new AbstractAction("Randomize Weights...") {
//            public void actionPerformed(final ActionEvent event) {
//                ((Hopfield) getGroup()).randomizeWeights();
//            }
//        };
//        menu.add(new JMenuItem(randWeights));
        setConextMenu(menu);
    }
    
    @Override
    public void updateBounds() {

        PBounds bounds = new PBounds();
        for (PNode node : getOutlinedObjects()) {
            bounds.add(node.getGlobalBounds());
        }
        
        // Add a little extra height at very top
        double inset = getOutlinePadding();
        bounds.setRect(bounds.getX() - inset,
                bounds.getY() - inset - 15,
                bounds.getWidth() + (2 * inset),
                bounds.getHeight() + (2 * inset) + 15);

        setPathToRectangle((float) bounds.getX(), (float) bounds.getY(),
                            (float) bounds.getWidth(), (float) bounds.getHeight());

        updateInteractionBox();

    }

}
