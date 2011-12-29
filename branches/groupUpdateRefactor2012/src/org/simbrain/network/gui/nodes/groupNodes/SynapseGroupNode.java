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

import java.awt.Color;

import javax.swing.JPopupMenu;

import org.simbrain.network.groups.SubnetworkGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.nodes.GroupNode;
import org.simbrain.network.gui.nodes.InteractionBox;
import org.simbrain.network.gui.nodes.SynapseNode;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.util.PBounds;

/**
 * PNode representation of a group of synapses.
 * 
 * @author jyoshimi
 */
public class SynapseGroupNode extends GroupNode {
    
    /**
     * Create a Synapse Group PNode.
     *
     * @param networkPanel parent panel
     * @param group the synapse group
     */
    public SynapseGroupNode(NetworkPanel networkPanel, SynapseGroup group) {
        super(networkPanel, group);
        setStrokePaint(Color.gray);
        setPaint(Color.gray);
        setTransparency(.2f);
        setInteractionBox(new SynapseInteractionBox(networkPanel));
        setContextMenu();
        setOutlinePadding(-5);
        setPickable(false);
        updateVisibility();
    }
    
    /**
     * Custom interaction box for Synapse group node.
     */
    private class SynapseInteractionBox extends InteractionBox {
        public SynapseInteractionBox(NetworkPanel net) {
            super(net, SynapseGroupNode.this);
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
            PBounds childBounds = node.getGlobalBounds();
            bounds.add(childBounds);
            if (node instanceof SynapseNode) {
                bounds.add(((SynapseNode) node).getLine().getBounds());
            }
        }

        double inset = getOutlinePadding();
        bounds.setRect(bounds.getX() - inset, bounds.getY() - inset,
                bounds.getWidth() + (2 * inset), bounds.getHeight()
                        + (2 * inset));

        setPathToRectangle((float) bounds.getX(), (float) bounds.getY(),
                (float) bounds.getWidth(), (float) bounds.getHeight());

        updateInteractionBox();
        moveToBack();
    }

    @Override
    protected void updateInteractionBox() {
        InteractionBox interactionBox = getInteractionBox();
        interactionBox.setOffset(
                getBounds().getX() - interactionBox.getFullBounds().width,
                getBounds().getCenterY() - interactionBox.getHeight() / 2);
    }
    
    @Override
    public void removePNode(PNode node) {
        super.removePNode(node);
        updateVisibility();
    }

    @Override
    public void addPNode(PNode node) {
        super.addPNode(node);
        updateVisibility();
    }
    
    /**
     * If there are no synpase nodes to display, make this whole
     * synapsegroupnode invisible.
     */
    private void updateVisibility() {
        if (getOutlinedObjects().isEmpty()) {
            setVisible(false);
        } else {
            setVisible(true);
        }
        
    }

}
