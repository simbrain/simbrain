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
package org.simbrain.network.nodes;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.simbrain.network.NetworkPanel;
import org.simnet.interfaces.Network;
import org.simnet.interfaces.Group;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;
import edu.umd.cs.piccolo.util.PBounds;

/**
 * Represents a group of neurons and weights that are part of other networks.
 */
public class ModelGroupNode extends ScreenElement implements PropertyChangeListener {

    /** Outline inset or border height. */
    public static final double OUTLINE_INSET_HEIGHT = 4d;

    /** Outline inset or border width. */
    public static final double OUTLINE_INSET_WIDTH = 4d;

    /** Default outline height. */
    private static final double DEFAULT_OUTLINE_HEIGHT = 0.0d;

    /** Default outline width. */
    private static final double DEFAULT_OUTLINE_WIDTH = 0.0d;

    /** Dash style. */
    private static final float[] DASH = {10.0f};

    /** Dash style. */
    private static final BasicStroke DASHED = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, DASH, 0.0f);

    /** The model group. */
    private final Group group;

    /** Outline node. */
    private final OutlineNode outline;

    /** Set properties action. */
    private Action setPropertiesAction;

    /** References to nodes in the group. */
    private ArrayList<PNode> referenceList = new ArrayList<PNode>();


    /**
     * Create a new abstract subnetwork node from the specified parameters.
     *
     * @param networkPanel networkPanel for this subnetwork node, must not be null.
     * @param subnetwork subnetwork for this subnetwork node, must not be null.
     */
    public ModelGroupNode(final NetworkPanel networkPanel,
                             final Group group) {

        super(networkPanel);

        this.group = group;
        outline = new OutlineNode();
        addChild(outline);
        outline.setStroke(DASHED);
        outline.setStrokePaint(Color.yellow);

    }

    /**
     * Add a node for reference.
     *
     * @param node node to add.
     */
    public void addReference(final PNode node) {
        node.addPropertyChangeListener(this);
        node.getParent().addPropertyChangeListener(this);
        referenceList.add(node);
    }


    /**
     * Remopve a reference node.
     *
     * @param node node to remove.
     */
    public void removeReference(final PNode node) {
        referenceList.remove(node);
    }

    /** @see PropertyChangeListener */
    public void propertyChange(final PropertyChangeEvent event) {
        updateOutlineBoundsAndPath();
    }

    /**
     * Update outline bounds and path.
     */
    public void updateOutlineBoundsAndPath() {

        // one of the child nodes' full bounds changed
        PBounds bounds = new PBounds();
        for (PNode node : referenceList) {
            if ((node instanceof NeuronNode) || (node instanceof SynapseNode)) {
                PBounds childBounds = node.getGlobalBounds();
                bounds.add(childBounds);
            }
        }

        // add border
        bounds.setRect(bounds.getX() - OUTLINE_INSET_WIDTH,
                       bounds.getY() - OUTLINE_INSET_HEIGHT,
                       bounds.getWidth() + (2 * OUTLINE_INSET_WIDTH),
                       bounds.getHeight() + (2 * OUTLINE_INSET_HEIGHT));

        // set outline to new bounds
        // TODO:  only update rect if it needs updating
        outline.setBounds(bounds);
        outline.setPathToRectangle((float) bounds.getX(), (float) bounds.getY(),
                                   (float) bounds.getWidth(), (float) bounds.getHeight());

    }

    /**
     * @return Returns the setPropertiesAction.
     */
    public Action getSetPropertiesAction() {
        return setPropertiesAction;
    }

    /**
     * @param setPropertiesAction The setPropertiesAction to set.
     */
    public void setSetPropertiesAction(final Action setPropertiesAction) {
        this.setPropertiesAction = setPropertiesAction;
    }

    /**
     * Outline node.
     */
    private class OutlineNode extends PPath {

        /**
         * Outline node.
         */
        public OutlineNode() {
            super();

            setPickable(false);
            setChildrenPickable(false);

            setBounds(0.0d, 0.0d, DEFAULT_OUTLINE_WIDTH, DEFAULT_OUTLINE_HEIGHT);
            setPathToRectangle(0.0f, 0.0f, (float) DEFAULT_OUTLINE_WIDTH, (float) DEFAULT_OUTLINE_HEIGHT);
        }


        /**
         * Set the outline stroke for this outline node to <code>outlineStroke</code>.
         *
         * @param outlineStroke outline stroke for this outline node
         */
        public final void setOutlineStroke(final Stroke outlineStroke) {
            setStroke(outlineStroke);
        }

        /**
         * Set the outline stroke paint for this outline node to <code>outlineStrokePaint</code>.
         *
         * @param outlineStrokePaint outline stroke paint for this outline node
         */
        public final void setOutlineStrokePaint(final Paint outlineStrokePaint) {
            setStrokePaint(outlineStrokePaint);
        }
    }


    @Override
    protected boolean hasContextMenu() {
        return false;
    }

    @Override
    protected boolean hasPropertyDialog() {
        return false;
    }

    @Override
    protected boolean hasToolTipText() {
        return false;
    }

    @Override
    protected JDialog getPropertyDialog() {
        return null;
    }

    @Override
    protected String getToolTipText() {
        return null;
    }

    @Override
    protected JPopupMenu getContextMenu() {
        return null;
    }

    /** @see ScreenElement */
    public final boolean isSelectable() {
        return false;
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

}