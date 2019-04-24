package org.simbrain.world.imageworld;

import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.workspace.gui.GuiComponent;

public class PixelDisplayDesktopComponent extends GuiComponent<PixelDisplayComponent> {

    /**
     * Construct a new PixelDisplayDesktopComponent GUI.
     *
     * @param frame     The frame in which to place GUI elements.
     * @param component The PixelDisplayComponent to interact with.
     */
    public PixelDisplayDesktopComponent(GenericFrame frame, PixelDisplayComponent component) {
        super(frame, component);
        add(new ImageWorldDesktopPanel(this.getParentFrame(), this, getWorkspaceComponent().getWorld()));
    }

    @Override
    protected void closing() {
    }

}
