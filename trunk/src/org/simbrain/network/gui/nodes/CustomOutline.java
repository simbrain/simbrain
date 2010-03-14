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
package org.simbrain.network.gui.nodes;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.simbrain.network.gui.NetworkPanel;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;
import edu.umd.cs.piccolo.util.PBounds;

/**
 * A custom outline that can be drawn around a collection of nodes. Be sure,
 * after initializing, to specify whether an interaction box will be used, and
 * whether positioning will be non-standard (i.e. not in a subnetworknode).
 *
 * TODO: Serious refactoring needed in this class. Interaction box needs to be
 * more general, and needs to resize as text is changed.
 *
 * TODO: Rename to outline or tabbed outline?
 */
public class CustomOutline extends PPath implements PropertyChangeListener {

    /** References to outlined objects. */
    private ArrayList<PNode> outlinedObjects = new ArrayList<PNode>();

    /**
     * Which coordinate system to place the outlined nodes in.
     * Set to GLOBAL if the outline is directly on a canvas.
     * Set to LOCAL if the outline is contained in a subnetwork node.
     */
    public enum NodePositioning { GLOBAL, PARENT }

    /** By default use parent positioning. */
    private NodePositioning nodePositioning = NodePositioning.PARENT;

    /** Inset between contained nodes and the outline itself. */
    private double inset = 2.0d;

    /** Interaction box. */
    private InteractionBox interactionBox;

    /** Whether this has an interaction box or not. */
    private boolean hasInteractionBox = false;

    /** Network panel. */
    private NetworkPanel networkPanel;

    /**
     * If no interaction box is used, no reference to a network panel is needed.
     */
    public CustomOutline() {
    }

    /**
     * @param net reference to parent network panel.
     */
    public CustomOutline(final NetworkPanel net) {
        networkPanel = net;
    }

    /** @see PPath. */
    public void propertyChange(final PropertyChangeEvent arg0) {
        updateBounds();
    }

    /**
     * Updated bounds of outline based on location of its outlined objects.
     */
    public void updateBounds() {
        PBounds inputBounds = new PBounds();

        for (PNode node : outlinedObjects) {
            switch (nodePositioning) {
            case GLOBAL:
                PBounds childBounds = node.getGlobalBounds();
                inputBounds.add(childBounds);
                break;
            case PARENT:
                childBounds = node.getBounds();
                node.localToParent(childBounds);
                inputBounds.add(childBounds);
                break;
            default:
                break;
            }
        }

        inputBounds.setRect(inputBounds.getX() - inset,
                inputBounds.getY() - inset,
                inputBounds.getWidth() + (2 * inset),
                inputBounds.getHeight() + (2 * inset));

        setPathToRectangle((float) inputBounds.getX(), (float) inputBounds.getY(),
                            (float) inputBounds.getWidth(), (float) inputBounds.getHeight());

        updateInteractionBox();

    }

    /**
     * @return the hasInteractionBox
     */
    public boolean isHasInteractionBox() {
        return hasInteractionBox;
    }

    /**
     * @param hasInteractionBox the hasInteractionBox to set
     */
    public void setHasInteractionBox(final boolean hasInteractionBox) {
        this.hasInteractionBox = hasInteractionBox;
        if (hasInteractionBox) {
            interactionBox = new InteractionBox(networkPanel);
            this.addChild(interactionBox);
        }
    }

    /**
     * @return the nodePositioning
     */
    public NodePositioning getNodePositioning() {
        return nodePositioning;
    }

    /**
     * @param nodePositioning the nodePositioning to set
     */
    public void setNodePositioning(final NodePositioning nodePositioning) {
        this.nodePositioning = nodePositioning;
    }

    /**
     * Update location of interaciton box.
     */
    private void updateInteractionBox() {
        if (hasInteractionBox) {
            interactionBox.setOffset(this.getBounds().getX()
                    - interactionBox.getOFFSET_X(), this.getBounds().getY()
                    - interactionBox.getOFFSET_Y());
        }
    }

    /**
     * @return the outlinedObjects
     */
    public ArrayList<PNode> getOutlinedObjects() {
        return outlinedObjects;
    }

    /**
     * @param outlinedObjects the outlinedObjects to set
     */
    public void setOutlinedObjects(final ArrayList<PNode> outlinedObjects) {
        this.outlinedObjects = outlinedObjects;
    }

    /**
     * Add an outlined object.
     *
     * @param node the object to add.
     */
    public void addOutlinedObject(final PNode node) {
        outlinedObjects.add(node);
    }

    /**
     * Remove one of the outlined aboves.
     *
     * @param node objet to remove.
     */
    public void removeOutlinedObject(final PNode node) {
        outlinedObjects.remove(node);
        if (outlinedObjects.isEmpty()) {
            this.removeFromParent();
        }
    }

    /**
     * Set the context menu on the interaction box.
     *
     * @param menu the new menu.
     */
    public void setConextMenu(final JPopupMenu menu) {
        interactionBox.setContextMenu(menu);
    }

    /**
     * Set a text label on the interaction box.
     *
     * @param text the text to set.
     */
    public void setTextLabel(final String text) {
        interactionBox.setText(text);
    }

    /**
     * Interaction Box: graphical element for interacting with a group.
     */
    private class InteractionBox extends ScreenElement {

        /** Width of interaction box. */
        private float BOX_WIDTH = 20;

        /** Height of interaction box. */
        private float BOX_HEIGHT = 10;

        /** Distance from upper left corner of group. */
        private float OFFSET_X = 0;

        /** Distance from upper left corner of group. */
        private float OFFSET_Y = BOX_HEIGHT + 3;

        /** Main circle of node. */
        private PPath box;

        /** Text label. */
        private PText textLabel;

        /** Context menu. */
        private JPopupMenu contextMenu;

        /**
         * Create a new tab node.
         */
        public InteractionBox(final NetworkPanel net) {
            super(net);
            box = PPath.createRectangle(0, 0, BOX_WIDTH, BOX_HEIGHT);
            box.setPaint(java.awt.Color.LIGHT_GRAY);
            box.setTransparency(.5f);
            box.setStrokePaint(java.awt.Color.GRAY);
            setBounds(box.getBounds());
            addChild(box);
        }

        /**
         * Set text for interaction box.
         *
         * @param textLabel the textLabel to set
         */
        public void setText(String text) {
            if (text == null) {
                return;
            }
            if (this.textLabel == null) {
                this.textLabel = new PText(text);
                this.addChild(textLabel);
                textLabel.scaleAboutPoint(.8, box.getBounds().getCenter2D()
                        .getX(), box.getBounds().getCenter2D().getY());
            } else {
                textLabel.setText(text);
                textLabel.resetBounds();
            }

            // Reset box bounds
            box.setBounds(textLabel.getBounds()); 
        }

        /** @see ScreenElement */
        protected JPopupMenu getContextMenu() {
            return contextMenu;
        }

        @Override
        protected JDialog getPropertyDialog() {
            return null;
        }

        @Override
        protected String getToolTipText() {
            return "";
        }

        @Override
        protected boolean hasContextMenu() {
            return true;
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
        public boolean isDraggable() {
            return false;
        }

        @Override
        public boolean isSelectable() {
            return false;
        }

        @Override
        public void resetColors() {
            // TODO Auto-generated method stub
        }

        @Override
        public boolean showSelectionHandle() {
            return false;
        }


        /**
         * @return the x offset
         */
        public float getOFFSET_X() {
            return OFFSET_X;
        }


        /**
         * @param offset_x the offset to set
         */
        public void setOFFSET_X(float offset_x) {
            OFFSET_X = offset_x;
        }


        /**
         * @return the oFFSET_Y
         */
        public float getOFFSET_Y() {
            return OFFSET_Y;
        }


        /**
         * @param offset_y the oFFSET_Y to set
         */
        public void setOFFSET_Y(final float offset_y) {
            OFFSET_Y = offset_y;
        }



        /**
         * @param contextMenu the contextMenu to set
         */
        public void setContextMenu(final JPopupMenu contextMenu) {
            this.contextMenu = contextMenu;
        }

    }
    /**
     * @return the networkPanel
     */
    public NetworkPanel getNetworkPanel() {
        return networkPanel;
    }

    /**
     * @param networkPanel the networkPanel to set
     */
    public void setNetworkPanel(NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
    }

}
