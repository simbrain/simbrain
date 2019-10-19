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

import org.piccolo2d.PCamera;
import org.piccolo2d.nodes.PText;
import org.piccolo2d.util.PBounds;
import org.simbrain.network.gui.NetworkPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Interaction Box: graphical element for interacting with a group.
 */
public abstract class InteractionBox extends ScreenElement {

    /**
     * Width of interaction box.
     */
    private final static float DEFAULT_WIDTH = 20;

    /**
     * Height of interaction box.
     */
    private final static float DEFAULT_HEIGHT = 10;

    /**
     * Text label.
     */
    private PText textLabel;

    /**
     * Context menu.
     */
    private JPopupMenu contextMenu;

    /**
     * This is the largest amount an interaction box's scale can be zoomed when
     * the scale gets small. Easiest to understand by changing the value and
     * "zooming  out" of a network containing a neuron group.
     */
    private final double largestZoomRescaleFactor = 4;

    /**
     * Reference to property change listener so it can be cleaned up later.
     */
    private final PropertyChangeListener zoomListener;

    Rectangle2D rect = new Rectangle2D.Float(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT);

    /**
     * Create a new tab node.
     *
     * @param net
     */
    public InteractionBox(final NetworkPanel net) {
        super(net);

        this.append(rect, false);
        Color color = new Color(248, 252, 184);
        setPaint(color);
        // setTransparency(.2f);
        setStrokePaint(java.awt.Color.GRAY);
        textLabel = new PText();
        addChild(textLabel);

        // Add listener to camera which causes the interaction box
        // to re-scale when the canvas is "zoomed out" (view scale below 1)
        zoomListener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                double viewScale = net.getCanvas().getCamera().getViewScale();
                if (viewScale < 1) {
                    // Rescale based on linear equation so that as view scale
                    // goes from 1 to 0 rescaleAmount goes from 1 to "largestZoomRescaleFactor"
                    double rescaleAmount = (1 - largestZoomRescaleFactor) * viewScale + largestZoomRescaleFactor;
                    setScale(rescaleAmount);
                } else {
                    setScale(1);
                }
            }
        };
        net.getCanvas().getCamera().addPropertyChangeListener(PCamera.PROPERTY_VIEW_TRANSFORM, zoomListener);

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
            textLabel.scaleAboutPoint(.8, getBounds().getCenter2D().getX(), getBounds().getCenter2D().getY());
        }
        textLabel.setText(text);
        textLabel.resetBounds();
        updateText();
    }

    /**
     * Update the text label bounds.
     */
    public void updateText() {
        // TODO: Appears to be one source of performance drains.
        // This is called multiple times at each iteration and it calls repaint
        // Reduce number of times this is called
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Reset box bounds
                textLabel.centerFullBoundsOnPoint(getBounds().getCenter2D().getX(), getBounds().getCenter2D().getY());
                setBounds(textLabel.getBounds());
                getNetworkPanel().repaint();
            }
        });
    }

    @Override
    protected void singleClickEvent() {
        //groupNode.selectAllNodes();
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
        return !getNetworkPanel().isRunning();
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
    public boolean showNodeHandle() {
        return true;
    }

    /**
     * @param contextMenu the contextMenu to set
     */
    public void setContextMenu(final JPopupMenu contextMenu) {
        this.contextMenu = contextMenu;
    }


    /**
     * @return the zoomListener
     */
    public PropertyChangeListener getZoomListener() {
        return zoomListener;
    }

}