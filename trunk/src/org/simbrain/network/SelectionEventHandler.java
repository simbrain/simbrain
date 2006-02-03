
package org.simbrain.network;

import java.awt.event.InputEvent;

import java.awt.geom.Point2D;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PCamera;

import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PInputEventFilter;
import edu.umd.cs.piccolo.event.PDragSequenceEventHandler;

import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PDimension;
import edu.umd.cs.piccolo.util.PNodeFilter;

import org.apache.commons.collections.CollectionUtils;

import org.simbrain.network.nodes.NeuronNode;
import org.simbrain.network.nodes.ScreenElement;
import org.simbrain.network.nodes.SelectionMarquee;
import org.simbrain.network.nodes.SynapseNode;

/**
 * Selection event handler.
 *
 * <h4>Desired behaviour</h4>
 * <table style="table-layout: fixed; border-collapse: collapse">
 *   <tr>
 *     <th>mouse event</th><th>mouse button</th><th>mouse over</th>
 *     <th>shift key</th><th>control key</th><th>command key</th>
 *     <th>resulting behaviour</th><th>responsible class</th>
 *   </tr>
 *   <tr style="background-color: #eeeeee; border-top: 1px solid #dddddd; border-left: 1px solid #dddddd; border-right: 1px solid #dddddd;">
 *     <td>single click</td><td>first, left</td><td>nothing, the camera</td>
 *     <td>any</td><td>may be considered a popup trigger</td><td>may be considered a popup trigger</td>
 *     <td>if a popup trigger, show the network panel context menu, otherwise clear selection</td>
 *     <td>if a popup trigger, ContextMenuEventHandler of NetworkPanel, otherwise this class</td>
 *   </tr>
 *   <tr style="background-color: #eeeeee; border-left: 1px solid #dddddd; border-right: 1px solid #dddddd;">
 *     <td>single click</td><td>first, left</td><td>a non-pickable node</td>
 *     <td>any</td><td>may be considered a popup trigger</td><td>may be considered a popup trigger</td>
 *     <td>if a popup trigger, show the network panel context menu, otherwise clear selection</td>
 *     <td>if a popup trigger, ContextMenuEventHandler of NetworkPanel, otherwise this class</td>
 *   </tr>
 *   <tr style="background-color: #f5f5f5; border-left: 1px solid #dddddd; border-right: 1px solid #dddddd;">
 *     <td>single click</td><td>first, left</td><td>a pickable node</td>
 *     <td>no</td><td>may be considered a popup trigger</td><td>may be considered a popup trigger</td>
 *     <td>if a popup trigger, show the node-specific context menu, otherwise set selection to that node</td>
 *     <td>if a popup trigger, ContextMenuEventHandler of ScreenElement, otherwise this class</td>
 *   </tr>
 *   <tr style="background-color: #eeeeee; border-left: 1px solid #dddddd; border-right: 1px solid #dddddd;">
 *     <td>single click</td><td>first, left</td><td>a pickable node</td>
 *     <td>yes</td><td>may be considered a popup trigger</td><td>may be considered a popup trigger</td>
 *     <td>if a popup trigger, show the node-specific context menu, otherwise toggle selection state of that node</td>
 *     <td>if a popup trigger, ContextMenuEventHandler of ScreenElement, otherwise this class</td>
 *   </tr>
 *   <tr style="background-color: #f5f5f5; border-left: 1px solid #dddddd; border-right: 1px solid #dddddd;">
 *     <td>single click</td><td>first, left</td><td>a pickable node</td>
 *     <td>no</td><td>may be considered a popup trigger</td><td>yes, may be considered a popup trigger</td>
 *     <td>if a popup trigger, show the node-specific context menu, otherwise set selection to that node</td>
 *     <td>if a popup trigger, ContextMenuEventHandler of ScreenElement, otherwise this class</td>
 *   </tr>
 *   <tr style="background-color: #eeeeee; border-left: 1px solid #dddddd; border-right: 1px solid #dddddd;">
 *     <td>single click</td><td>second, center, middle</td><td>any</td>
 *     <td>any</td><td>any</td><td>any</td>
 *     <td>ignored</td><td>ignored</td>
 *   </tr>
 *   <tr style="background-color: #f5f5f5; border-left: 1px solid #dddddd; border-right: 1px solid #dddddd;">
 *     <td>single click</td><td>third, right</td><td>nothing, the camera</td>
 *     <td>any</td><td>any</td><td>any</td>
 *     <td>show the network panel context menu</td>
 *     <td>ContextMenuEventHandler of NetworkPanel</td>
 *   </tr>
 *   <tr style="background-color: #eeeeee; border-left: 1px solid #dddddd; border-right: 1px solid #dddddd;">
 *     <td>single click</td><td>third, right</td><td>a non-pickable node</td>
 *     <td>any</td><td>any</td><td>any</td>
 *     <td>show the network panel context menu</td>
 *     <td>ContextMenuEventHandler of NetworkPanel</td>
 *   </tr>
 *   <tr style="background-color: #f5f5f5; border-left: 1px solid #dddddd; border-right: 1px solid #dddddd;">
 *     <td>single click</td><td>third, right</td><td>a pickable node</td>
 *     <td>any</td><td>any</td><td>any</td>
 *     <td>show the node-specific context menu</td>
 *     <td>ContextMenuEventHandler of ScreenElement</td>
 *   </tr>
 *   <tr style="background-color: #eeeeee; border-left: 1px solid #dddddd; border-right: 1px solid #dddddd;">
 *     <td>double click</td><td>first, left</td><td>nothing, the camera</td>
 *     <td>any</td><td>any</td><td>any</td>
 *     <td>ignored</td><td>ignored</td>
 *   </tr>
 *   <tr style="background-color: #f5f5f5; border-left: 1px solid #dddddd; border-right: 1px solid #dddddd;">
 *     <td>double click</td><td>first, left</td><td>a non-pickable node</td>
 *     <td>any</td><td>any</td><td>any</td>
 *     <td>ignored</td><td>ignored</td>
 *   </tr>
 *   <tr style="background-color: #eeeeee; border-left: 1px solid #dddddd; border-right: 1px solid #dddddd;">
 *     <td>double click</td><td>first, left</td><td>a pickable node</td>
 *     <td>any</td><td>any</td><td>any</td>
 *     <td>shows a properties dialog for all selected objects of that type</td>
 *     <td>TBD, should probably be a mouse listener on ScreenElement</td>
 *   </tr>
 *   <tr style="background-color: #f5f5f5; border-left: 1px solid #dddddd; border-right: 1px solid #dddddd;">
 *     <td>double click</td><td>second, center, middle</td><td>any</td>
 *     <td>any</td><td>any</td><td>any</td>
 *     <td>ignored</td><td>ignored</td>
 *   </tr>
 *   <tr style="background-color: #eeeeee; border-left: 1px solid #dddddd; border-right: 1px solid #dddddd;">
 *     <td>double click</td><td>third, right</td><td>any</td>
 *     <td>any</td><td>any</td><td>any</td>
 *     <td>ignored</td><td>ignored</td>
 *   </tr>
 *   <tr style="background-color: #f5f5f5; border-left: 1px solid #dddddd; border-right: 1px solid #dddddd;">
 *     <td>drag</td><td>first, left</td><td>nothing, the camera</td>
 *     <td>no</td><td>may be considered a popup trigger</td><td>may be considered a popup trigger</td>
 *     <td>if a popup trigger, show the network panel context menu, otherwise clear selection and create a selection marquee</td>
 *     <td>if a popup trigger, ContextMenuEventHandler of NetworkPanel, otherwise this class</td>
 *   </tr>
 *   <tr style="background-color: #eeeeee; border-left: 1px solid #dddddd; border-right: 1px solid #dddddd;">
 *     <td>drag</td><td>first, left</td><td>nothing, the camera</td>
 *     <td>yes</td><td>may be considered a popup trigger</td><td>may be considered a popup trigger</td>
 *     <td>if a popup trigger, show the network panel context menu, otherwise create a selection marquee that toggles selection</td>
 *     <td>if a popup trigger, ContextMenuEventHandler of NetworkPanel, otherwise this class</td>
 *   </tr>
 *   <tr style="background-color: #f5f5f5; border-left: 1px solid #dddddd; border-right: 1px solid #dddddd;">
 *     <td>drag</td><td>first, left</td><td>nothing, the camera</td>
 *     <td>no</td><td>may be considered a popup trigger</td><td>yes, may be considered a popup trigger</td>
 *     <td>if a popup trigger, show the network panel context menu, otherwise clear selection and create a selection marquee</td>
 *     <td>if a popup trigger, ContextMenuEventHandler of NetworkPanel, otherwise this class</td>
 *   </tr>
 *   <tr style="background-color: #eeeeee; border-left: 1px solid #dddddd; border-right: 1px solid #dddddd;">
 *     <td>drag</td><td>first, left</td><td>a non-pickable node</td>
 *     <td>no</td><td>may be considered a popup trigger</td><td>may be considered a popup trigger</td>
 *     <td>if a popup trigger, show the network panel context menu, otherwise clear selection and create a selection marquee</td>
 *     <td>if a popup trigger, ContextMenuEventHandler of NetworkPanel, otherwise this class</td>
 *   </tr>
 *   <tr style="background-color: #f5f5f5; border-left: 1px solid #dddddd; border-right: 1px solid #dddddd;">
 *     <td>drag</td><td>first, left</td><td>a non-pickable node</td>
 *     <td>yes</td><td>may be considered a popup trigger</td><td>may be considered a popup trigger</td>
 *     <td>if a popup trigger, show the network panel context menu, otherwise create a selection marquee</td>
 *     <td>if a popup trigger, ContextMenuEventHandler of NetworkPanel, otherwise this class</td>
 *   </tr>
 *   <tr style="background-color: #eeeeee; border-left: 1px solid #dddddd; border-right: 1px solid #dddddd;">
 *     <td>drag</td><td>first, left</td><td>a non-pickable node</td>
 *     <td>no</td><td>may be considered a popup trigger</td><td>yes, may be considered a popup trigger</td>
 *     <td>if a popup trigger, show the network panel context menu, otherwise clear selection and create a selection marquee</td>
 *     <td>if a popup trigger, ContextMenuEventHandler of NetworkPanel, otherwise this class</td>
 *   </tr>
 *   <tr style="background-color: #f5f5f5; border-left: 1px solid #dddddd; border-right: 1px solid #dddddd;">
 *     <td>drag</td><td>second, center, middle</td><td>any</td>
 *     <td>any</td><td>any</td><td>any</td>
 *     <td>ignored</td><td>ignored</td>
 *   </tr>
 *   <tr style="background-color: #eeeeee; border-left: 1px solid #dddddd; border-right: 1px solid #dddddd; border-bottom: 1px solid #dddddd;">
 *     <td>drag</td><td>third, right</td><td>any</td>
 *     <td>any</td><td>any</td><td>any</td>
 *     <td>ignored</td><td>ignored</td>
 *   </tr>
 * </table>
 */
final class SelectionEventHandler
    extends PDragSequenceEventHandler {

    /** Selection marquee. */
    private SelectionMarquee marquee;

    /** Picked node, if any. */
    private PNode pickedNode;

    /** Marquee selection start position. */
    private Point2D marqueeStartPosition;

    /** Bounds filter. */
    private final BoundsFilter boundsFilter;

    /** Prior selection, if any.  Required for shift-lasso selection. */
    private Collection priorSelection = Collections.EMPTY_LIST;
    

    /**
     * Create a new selection event handler.
     */
    public SelectionEventHandler() {
        super();

        boundsFilter = new BoundsFilter();
        setEventFilter(new SelectionEventFilter());
    }

    /** @see PDragSequenceEventHandler */
    public void mousePressed(final PInputEvent event) {
        super.mousePressed(event);
        NetworkPanel networkPanel = (NetworkPanel) event.getComponent();
        networkPanel.setLastClickedPosition(event.getPosition());
    }

    /** @see PDragSequenceEventHandler */
    public void mouseClicked(final PInputEvent event) {

        super.mouseClicked(event);

        if (event.getClickCount() != 1) {
            return;
        }

        PNode node = event.getPath().getPickedNode();
        NetworkPanel networkPanel = (NetworkPanel) event.getComponent();
        networkPanel.setLastClickedPosition(event.getPosition());

        if (node instanceof PCamera) {
            if (!event.isShiftDown()) {
                networkPanel.clearSelection();
            }
        }
    }

    /** @see PDragSequenceEventHandler */
    protected void startDrag(final PInputEvent event) {

        super.startDrag(event);

        marqueeStartPosition = event.getPosition();
        pickedNode = event.getPath().getPickedNode();
        NetworkPanel networkPanel = (NetworkPanel) event.getComponent();

        if (pickedNode instanceof PCamera) {
            pickedNode = null;
        }

        if (pickedNode == null) {

            if (event.isShiftDown()) {
                priorSelection = new ArrayList(networkPanel.getSelection());
            }
            else {
                networkPanel.clearSelection();
            }

            // create a new selection marquee at the mouse position
            marquee = new SelectionMarquee((float) marqueeStartPosition.getX(),
                                           (float) marqueeStartPosition.getY());

            // add marquee as child of the network panel's layer
            networkPanel.getLayer().addChild(marquee);
        } else {
            if (pickedNode instanceof NeuronNode) {
                networkPanel.setLastSelectedNeuron((NeuronNode) pickedNode);
            }

            // start dragging selected node(s)
            if (networkPanel.isSelected(pickedNode)) {
                if (event.isShiftDown()) {
                    networkPanel.toggleSelection(pickedNode);
                }
            }
            else {
                if (event.isShiftDown()) {
                    networkPanel.toggleSelection(pickedNode);
                }
                else {
                    networkPanel.setSelection(Collections.singleton(pickedNode));
                }
            }
        }
    }

    /** @see PDragSequenceEventHandler */
    protected void drag(final PInputEvent event) {

        super.drag(event);

        NetworkPanel networkPanel = (NetworkPanel) event.getComponent();

        if (pickedNode == null) {

            // continue marquee selection
            Point2D position = event.getPosition();

            PBounds rect = new PBounds();
            rect.add(marqueeStartPosition);
            rect.add(position);

            marquee.globalToLocal(rect);

            marquee.setPathToRectangle((float) rect.getX(), (float) rect.getY(),
                                       (float) rect.getWidth(), (float) rect.getHeight());

            boundsFilter.setBounds(rect);

            Collection highlightedNodes = networkPanel.getLayer().getRoot().getAllNodes(boundsFilter, null);

            if (event.isShiftDown()) {
                Collection selection = CollectionUtils.union(priorSelection, highlightedNodes);
                selection.removeAll(CollectionUtils.intersection(priorSelection, highlightedNodes));
                networkPanel.setSelection(selection);
            }
            else {
                networkPanel.setSelection(highlightedNodes);
            }

        } else {
            // continue to drag selected node(s)
            PDimension delta = event.getDeltaRelativeTo(pickedNode);

            for (Iterator i = networkPanel.getSelection().iterator(); i.hasNext();) {
                PNode node = (PNode) i.next();

                if (node instanceof ScreenElement) {
                    ScreenElement screenElement = (ScreenElement) node;

                    if (screenElement.isDraggable()) {
                        screenElement.localToParent(delta);
                        screenElement.offset(delta.getWidth(), delta.getHeight());
                        networkPanel.getNetworkFrame().setChangedSinceLastSave(true);
                    }
                }
            }
        }
    }
    
    /** @see PDragSequenceEventHandler */
    protected void endDrag(final PInputEvent event) {

        super.endDrag(event);

        if (pickedNode == null) {
            // end marquee selection
            marquee.removeFromParent();
            marquee = null;
            marqueeStartPosition = null;
        }
        else {
            // end drag selected node(s)
            pickedNode = null;
        }
        priorSelection = Collections.EMPTY_LIST;
        NetworkPanel networkPanel = (NetworkPanel) event.getComponent();
        networkPanel.repaint();
    }


    /**
     * Bounds filter.
     */
    private class BoundsFilter
        implements PNodeFilter {

        /** Bounds. */
        private PBounds bounds;


        /**
         * Set the bounds for this bounds filter to <code>bounds</code>.
         *
         * @param bounds bounds for this bounds filter
         */
        public void setBounds(final PBounds bounds) {
            this.bounds = bounds;
        }

        /** @see PNodeFilter */
        public boolean accept(final PNode node) {
                        
            boolean isPickable = node.getPickable();
            boolean boundsIntersects = node.getGlobalBounds().intersects(bounds);
            boolean isLayer = (node instanceof PLayer);
            boolean isCamera = (node instanceof PCamera);
            boolean isMarquee = (marquee == node);

            return (isPickable && boundsIntersects && !isLayer && !isCamera && !isMarquee);
        }

        /** @see PNodeFilter */
        public boolean acceptChildrenOf(final PNode node) {

            boolean areChildrenPickable = node.getChildrenPickable();
            boolean isCamera = (node instanceof PCamera);
            boolean isLayer = (node instanceof PLayer);
            boolean isMarquee = (marquee == node);

            return ((areChildrenPickable || isCamera || isLayer) && !isMarquee);
        }
    }

    /**
     * Selection event filter, accepts various mouse events, but only when
     * the network panel's edit mode is <code>EditMode.SELECTION</code>.
     */
    private class SelectionEventFilter
        extends PInputEventFilter {

        /**
         * Create a new selection event filter.
         */
        public SelectionEventFilter() {
            super(InputEvent.BUTTON1_MASK);
        }


        /** @see PInputEventFilter */
        public boolean acceptsEvent(final PInputEvent event, final int type) {

            NetworkPanel networkPanel = (NetworkPanel) event.getComponent();
            EditMode editMode = networkPanel.getEditMode();

            return (editMode.isSelection() && super.acceptsEvent(event, type));
        }
    }
}