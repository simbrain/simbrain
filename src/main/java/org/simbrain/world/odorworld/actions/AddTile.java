package org.simbrain.world.odorworld.actions;

import org.simbrain.world.odorworld.OdorWorldPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class AddTile extends AbstractAction {

    /**
     * Reference to Panel; the action refers to the panel because it needs
     * information on mouse clicks, etc.
     */
    private final OdorWorldPanel worldPanel;

    /**
     * Create a new add entity action.
     *
     * @param worldPanel parent panel.
     */
    public AddTile(final OdorWorldPanel worldPanel) {
        super("Add Tile");
        this.worldPanel = worldPanel;
    }


    public void actionPerformed(final ActionEvent event) {
        worldPanel.getWorld().addTile();
    }
}
