package org.simbrain.world.odorworld.gui;

import org.piccolo2d.PNode;
import org.piccolo2d.event.PBasicInputEventHandler;
import org.piccolo2d.event.PInputEvent;
import org.simbrain.util.piccolo.PiccoloUtilsKt;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.OdorWorldPanel;

import javax.swing.*;
import java.awt.event.MouseEvent;

/**
 * Handles context menu mouse event. Since {@link WorldMouseHandler} filters out {@link MouseEvent#BUTTON3},
 * this class is here to handle the right click event that is necessary to bring up the context menu.
 */
public class WorldContextMenuEventHandler extends PBasicInputEventHandler {

    /**
     * Odor World Panel.
     */
    private final OdorWorldPanel odorWorldPanel;

    /**
     * Reference to parent world.
     */
    private final OdorWorld world;

    public WorldContextMenuEventHandler(OdorWorldPanel odorWorldPanel, OdorWorld world) {
        this.odorWorldPanel = odorWorldPanel;
        this.world = world;
    }

    private void showContextMenu(final PInputEvent mouseEvent) {
        if (world == null) {
            return;
        }

        var menuPosition = mouseEvent.getCanvasPosition();

        // Set picked node
        PNode pickedNode = mouseEvent.getPath().getPickedNode();

        // Show context menu for right click
        if (mouseEvent.isPopupTrigger()) {
            mouseEvent.setHandled(true);
            JPopupMenu menu;
            if (pickedNode.getParent() instanceof EntityNode entity) {
                // Selection-based items in the entity menu should act on the clicked entity
                if (!odorWorldPanel.isSelected(entity)) {
                    odorWorldPanel.clearSelection();
                    odorWorldPanel.getSelectionManager().add(entity);
                }
                menu = entity.createContextMenu(odorWorldPanel);
            } else {
                menu = odorWorldPanel.getContextMenu();
            }
            // Apply fix for Piccolo2D mouse button counting bug when JPopupMenu consumes mouse release events
            PiccoloUtilsKt.applyMouseButtonFix(menu, odorWorldPanel.getCanvas());
            menu.show(odorWorldPanel, (int) menuPosition.getX(), (int) menuPosition.getY());
        }
    }

    @Override
    public void mousePressed(final PInputEvent mouseEvent) {
        super.mousePressed(mouseEvent);
        world.lastClickedPosition = mouseEvent.getPosition();
        showContextMenu(mouseEvent);
    }

}
