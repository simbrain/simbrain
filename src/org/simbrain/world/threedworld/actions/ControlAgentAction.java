package org.simbrain.world.threedworld.actions;

import java.awt.Image;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import org.simbrain.resource.ResourceManager;
import org.simbrain.world.threedworld.ThreeDWorld;
import org.simbrain.world.threedworld.entities.Agent;
import org.simbrain.world.threedworld.entities.Entity;

public class ControlAgentAction extends AbstractAction {
    private static final long serialVersionUID = 8726881360072302151L;
    
    private ThreeDWorld world;
    
    public ControlAgentAction(ThreeDWorld world) {
        super("Control Agent");
        this.world = world;
        putValue(SMALL_ICON, ResourceManager.getSmallIcon("Control.png"));
        putValue(SHORT_DESCRIPTION, "Control Agent");
        putValue("selected", false);
    }
    
    @Override public void actionPerformed(ActionEvent event) {
        if (world.getSelectionController().hasSelection()) {
            Entity entity = world.getSelectionController().getSelectedEntity();
            if (entity instanceof Agent)
                world.getAgentController().control((Agent)entity);
            putValue("selected", true);
        }
    }
}
