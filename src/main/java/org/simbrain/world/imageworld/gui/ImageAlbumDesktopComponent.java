package org.simbrain.world.imageworld.gui;

import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.workspace.gui.GuiComponent;
import org.simbrain.world.imageworld.ImageAlbumComponent;

public class ImageAlbumDesktopComponent extends GuiComponent<ImageAlbumComponent> {


    /**
     * Construct a new ImageDesktopComponent GUI.
     *
     * @param frame     The frame in which to place GUI elements.
     * @param component The ImageWorldComponent to interact with.
     */
    public ImageAlbumDesktopComponent(GenericFrame frame, ImageAlbumComponent component) {
        super(frame, component);
        add(new ImageWorldDesktopPanel(this.getParentFrame(), this, getWorkspaceComponent().getWorld()));
    }

    @Override
    protected void closing() {
    }

}
