package org.simbrain.world.threedworld.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

import org.simbrain.world.threedworld.ThreeDWorld;
import org.simbrain.world.threedworld.entities.BoxEntity;
import org.simbrain.world.threedworld.entities.Entity;

import com.jme3.math.Vector3f;

public class AddBlockAction extends AbstractAction {
    private static final long serialVersionUID = 2605000029274172245L;
    
    private ThreeDWorld world;
    
    public AddBlockAction(ThreeDWorld world) {
        super("Add Block");
        this.world = world;
    }
    
    @Override
    public void actionPerformed(ActionEvent event) {
        world.getEngine().enqueue(() -> {
            Entity entity = new BoxEntity(world.getEngine(), "Block" + world.createId());
            world.getEntities().add(entity);
            world.getSelectionController().select(entity);
            if (world.getSelectionController().getCursorContact(false) != null)
                world.getSelectionController().translateToCursor();
            else {
                Vector3f location = Vector3f.ZERO.clone();
                Vector3f offset = Vector3f.UNIT_Y.clone();
                world.getSelectionController().offsetBoundingVolume(location, offset);
                world.getSelectionController().translateSelection(location);
            }
        });
    }
}
