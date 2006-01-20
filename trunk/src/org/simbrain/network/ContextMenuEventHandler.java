
package org.simbrain.network;

import java.awt.geom.Point2D;

import javax.swing.JPopupMenu;

import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;

/**
 * Context menu event handler.
 */
final class ContextMenuEventHandler
    extends PBasicInputEventHandler {

    /**
     * Show the context menu.
     *
     * @param event event
     */
    private void showContextMenu(final PInputEvent event) {

        event.setHandled(true);  // seems to confuse zoom event handler??
        NetworkPanel networkPanel = (NetworkPanel) event.getComponent();
        JPopupMenu contextMenu = networkPanel.getContextMenu();
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