package org.simbrain.world.threedworld.actions;

import org.simbrain.util.ResourceManager;
import org.simbrain.world.threedworld.ThreeDWorld;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class CameraHomeAction extends AbstractAction {
    private static final long serialVersionUID = 3114294781479510819L;

    private ThreeDWorld world;

    public CameraHomeAction(ThreeDWorld world) {
        super("Camera Home");
        this.world = world;
        putValue(SMALL_ICON, ResourceManager.getSmallIcon("home.png"));
        putValue(SHORT_DESCRIPTION, "Move Camera Home");
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        world.getEngine().enqueue(() -> {
            world.getCameraController().moveCameraHome();
        });
    }
}
