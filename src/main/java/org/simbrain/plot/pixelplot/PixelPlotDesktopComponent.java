package org.simbrain.plot.pixelplot;

import org.simbrain.util.ResourceManager;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.workspace.gui.DesktopComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;

public class PixelPlotDesktopComponent extends DesktopComponent<PixelPlotComponent> {

    /**
     * If true show grid lines.
     */
    private boolean showGridLines = true;

    /**
     * Displays the {@link EmitterMatrix}
     */
    private EmitterPanel emitterPanel = new EmitterPanel();

    /**
     * Construct a new PixelDisplayDesktopComponent GUI.
     *
     * @param frame     The frame in which to place GUI elements.
     * @param component The PixelDisplayComponent to interact with.
     */
    public PixelPlotDesktopComponent(GenericFrame frame, PixelPlotComponent component) {
        super(frame, component);
        setLayout(new BorderLayout());
        // add(BorderLayout.NORTH,getPixelDisplayToolbar());
        add(BorderLayout.CENTER, emitterPanel);
        getWorkspaceComponent().getEmitter().getEvents().onImageUpdate(this::repaint);
    }

    private class EmitterPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            BufferedImage currentImage = getWorkspaceComponent().getEmitter().getImage();
            if (currentImage == null) {
                return;
            }
            graphics.drawImage(currentImage, 0, 0, getWidth(), getHeight(), this);

            // Draw grid lines
            if (showGridLines) {
                // Don't draw gridlines in cases where they would obscure the image itself
                // That is, only draw if image is zoomed in more than 10 times
                if (getWidth() > currentImage.getWidth() * 10 && getHeight() > currentImage.getHeight() * 10) {
                    graphics.setColor(Color.GRAY);
                    for (int i = 0; i < currentImage.getWidth(); i++) {
                        graphics.drawLine(
                                (int) ((double) i / currentImage.getWidth() * getWidth()),
                                0,
                                (int) ((double) i / currentImage.getWidth() * getWidth()),
                                getHeight()
                        );
                    }
                    for (int i = 0; i < currentImage.getHeight(); i++) {
                        graphics.drawLine(
                                0,
                                (int) ((double) i / currentImage.getHeight() * getHeight()),
                                getWidth(),
                                (int) ((double) i / currentImage.getHeight() * getHeight())
                        );
                    }
                }
            }
        }
    }

    /**
     * Toolbar buttons for pixel display world.
     *
     * @return the list of buttons
     */
    public java.util.List<JButton> getPixelDisplayToolbar() {
        List<JButton> returnList = new LinkedList<>();
        JButton editEmitterButton = new JButton();
        editEmitterButton.setIcon(ResourceManager.getSmallIcon("menu_icons/resize.png"));
        editEmitterButton.setToolTipText("Edit Emitter Matrix");
        // editEmitterButton.addActionListener(evt -> {
        //     ResizeEmitterMatrixDialog dialog = new ResizeEmitterMatrixDialog((PixelConsumer) world);
        //     dialog.setVisible(true);
        // });
        returnList.add(editEmitterButton);
        return returnList;
    }

    @Override
    protected void closing() {
    }

}
