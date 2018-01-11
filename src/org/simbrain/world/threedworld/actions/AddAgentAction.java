package org.simbrain.world.threedworld.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.world.threedworld.ThreeDWorld;
import org.simbrain.world.threedworld.entities.Agent;
import org.simbrain.world.threedworld.entities.ModelEntity;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.Vector3f;

public class AddAgentAction extends AbstractAction {
    private static final long serialVersionUID = 5219992141265803787L;
    
    private ThreeDWorld world;
    
    public AddAgentAction(ThreeDWorld world) {
        super("Add Agent");
        this.world = world;
    }
    
    @Override
    public void actionPerformed(ActionEvent event) {
        world.getEngine().enqueue(() -> {
            String fileName = "Models/Monkey.j3o";
            String name = "Agent" + world.createId();
            ModelEntity model = ModelEntity.load(world.getEngine(), name, fileName);
            CollisionShape shape = CollisionShapeFactory.createBoxShape(model.getNode());
            model.setBody(new RigidBodyControl(shape, 1));
            Agent agent = new Agent(model);
            world.getEntities().add(agent);
            world.getSelectionController().select(agent);
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
