package org.simbrain.world.threedworld.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JToggleButton;

import org.simbrain.resource.ResourceManager;
import org.simbrain.world.threedworld.ThreeDWorld;

public class SnapTransformsAction extends AbstractAction {
    private static final long serialVersionUID = 2391250770941926187L;
    
    private ThreeDWorld world;
    
    public SnapTransformsAction(ThreeDWorld world) {
        super("Snap Transforms");
        this.world = world;
        putValue(SMALL_ICON, ResourceManager.getImageIcon("grid.png"));
        putValue(SHORT_DESCRIPTION, "Snap Transforms");
    }
    
    @Override public void actionPerformed(ActionEvent event) {
        JToggleButton source = (JToggleButton)event.getSource();
        world.getSelectionController().setSnapTransformations(source.isSelected());
    }
}
