package org.simbrain.world.imageworld;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Display images from an ImageSource.
 *
 * @author Tim Shea
 * @author Jeff Yoshimi
 */
public class ImagePanel extends JPanel implements ImageSourceListener {

    private static final long serialVersionUID = 1L;

    /**
     * Reference to last image provided, so we don't to have reload the image
     * every time we redraw the panel.
     */
    private BufferedImage currentImage;

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        graphics.drawImage(currentImage, 0, 0, getWidth(), getHeight(), this);
    }

    @Override
    public void onImageUpdate(ImageSource source) {
        currentImage = source.getCurrentImage();
        repaint();
    }

    @Override
    public void onResize(ImageSource source) {
        repaint();
    }
}
