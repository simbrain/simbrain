package org.simbrain.world.threedworld;

import java.awt.Point;

import javax.swing.JPopupMenu;

import org.simbrain.world.threedworld.engine.ThreeDEngine;

/**
 * ContextMenu populates a popup menu with ThreeDWorld actions and displays
 * it at the location of the cursor.
 */
public class ContextMenu {
    private JPopupMenu popupMenu;

    /**
     * Construct a new ContextMenu.
     * @param world The world from which to get the context actions.
     */
    public ContextMenu(ThreeDWorld world) {
        popupMenu = new JPopupMenu();
        popupMenu.add(world.getAction("Add Entity"));
        popupMenu.add(world.getAction("Add Block"));
        popupMenu.add(world.getAction("Add Agent"));
        popupMenu.add(world.getAction("Add Mouse"));
        popupMenu.addSeparator();
        popupMenu.add(world.getAction("Copy Selection"));
        popupMenu.add(world.getAction("Paste Selection"));
        popupMenu.add(world.getAction("Delete Selection"));
        popupMenu.add(world.getAction("Edit Entity"));
        popupMenu.addSeparator();
        popupMenu.add(world.getAction("Control Agent"));
        popupMenu.add(world.getAction("Release Agent"));
    }

    /**
     * Show the ContextMenu at the current cursor position.
     * @param engine The ThreeDEngine from which to get the cursor position.
     */
    public void show(ThreeDEngine engine) {
        Point position = engine.getPanel().getMousePosition();
        if (position == null) {
            position = new Point(0, 0);
        }
        popupMenu.show(engine.getPanel(), position.x, position.y);
    }

    /**
     * Hide the ContextMenu if it is showing.
     */
    public void hide() {
        popupMenu.setVisible(false);
    }
}
