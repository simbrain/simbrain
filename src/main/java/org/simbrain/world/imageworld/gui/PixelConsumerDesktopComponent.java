package org.simbrain.world.imageworld.gui;

import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.workspace.gui.DesktopComponent;
import org.simbrain.world.imageworld.PixelConsumerComponent;

public class PixelConsumerDesktopComponent extends DesktopComponent<PixelConsumerComponent> {

    /**
     * Construct a new PixelDisplayDesktopComponent GUI.
     *
     * @param frame     The frame in which to place GUI elements.
     * @param component The PixelDisplayComponent to interact with.
     */
    public PixelConsumerDesktopComponent(GenericFrame frame, PixelConsumerComponent component) {
        super(frame, component);
        add(new ImageWorldDesktopPanel(this.getParentFrame(), this, getWorkspaceComponent().getWorld()));
    }

    @Override
    protected void closing() {
    }

}
