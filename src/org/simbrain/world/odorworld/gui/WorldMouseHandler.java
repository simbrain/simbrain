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
package org.simbrain.world.odorworld.gui;

import org.piccolo2d.PCamera;
import org.piccolo2d.PLayer;
import org.piccolo2d.PNode;
import org.piccolo2d.event.PDragSequenceEventHandler;
import org.piccolo2d.event.PInputEvent;
import org.piccolo2d.event.PInputEventFilter;
import org.piccolo2d.nodes.PPath;
import org.piccolo2d.util.PBounds;
import org.piccolo2d.util.PDimension;
import org.piccolo2d.util.PNodeFilter;
import org.simbrain.network.gui.nodes.SelectionMarquee;
import org.simbrain.network.gui.nodes.SynapseNode;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;
import org.simbrain.util.piccolo.Tile;
import org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.OdorWorldPanel;
import org.simbrain.world.odorworld.actions.ShowEntityDialogAction;
import org.simbrain.world.odorworld.dialogs.EntityDialog;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * Handle simbrain drag events, which pan the canvas, create lassos for
 * selection. handles selection an, toggle selection, drags objects as
 * appropriate, updates relevant graphics parameters like "last clicked
 * position".
 * <p>
 * Coding this properly requires tracking picked nodes and their parents fairly
 * closely. To see the scene graph hierarchy for debugging this use ctrl-c while
 * a network panel is open.
 *
 * @author Michael Heuer
 * @author Jeff Yoshimi
 */
public final class WorldMouseHandler extends PDragSequenceEventHandler {

    //TODO: Factor out common features and move to piccolo utility

    /**
     * Selection marquee.
     */
    private SelectionMarquee marquee;

    /**
     * Picked node, if any, at the beginning of this drag sequence.
     */
    private PNode pickedNode;

    /**
     * Marquee selection start position.
     */
    private Point2D marqueeStartPosition;

    /**
     * Bounds filter.
     */
    private final BoundsFilter boundsFilter;

    /**
     * Prior selection, if any. Required for shift-lasso selection.
     */
    private Collection priorSelection = Collections.EMPTY_LIST;

    /**
     * Odor World Panel.
     */
    private final OdorWorldPanel odorWorldPanel;

    /**
     * Reference to parent world.
     */
    private final OdorWorld world;

    /**
     * Create a new selection event handler.
     *
     * @param odorWorldPanel parent panel
     */
    public WorldMouseHandler(OdorWorldPanel odorWorldPanel, OdorWorld world) {
        super();
        this.world = world;
        boundsFilter = new BoundsFilter();
        setEventFilter(new SelectionEventFilter());
        this.odorWorldPanel = odorWorldPanel;
    }

    @Override
    public void mousePressed(final PInputEvent mouseEvent) {
        super.mousePressed(mouseEvent);

        if(world == null) {
            return;
        }

        // Set last clicked position, used in many areas for "placement" of
        // objects in the last clicked position on screen.
        world.setLastClickedPosition(mouseEvent.getCanvasPosition());

        // Set picked node
        PNode pickedNode = mouseEvent.getPath().getPickedNode();

        // Show context menu for right click
        if (mouseEvent.isControlDown() || (mouseEvent.getButton() == MouseEvent.BUTTON3)) {
            if (pickedNode.getParent() instanceof EntityNode) {
                JPopupMenu menu = odorWorldPanel.getContextMenu(((EntityNode) pickedNode.getParent()).getEntity());
                menu.show(odorWorldPanel, (int) world.getLastClickedPosition().getX(), (int) world.getLastClickedPosition().getY());
            } else {
                JPopupMenu menu = odorWorldPanel.getContextMenu(null);
                menu.show(odorWorldPanel, (int) world.getLastClickedPosition().getX(), (int) world.getLastClickedPosition().getY());
            }
        }

        // Set pressed position for use in double clicking
        if (pickedNode instanceof PCamera) {
            odorWorldPanel.setBeginPosition(mouseEvent.getPosition());
        }
    }

    @Override
    public void mouseClicked(final PInputEvent event) {

        // System.out.println("In drag event handler mouse clicked:" + event);
        super.mouseClicked(event);

        // Set picked node
        PNode node = event.getPath().getPickedNode();
        // System.out.println("Mouse clicked / Picked node: " + node);

        // Double click on entities to edit them
        if (event.getClickCount() != 1) {
            if (node.getParent() instanceof EntityNode) {
                EntityNode entityNode = (EntityNode) node.getParent();
                // Edit odor world entity properties
                EntityDialog dialog = new EntityDialog(entityNode.getEntity());
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
            } else {
                Tile tile = odorWorldPanel.getTile(event.getPosition());
                if (tile == null) {
                    return;
                }
                AnnotatedPropertyEditor ape = new AnnotatedPropertyEditor(tile);
                StandardDialog dialog = ape.getDialog();
                dialog.setLocationRelativeTo(null);
                dialog.pack();
                dialog.setVisible(true);
            }
            return;
        }

        // Clicking in empty parts of the canvas removes green selections.
        if (node instanceof PCamera) {
            if (!event.isShiftDown()) {
                odorWorldPanel.clearSelection();
            }
        }
    }

    @Override
    protected void startDrag(final PInputEvent event) {

        super.startDrag(event);

        marqueeStartPosition = event.getPosition();

        // Set the initially picked node
        pickedNode = event.getPath().getPickedNode();

        // Cases where nothing was clicked on
        if (noObjectWasClickedOn()) {
            if (event.isShiftDown()) {
                priorSelection = new ArrayList(odorWorldPanel.getSelection());
            } else {
                // Don't clear selection when panning screen
                if (!event.isMetaDown()) {
                    odorWorldPanel.clearSelection();
                }
            }

            // Create a new selection marquee at the mouse position
            marquee = new SelectionMarquee((float) marqueeStartPosition.getX(), (float) marqueeStartPosition.getY());

            // Add marquee as child of the network panel's layer
            odorWorldPanel.getCanvas().getLayer().addChild(marquee);
            return;
        }

        // Either start dragging selected node(s) or toggle selection (if shift
        // is pressed).
        if (odorWorldPanel.isSelected(pickedNode)) {
            if (event.isShiftDown()) {
                odorWorldPanel.toggleSelection(pickedNode);
            }
        } else {
            if (event.isShiftDown()) {
                odorWorldPanel.toggleSelection(pickedNode);
            } else {
                odorWorldPanel.setSelection(Collections.singleton(pickedNode));
            }
        }
    }

    @Override
    protected void drag(final PInputEvent event) {

        super.drag(event);

        // If the command/control button is down,
        // pan the canvas.
        if (event.isMetaDown()) {
            pan(event);
            return;
        }

        // The case where nothing was clicked on initially. So draw the lasso
        // and select things.
        if (noObjectWasClickedOn()) {
            // Select lassoed nodes
            Point2D position = event.getPosition();
            PBounds rect = new PBounds();
            rect.add(marqueeStartPosition);
            rect.add(position);
            marquee.globalToLocal(rect);
            marquee.reset(); //todo: better way?
            marquee.append(new Rectangle2D.Float((float) rect.getX(), (float) rect.getY(), (float) rect.getWidth(), (float) rect.getHeight()), false);
            boundsFilter.setBounds(rect);
            Collection highlightedNodes = odorWorldPanel.getCanvas().getLayer().getRoot().getAllNodes(boundsFilter, null);
            // Toggle things if shift is being pressed
            if (event.isShiftDown()) {
                Collection selection = Utils.union(priorSelection, highlightedNodes);
                selection.removeAll(Utils.intersection(priorSelection, highlightedNodes));
                odorWorldPanel.setSelection(selection);
            } else {
                odorWorldPanel.setSelection(highlightedNodes);
            }
            return;

        }

        // Where is the drag in relation to the initially clicked on object
        PDimension delta = event.getDeltaRelativeTo(pickedNode);

        //System.out.println("Drag:" + pickedNode);

        // Continue to drag nodes that have already been selected
        for (PNode node : odorWorldPanel.getSelectedEntities()) {
            // TODO: networkpanel has a draggable flag here
            // TODO: Only update model at end of drag.
            // TODO: This getparent business...
            node.localToParent(delta);
            node.offset(delta.getWidth(), delta.getHeight());
            // Below needed for proper continuous updating of couplings
            if (node instanceof EntityNode) {
                ((EntityNode) node).pushViewPositionToModel();
            }
        }

    }

    @Override
    protected void endDrag(final PInputEvent event) {

        super.endDrag(event);

        // Nothing was being dragged
        if (noObjectWasClickedOn()) {
            // End lasso selection
            marquee.removeFromParent();
            marquee = null;
            marqueeStartPosition = null;
            return;
        }

        // End drag selected node(s)
        pickedNode = null;

        priorSelection = Collections.EMPTY_LIST;
        odorWorldPanel.repaint();
    }

    /**
     * Encapsulate logic for determining the case where no object (neuron node,
     * synpase node, etc) was clicked on at the beginning of this drag
     * sequence.
     *
     * @return true if no object was clicked on, false otherwise.
     */
    private boolean noObjectWasClickedOn() {
        boolean pickedNodeNull = (pickedNode == null);
        boolean cameraPicked = (pickedNode instanceof PCamera);
        return (pickedNodeNull || cameraPicked);
    }

    /**
     * A filter that determines whether a given pnode is selectable or not.
     * Bounds are updated as the lasso tool is dragged.
     */
    private class BoundsFilter implements PNodeFilter {

        /**
         * Bounds.
         */
        private PBounds bounds;

        /**
         * Set the bounds for this bounds filter to <code>bounds</code>.
         *
         * @param bounds bounds for this bounds filter
         */
        public void setBounds(final PBounds bounds) {
            this.bounds = bounds;
        }

        /**
         * @param node
         * @return
         * @see PNodeFilter
         */
        public boolean accept(final PNode node) {
            boolean isPickable = node.getPickable();
            boolean boundsIntersects = node.getGlobalBounds().intersects(bounds);
            // Allow selection of synapses via the line associated with it
            if (node instanceof SynapseNode) {
                PPath.Float line = ((SynapseNode) node).getLine();
                if (bounds.intersects(line.getGlobalBounds())) {
                    boundsIntersects = true;
                }

            }
            boolean isLayer = (node instanceof PLayer);
            boolean isCamera = (node instanceof PCamera);
            boolean isMarquee = (marquee == node);

            return (isPickable && boundsIntersects && !isLayer && !isCamera && !isMarquee);
        }

        /**
         * @param node
         * @return
         * @see PNodeFilter
         */
        public boolean acceptChildrenOf(final PNode node) {
            boolean areChildrenPickable = node.getChildrenPickable();
            boolean isCamera = (node instanceof PCamera);
            boolean isLayer = (node instanceof PLayer);
            boolean isMarquee = (marquee == node);

            return ((areChildrenPickable || isCamera || isLayer) && !isMarquee);
        }
    }

    /**
     * Selection event filter, accepts various mouse events, but only when the
     * network panel's edit mode is <code>EditMode.SELECTION</code>.
     */
    private class SelectionEventFilter extends PInputEventFilter {

        /**
         * Create a new selection event filter.
         */
        public SelectionEventFilter() {
            super(InputEvent.BUTTON1_MASK);
        }

//        /**
//         * @param event
//         * @param type
//         * @return
//         * @see PInputEventFilter
//         */
//        public boolean acceptsEvent(final PInputEvent event, final int type) {
//
//            EditMode editMode = odorWorldPanel.getEditMode();
//
//            if (editMode.isSelection() && super.acceptsEvent(event, type)) {
//                odorWorldPanel.getTextHandle().stopEditing();
//                return true;
//            } else {
//                return false;
//            }
//        }
    }

    /**
     * Pans the camera in response to the pan event provided. (From the source
     * code for PanEventHandler. Note that "autopan"--from that class--is not
     * being used. Not sure what is being lost by not using it.)
     *
     * @param event contains details about the drag used to translate the view
     * @author Jesse Grosjean
     */
    protected void pan(final PInputEvent event) {
        final PCamera c = event.getCamera();
        final Point2D l = event.getPosition();

        if (c.getViewBounds().contains(l)) {
            final PDimension d = event.getDelta();
            c.translateView(d.getWidth(), d.getHeight());
        }
    }

}
