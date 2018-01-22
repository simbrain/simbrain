package org.simbrain.world.threedworld.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.world.threedworld.ThreeDWorld;
import org.simbrain.world.threedworld.controllers.SelectionController;
import org.simbrain.world.threedworld.controllers.SelectionController.SelectionListener;

public class CopySelectionAction extends AbstractAction implements SelectionListener {
    private static final long serialVersionUID = -4679471843215287046L;
    
    private ThreeDWorld world;
    
    public CopySelectionAction(ThreeDWorld world) {
        super("Copy Selection");
        this.world = world;
        world.getSelectionController().addListener(this);
        onSelectionChanged(world.getSelectionController());
    }
    
    @Override
    public void actionPerformed(ActionEvent event) {
        world.getClipboardController().copySelection();
    }
    
    @Override
    public void onSelectionChanged(SelectionController controller) {
        setEnabled(controller.hasSelection());
    }
}
