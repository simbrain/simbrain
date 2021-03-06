package org.simbrain.world.threedworld.actions;

import org.simbrain.world.threedworld.ThreeDWorld;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class EditEntityAction extends AbstractAction {
    private static final long serialVersionUID = 4347451650539760125L;

    private ThreeDWorld world;

    public EditEntityAction(ThreeDWorld world, boolean enabled) {
        super("Edit Entity");
        this.world = world;
        setEnabled(enabled);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        world.getSelectionController().editSelection();
    }
}
