package org.simbrain.world.threedworld.actions;

import java.awt.Image;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import org.simbrain.resource.ResourceManager;
import org.simbrain.world.threedworld.ThreeDWorld;

public class ReleaseAgentAction extends AbstractAction {
    private static final long serialVersionUID = -6909554638623111534L;
    
    private ThreeDWorld world;
    
    public ReleaseAgentAction(ThreeDWorld world) {
        super("Release Agent");
        this.world = world;
        putValue(SMALL_ICON, ResourceManager.getSmallIcon("ControlEmpty.png"));
        putValue(SHORT_DESCRIPTION, "Release Agent");
    }
    
    @Override public void actionPerformed(ActionEvent e) {
        if (world.getAgentController().isControlActive())
            world.getAgentController().release();
    }
}
