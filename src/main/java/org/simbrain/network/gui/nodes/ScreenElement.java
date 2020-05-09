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

import org.jetbrains.annotations.Nullable;
import org.piccolo2d.PNode;
import org.piccolo2d.event.PBasicInputEventHandler;
import org.piccolo2d.event.PInputEvent;
import org.piccolo2d.event.PInputEventFilter;
import org.piccolo2d.nodes.PPath;
import org.piccolo2d.util.PBounds;
import org.simbrain.network.NetworkModel;
import org.simbrain.network.gui.NetworkPanel;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.geom.Point2D;

/**
 * <b>ScreenElement</b> extends a Piccolo node with property change, tool tip,
 * and property dialog, and support. Screen elements are automatically support the primary user interactions in the
 * network panel.
 */
public abstract class ScreenElement extends PPath.Float {

    /**
     * Network panel.
     */
    private final NetworkPanel networkPanel;

    /**
     * Create a new abstract screen element with the specified network panel.
     */
    protected ScreenElement(final NetworkPanel np) {
        super();
        networkPanel = np;
        addInputEventListener(new ContextMenuEventHandler());
        addInputEventListener(new PropertyDialogEventHandler());
        addInputEventListener(new ToolTipTextUpdater(networkPanel) {
            protected String getToolTipText() {
                return ScreenElement.this.getToolTipText();
            }
        });
    }

    /**
     * Returns a reference to the model object this node represents.
     */
    public abstract NetworkModel getModel();

    /**
     * Return true if this screen element is selectable.
     *
     * Being selectable requires that this screen element is pickable as far as the Piccolo API is concerned, so if this
     * method returns true, be sure that this class also returns true for PNode.getPickable
     */
    public abstract boolean isSelectable();

    /**
     * Return true if this screen element is draggable. Assumes {@link #isSelectable()} is also true.
     */
    public abstract boolean isDraggable();

    /**
     * Return a String to use as tool tip text for this screen element. Return null if this
     * screen element does not have tool tip text.
     */
    @Nullable
    public String getToolTipText() {return null;}

    /**
     * Return a context menu specific to this screen element or null if none.
     */
    @Nullable
    public JPopupMenu getContextMenu() { return null;}

    /**
     * Return a property dialog for this screen element, or null if it does not have one.
     */
    @Nullable
    public JDialog getPropertyDialog() {
        return null;
    }

    /**
     * Supports "reset to default" in
     * {@link org.simbrain.network.gui.actions.network.ShowNetworkPreferencesAction}.
     */
    public void resetToDefault() {};

    public final NetworkPanel getNetworkPanel() {
        return networkPanel;
    }

    /**
     * Screen element-specific context menu event handler.
     */
    private class ContextMenuEventHandler extends PBasicInputEventHandler {

        /**
         * Show the context menu.
         */
        private void showContextMenu(final PInputEvent event) {
            event.setHandled(true);
            JPopupMenu contextMenu = getContextMenu();
            Point2D canvasPosition = event.getCanvasPosition();
            //networkPanel.getPlacementManager().setLastClickedPosition(canvasPosition);
            contextMenu.show(networkPanel.getCanvas(), (int) canvasPosition.getX(), (int) canvasPosition.getY());
        }

        @Override
        public void mousePressed(final PInputEvent event) {
            if (event.isPopupTrigger()) {
                showContextMenu(event);
            }
        }

        @Override
        public void mouseReleased(final PInputEvent event) {
            if (event.isPopupTrigger()) {
                showContextMenu(event);
            }
        }
    }

    /**
     * Property dialog event handler.
     */
    private class PropertyDialogEventHandler extends PBasicInputEventHandler {

        public PropertyDialogEventHandler() {
            super();
            setEventFilter(new PInputEventFilter(InputEvent.BUTTON1_MASK));
        }

        @Override
        public void mouseClicked(final PInputEvent event) {
            if (event.getClickCount() == 2) {
                event.setHandled(true);
                SwingUtilities.invokeLater(() -> {
                    JDialog propertyDialog = ScreenElement.this.getPropertyDialog();
                    propertyDialog.pack();
                    propertyDialog.setLocationRelativeTo(null);
                    propertyDialog.setVisible(true);
                });
            }
        }
    }

    /**
     * Returns a reference to the the top level PNode of this Screen Element. Usually the ScreenElement is the top level
     * PNode, but in some cases e.g. an interaction box, it's not.  Override in those cases.
     */
    public PNode getNode() {
        return this;
    }

    /**
     * Override if selection events should select something besides the PNode that overrides ScreenElement.
     */
    public ScreenElement getSelectionTarget() {
        return this;
    }

    /**
     * Returns true if the provided bounds intersect this screen element
     */
    public boolean isIntersecting(PBounds bound) {
        return getGlobalBounds().intersects(bound);
    }

    /**
     * Select this element.
     */
    public void select() {
        networkPanel.getSelectionManager().add(this);
    }
}
