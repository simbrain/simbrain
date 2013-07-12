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

import java.awt.Color;
import java.awt.Paint;

import javax.swing.JDialog;
import javax.swing.JPopupMenu;

import org.simbrain.network.gui.NetworkPanel;

import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;

/**
 * Interaction Box: graphical element for interacting with a group.
 */
public class InteractionBox extends ScreenElement {

    /** Width of interaction box. */
    private final static float DEFAULT_WIDTH = 20;

    /** Height of interaction box. */
    private final static float DEFAULT_HEIGHT = 10;

    /** Main circle of node. */
    private PPath box;

    /** Text label. */
    private PText textLabel;

    /** Context menu. */
    private JPopupMenu contextMenu;

    /** Reference to associated groupNode. */
    private GroupNode groupNode;

    /**
     * Create a new tab node.
     */
    public InteractionBox(final NetworkPanel net, final GroupNode node) {
        super(net);
        groupNode = node;
        box = PPath.createRectangle(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        Color color = new Color(248, 252, 184);
        box.setPaint(color);
        // box.setTransparency(.2f);
        box.setStrokePaint(java.awt.Color.GRAY);
        setBounds(box.getBounds());
        addChild(box);
        textLabel = new PText();
        box.addChild(textLabel);
    }

    /**
     * Set text for interaction box.
     *
     * @param text the textLabel to set
     */
    public void setText(String text) {
        if (text == null) {
            return;
        }
        if (textLabel.getScale() == 1) {
            textLabel.scaleAboutPoint(.8, box.getBounds().getCenter2D().getX(),
                    box.getBounds().getCenter2D().getY());
        }
        textLabel.setText(text);
        textLabel.resetBounds();
        updateText();
    }

    /**
     * Update the text label bounds.
     */
    public void updateText() {
        // Reset box bounds
        textLabel.centerFullBoundsOnPoint(box.getBounds().getCenter2D().getX(),
                box.getBounds().getCenter2D().getY());
        box.setBounds(textLabel.getBounds());
        setBounds(box.getBounds());
        getNetworkPanel().repaint();
    }

    @Override
    protected void singleClickEvent() {
        groupNode.selectAllNodes();
    }

    @Override
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
        return true;
    }

    @Override
    public void resetColors() {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean showSelectionHandle() {
        return true;
    }

    /**
     * @param contextMenu the contextMenu to set
     */
    public void setContextMenu(final JPopupMenu contextMenu) {
        this.contextMenu = contextMenu;
    }

    @Override
    public void setPaint(Paint arg0) {
        super.setPaint(arg0);
        box.setPaint(arg0);
    }

}