package org.simbrain.world.threedworld.actions;

import com.jme3.math.Vector3f;
import org.simbrain.world.threedworld.ThreeDWorld;
import org.simbrain.world.threedworld.entities.ModelEntity;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class AddEntityAction extends AbstractAction {
    private static final long serialVersionUID = 2605000029274172245L;

    private ThreeDWorld world;

    public AddEntityAction(ThreeDWorld world) {
        super("Add Entity");
        this.world = world;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        world.getEngine().enqueue(() -> {
            String fileName = "Models/UVSphere.j3o";
            String name = "Model" + world.createId();
            ModelEntity model = ModelEntity.load(world.getEngine(), name, fileName);
            world.getEntities().add(model);
            world.getSelectionController().select(model);
            if (world.getSelectionController().getCursorContact(false) != null)
                world.getSelectionController().translateToCursor();
            else {
                Vector3f location = Vector3f.ZERO.clone();
                Vector3f offset = Vector3f.UNIT_Y.clone();
                world.getSelectionController().offsetBoundingVolume(location, offset);
                world.getSelectionController().translateSelection(location);
            }
        }, true);
        world.getSelectionController().editSelection();
    }
}
