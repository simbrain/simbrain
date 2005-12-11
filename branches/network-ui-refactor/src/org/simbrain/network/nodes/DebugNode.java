
package org.simbrain.network.nodes;

import java.awt.geom.Point2D;

import javax.swing.JPopupMenu;

import edu.umd.cs.piccolo.PNode;

import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;

import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;

import org.simbrain.network.NetworkPanel;

/**
 * Debug node.
 */
public final class DebugNode
    extends PNode {

    /** Cached context menu. */
    private JPopupMenu contextMenu;


    /**
     * Create a new debug node.
     */
    public DebugNode(final double x, final double y) {

        super();
        offset(x, y);
        setPickable(true);
        setChildrenPickable(false);

        PText label = new PText("Debug");
        PNode rect = PPath.createRectangle(0.0f, 0.0f, 50.0f, 50.0f);

        label.offset(7.5d, 21.0d);

        rect.addChild(label);
        addChild(rect);

        createContextMenu();

        // add tool tip text updater
        addInputEventListener(new ToolTipTextUpdater() {
 
               /** @see ToolTipTextUpdater */
                protected String getToolTipText() {
                    return "debug";
                }
            });

        addInputEventListener(new ContextMenuEventHandler());
    }

    /**
     * Create a new context menu specific to this debug node.
     *
     * TODO: make abstract?
     */
    private void createContextMenu() {

        contextMenu = new JPopupMenu();
        // add actions
        contextMenu.add(new javax.swing.JMenuItem("Debug node"));
        contextMenu.add(new javax.swing.JMenuItem("Node specific context menu item"));
        contextMenu.add(new javax.swing.JMenuItem("Node specific context menu item"));
    }

    /**
     * Return the context menu specific to this debug node.
     *
     * @return the context menu specific to this debug node
     */
    private JPopupMenu getContextMenu() {
        return contextMenu;
    }


    /**
     * Debug node-specific context menu handler.
     */
    private class ContextMenuEventHandler
        extends PBasicInputEventHandler {

        /**
         * Show the context menu.
         *
         * @param event event
         */
        private void showContextMenu(final PInputEvent event) {

            event.setHandled(true);
            NetworkPanel networkPanel = (NetworkPanel) event.getComponent();
            JPopupMenu contextMenu = getContextMenu();
            Point2D canvasPosition = event.getCanvasPosition();
            contextMenu.show(networkPanel, (int) canvasPosition.getX(), (int) canvasPosition.getY());
        }

        /** @see PBasicInputEventHandler */
        public void mousePressed(final PInputEvent event) {
            if (event.isPopupTrigger()) {
                showContextMenu(event);
            }
        }

        /** @see PBasicInputEventHandler */
        public void mouseReleased(final PInputEvent event) {
            if (event.isPopupTrigger()) {
                showContextMenu(event);
            }
        }
    }
}