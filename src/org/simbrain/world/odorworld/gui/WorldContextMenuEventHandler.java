package org.simbrain.world.odorworld.gui;

import org.piccolo2d.PCamera;
import org.piccolo2d.PNode;
import org.piccolo2d.event.PBasicInputEventHandler;
import org.piccolo2d.event.PInputEvent;
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
    }
}
