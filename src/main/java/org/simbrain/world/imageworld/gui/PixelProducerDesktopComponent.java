package org.simbrain.world.imageworld.gui;

import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.workspace.gui.GuiComponent;
import org.simbrain.world.imageworld.PixelProducerComponent;

public class PixelProducerDesktopComponent extends GuiComponent<PixelProducerComponent> {

    /**
     * Construct a new ImageDesktopComponent GUI.
     *
     * @param frame     The frame in which to place GUI elements.
     * @param component The ImageWorldComponent to interact with.
     */
    public PixelProducerDesktopComponent(GenericFrame frame, PixelProducerComponent component) {
        super(frame, component);
        add(new ImageWorldDesktopPanel(this.getParentFrame(), this, getWorkspaceComponent().getWorld()));
    }

    @Override
    protected void closing() {
    }

}
