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

package org.simbrain.network.gui.nodes.subnetworks;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.network.ActorCriticPropertiesDialog;
import org.simbrain.network.gui.nodes.CustomOutline;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.gui.nodes.SubnetworkNode;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.networks.actorcritic.ActorCritic;

import edu.umd.cs.piccolo.PNode;

/**
 * <b>ActorCriticNetworkNode</b> is the graphical representation of a actorcritic network.
 */
public class ActorCriticNetworkNode extends SubnetworkNode {

    /** Randomize network action. */
    private Action randomizeAction;

    /** Train network action. */
    //private Action trainAction;

    /** Dash style. */
    private static final float[] DASH = {3.0f};

    /** Dash Stroke. */
    private static final BasicStroke DASHED = new BasicStroke(.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 2.0f, DASH, 0.0f);

    /** Layer outline inset. */
    private static final Color LAYER_COLOR = Color.GRAY;

    /** Outline for state layer. */
    private CustomOutline stateOutline = new CustomOutline();

    /** Outline for actor layer. */
    private CustomOutline actorOutline = new CustomOutline();

    /** Outline for critic layer. */
    private CustomOutline criticOutline = new CustomOutline();

    /**
     * Create a new ActorCriticNetworkNode.
     *
     * @param networkPanel reference to network panel
     * @param subnetwork reference to subnetwork
     * @param x initial x position
     * @param y initial y position
     */
    public ActorCriticNetworkNode(final NetworkPanel networkPanel,
                                     final ActorCritic subnetwork,
                                     final double x,
                                     final double y) {

        super(networkPanel, subnetwork, x, y);

        stateOutline.setStroke(DASHED);
        stateOutline.setStrokePaint(LAYER_COLOR);
        actorOutline.setStroke(DASHED);
        actorOutline.setStrokePaint(LAYER_COLOR);
        criticOutline.setStroke(DASHED);
        criticOutline.setStrokePaint(LAYER_COLOR);

        addChild(stateOutline);
        addChild(actorOutline);
        addChild(criticOutline);

        randomizeAction = new AbstractAction("Randomize Actor Critic Network") {
            public void actionPerformed(final ActionEvent event) {
        	subnetwork.randomize();
                subnetwork.getRootNetwork().fireNetworkChanged();
            }
        };

    }
    
    /**
     * Set references to layers.
     */
    public void init() {

        ArrayList<PNode> stateNodes = new ArrayList<PNode>();
        ArrayList<PNode> actorNodes = new ArrayList<PNode>();
        ArrayList<PNode> criticNodes = new ArrayList<PNode>();

        ActorCritic subnetwork = (ActorCritic) this.getSubnetwork();
        for (Iterator i = this.getChildrenIterator(); i.hasNext(); ) {
            PNode node = (PNode) i.next();
            if (node instanceof NeuronNode) {
                Neuron neuron = ((NeuronNode) node).getNeuron();
                if (subnetwork.getNetworkList().get(0).getNeuronList().contains(neuron)) {
                    stateNodes.add((NeuronNode) node);
                } else if (subnetwork.getNetworkList().get(1).getNeuronList().contains(neuron)) {
                    actorNodes.add((NeuronNode) node);
                } else if (subnetwork.getNetworkList().get(2).getNeuronList().contains(neuron)) {
                    criticNodes.add((NeuronNode) node);
                }
            }
        }
        stateOutline.setOutlinedObjects(stateNodes);
        actorOutline.setOutlinedObjects(actorNodes);
        criticOutline.setOutlinedObjects(criticNodes);
    }
    
    /** @see SubnetworkNode. */
    protected void updateOutlineBoundsAndPath() {
        super.updateOutlineBoundsAndPath();

        stateOutline.updateBounds();
        actorOutline.updateBounds();
        criticOutline.updateBounds();
    }
    
    @Override
    protected JDialog getPropertyDialog() {
	return new ActorCriticPropertiesDialog(getActorCriticSubnetwork());
    }
    
    /** @see org.simbrain.network.gui.nodes.ScreenElement */
    public ActorCritic getActorCriticSubnetwork() {
        return ((ActorCritic) getSubnetwork());
    }

    @Override
    protected String getToolTipText() {
	return "Actor Critic Network";
    }

    @Override
    protected boolean hasContextMenu() {
	return true;
    }
    
    /** @see org.simbrain.network.gui.nodes.ScreenElement */
    protected JPopupMenu getContextMenu() {
        JPopupMenu contextMenu = super.getContextMenu();
        contextMenu.add(randomizeAction);
        //contextMenu.addSeparator();
        //contextMenu.add(trainAction);
        contextMenu.addSeparator();
        contextMenu.add(super.getSetPropertiesAction());
        return contextMenu;

    }    

    @Override
    protected boolean hasPropertyDialog() {
	return true;
    }

    @Override
    protected boolean hasToolTipText() {
	return true;
    }
    
    /** @see PNode */
    public PNode removeChild(final PNode child) {
        PNode ret = super.removeChild(child);

        if (stateOutline.getOutlinedObjects().contains(child)) {
            stateOutline.removeOutlinedObject(child);
        } else if (actorOutline.getOutlinedObjects().contains(child)) {
            actorOutline.removeOutlinedObject(child);
        }
        
        updateOutlineBoundsAndPath();
        return ret;
    }    

}
