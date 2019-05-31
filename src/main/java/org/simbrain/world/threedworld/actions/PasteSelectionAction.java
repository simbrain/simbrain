package org.simbrain.world.threedworld.actions;

import org.simbrain.world.threedworld.ThreeDWorld;
import org.simbrain.world.threedworld.controllers.ClipboardController;
import org.simbrain.world.threedworld.controllers.ClipboardController.ClipboardListener;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class PasteSelectionAction extends AbstractAction implements ClipboardListener {
    private static final long serialVersionUID = -4679471843215287046L;

    private ThreeDWorld world;

    public PasteSelectionAction(ThreeDWorld world) {
        super("Paste Selection");
        this.world = world;
        world.getClipboardController().addListener(this);
        onClipboardChanged(world.getClipboardController());
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        world.getClipboardController().pasteSelection();
    }

    @Override
    public void onClipboardChanged(ClipboardController controller) {
        setEnabled(controller.hasClipboardContents());
    }
}
