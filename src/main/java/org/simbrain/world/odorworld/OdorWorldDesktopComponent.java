package org.simbrain.world.odorworld;

import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.workspace.gui.DesktopComponent;

import javax.swing.*;
import java.awt.*;

/**
 * <b>WorldPanel</b> is the container for the world component. Handles toolbar
 * buttons, and serializing of world data. The main environment codes is in
 * {@link OdorWorldPanel}.
 */
public class OdorWorldDesktopComponent extends DesktopComponent<OdorWorldComponent> {

    private static final long serialVersionUID = 1L;

    /**
     * Odor world to be in frame.
     */
    private final OdorWorldPanel worldPanel;

    private OdorWorldFrameMenu menu;

    public OdorWorldDesktopComponent(GenericFrame frame, OdorWorldComponent component) {
        super(frame, component);
        setLayout(new BorderLayout());
        worldPanel = new OdorWorldPanel(component, component.getWorld());
        add("Center", worldPanel);
        menu = new OdorWorldFrameMenu(this, component.getWorld());
        menu.setUpMenus();
        getParentFrame().setJMenuBar(menu); // TODO: Move menu creation to this

        worldPanel.getWorld().getEvents().getTileMapChanged().on(this::fitFrameToWorldSize);

        // component.setCurrentDirectory(OdorWorldPreferences.getCurrentDirectory());

        menu = new OdorWorldFrameMenu(this, worldPanel.getWorld());
        menu.setUpMenus();
        getParentFrame().setJMenuBar(menu);
        SwingUtilities.invokeLater(this::fitFrameToWorldSize);
    }

    /**
     * Set frame size to fit the world size. If the world size is too large constrain it to a defaultMaxSize
     */
    public void fitFrameToWorldSize() {
        int defaultMaxSize = 800;
        int worldWidth = (int) worldPanel.getWorld().getWidth();
        int worldHeight = (int) worldPanel.getWorld().getHeight();
        int widthOffset = (int) (getParentFrame().getSize().width - worldPanel.getCanvas().getCamera().getWidth());
        int heightOffset = (int) (getParentFrame().getSize().height - worldPanel.getCanvas().getCamera().getHeight());
        getParentFrame().setPreferredSize(
                new Dimension(
                    Math.min(worldWidth + widthOffset, defaultMaxSize),
                    Math.min(worldHeight + heightOffset, defaultMaxSize)
                )
        );
        SwingUtilities.invokeLater(() -> worldPanel.setScalingFactor(1));
        getParentFrame().pack();
    }

    /**
     * Resizes the frame of the parent window to fit the specified width and height,
     * including necessary adjustments for offsets caused by frame decorations.
     * And zoom out to fit the world size.
     *
     * @param width the desired camera width of the content area to be fit
     * @param height the desired camera height of the content area to be fit
     */
    public void zoomToFitSize(int width, int height) {
        int widthOffset = getParentFrame().getSize().width - worldPanel.getCanvas().getWidth();
        int heightOffset = getParentFrame().getSize().height - worldPanel.getCanvas().getHeight();
        ((JInternalFrame) getParentFrame()).setSize(width + widthOffset, height + heightOffset);
        SwingUtilities.invokeLater(() -> worldPanel.getCanvas().scale(0.01)); // zoom out all the way
    }

    public OdorWorldPanel getWorldPanel() {
        return worldPanel;
    }

    public OdorWorldFrameMenu getMenu() {
        return menu;
    }

    public void setMenu(final OdorWorldFrameMenu menu) {
        this.menu = menu;
    }

}
