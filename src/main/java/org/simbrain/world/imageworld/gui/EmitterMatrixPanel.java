package org.simbrain.world.imageworld.gui;

import org.simbrain.world.imageworld.ImageSource;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Display images from an ImageSource.
 *
 * @author Tim Shea
 * @author Jeff Yoshimi
 */
public class EmitterMatrixPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /**
     * If true show grid lines.
     */
    private boolean showGridLines;

    /**
     * Construct the world.
     *
     * @param showGridLines
     */
    public EmitterMatrixPanel(boolean showGridLines) {
        this.showGridLines = showGridLines;
    }

    /**
     * Reference to last image provided, so we don't to have reload the image
     * every time we redraw the panel.
     */
    private BufferedImage currentImage;

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (currentImage == null) return;
        graphics.drawImage(currentImage, 0, 0, getWidth(), getHeight(), this);

        // Draw grid lines
        if(showGridLines) {
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

    public void setShowGridLines(boolean showGridLines) {
        this.showGridLines = showGridLines;
        repaint();
    }

    public void onImageUpdate(ImageSource source) {
        currentImage = source.getCurrentImage();
        repaint();
    }

    public void onResize(ImageSource source) {
        repaint();
    }
}
