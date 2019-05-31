package org.simbrain.world.threedworld.actions;

import org.simbrain.resource.ResourceManager;
import org.simbrain.world.threedworld.ThreeDWorld;
import org.simbrain.world.threedworld.engine.ThreeDEngine;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * ToggleRunAction provides the GUI function of toggling the engine state of a ThreeDWorld
 * from RenderOnly to RunAll.
 */
public class ToggleRunAction extends AbstractAction {
    private static final long serialVersionUID = -8541379191902665764L;

    private ThreeDWorld world;

    /**
     * Construct a new Action.
     *
     * @param world The world to toggle the run status in.
     */
    public ToggleRunAction(ThreeDWorld world) {
        super("Run Physics");
        this.world = world;
        putValue(SMALL_ICON, ResourceManager.getSmallIcon("physics.png"));
        putValue(SHORT_DESCRIPTION, "Run Physics Simulation");
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        ThreeDEngine.State toggleState = (world.getEngine().getState() == ThreeDEngine.State.RenderOnly ? ThreeDEngine.State.RunAll : ThreeDEngine.State.RenderOnly);
        world.getEngine().queueState(toggleState, false);
    }
}
