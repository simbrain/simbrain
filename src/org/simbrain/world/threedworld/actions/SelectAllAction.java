package org.simbrain.world.threedworld.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.world.threedworld.ThreeDWorld;

public class SelectAllAction extends AbstractAction {
    private static final long serialVersionUID = -4679471843215287046L;
    
    private ThreeDWorld world;
    
    public SelectAllAction(ThreeDWorld world) {
        super("Select All");
        this.world = world;
    }
    
    @Override
    public void actionPerformed(ActionEvent event) {
        world.getEngine().enqueue(() -> {
            world.getSelectionController().selectAll(world.getEntities());
        });
    }
}
