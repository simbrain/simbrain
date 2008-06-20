/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005-2006 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.network.gui.nodes;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.interfaces.Group;

import edu.umd.cs.piccolo.PNode;

/**
 * Represents a group of neurons and weights that are part of other networks.
 */
public class ModelGroupNode extends CustomOutline implements PropertyChangeListener {

    /** Outline inset or border height. */
    public static final double OUTLINE_INSET_HEIGHT = 4d;

    /** Outline inset or border width. */
    public static final double OUTLINE_INSET_WIDTH = 4d;

    /** Dash style. */
    private static final float[] DASH = {10.0f};

    /** Dash style. */
    private static final BasicStroke DASHED = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, DASH, 0.0f);

    /** The model group. */
    private final Group group;

    /** Turn this group on or off. */
    private Action groupOnOff;

    /** Action which removes this group. */
    private Action removeGroup;

    /**
     * Create a new abstract subnetwork node from the specified parameters.
     *
     * @param networkPanel networkPanel for this subnetwork node, must not be null.
     * @param subnetwork subnetwork for this subnetwork node, must not be null.
     */
    public ModelGroupNode(final NetworkPanel networkPanel, final Group group) {
        super(networkPanel);
        this.setHasInteractionBox(true);
        this.setNodePositioning(CustomOutline.NodePositioning.GLOBAL);
        this.group = group;
        setStroke(DASHED);
        setStrokePaint(Color.yellow);
    }


    /**
     * Creates default actions for all model group nodes.
     * @return context menu populated with default actions.
     */
    protected JPopupMenu getContextMenu() {
        JPopupMenu ret = new JPopupMenu();
        groupOnOff = new AbstractAction("Group is active") {
            public void actionPerformed(final ActionEvent event) {
                group.toggleOnOff();
            }
        };
        removeGroup = new AbstractAction("Remove group") {
            public void actionPerformed(final ActionEvent event) {
                getNetworkPanel().getRootNetwork().deleteGroup(group);
            }
        };
        JCheckBoxMenuItem groupOnOffItem = new JCheckBoxMenuItem(groupOnOff);
        ret.add(groupOnOffItem);
        ret.add(removeGroup);
        return ret;
    }

    /**
     * Add a node for reference.
     *
     * @param node node to add.
     */
    public void addReference(final PNode node) {
        node.addPropertyChangeListener(this);
        node.getParent().addPropertyChangeListener(this);
        addOutlinedObject(node);
    }

    /**
     * Remopve a reference node.
     *
     * @param node node to remove.
     */
    public void removeReference(final PNode node) {
        removeOutlinedObject(node);
    }

    /** @see ScreenElement */
    public final boolean showSelectionHandle() {
        return false;
    }

    /** @see ScreenElement */
    public final boolean isDraggable() {
        return false;
    }

    /** @see ScreenElement */
    public final void resetColors() {
        // empty
    }

    /**
     * @return the group
     */
    public Group getGroup() {
        return group;
    }

}