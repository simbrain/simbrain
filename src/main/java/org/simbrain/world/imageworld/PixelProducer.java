package org.simbrain.world.imageworld;

import org.simbrain.util.ResourceManager;
import org.simbrain.world.imageworld.gui.ImagePanel;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * The default "Image World" which allows images to be filtered by sensor
 * matrices and the resulting vectors to be sent to Neural networks and other
 * Simbrain components via couplings.
 */
public class PixelProducer extends ImageWorld {

    /**
     * The object which produces the actual pixels produced.
     */
    private PixelProducerSource imageSource;

    /**
     * Current pen color when drawing on the current image.
     */
    private Color penColor = Color.white;

    /**
     * Construct the image world.
     */
    public PixelProducer() {
        super();
        showGridLines = false;
        imagePanel = new ImagePanel(showGridLines);
        imageSource = new PixelProducerSource();
        imageSource.loadImage(ResourceManager.getImageIcon("imageworld/bobcat.jpg"));
        initializeDefaultSensorMatrices();

        // Ability to paint pixels black and white
        MouseAdapter mouseAdapter = new MouseAdapter() {

            @Override
            public void mouseDragged(MouseEvent evt) {
                drawPixel(evt);
            }

            @Override
            public void mousePressed(MouseEvent evt) {
                drawPixel(evt);
            }
        };
        imagePanel.addMouseListener(mouseAdapter);
        imagePanel.addMouseMotionListener(mouseAdapter);

    }

    /**
     * Draw a pixel at the current point in the image panel.
     */
    private void drawPixel(MouseEvent evt) {
        var ratioX = 1.0 * imagePanel.getWidth() / imageSource.getWidth();
        var ratioY = 1.0 * imagePanel.getHeight() / imageSource.getHeight();
        var x = (int) (evt.getX() / ratioX);
        var y = (int) (evt.getY() / ratioY);

        imageSource.getCurrentImage().setRGB(x, y, penColor.getRGB());
        imageSource.notifyImageUpdate();
    }

    /**
     * Replace the current image with a blank canvas of the indicated size.
     */
    public void createBlankCanvas(int width, int height) {
        imageSource.setCurrentImage(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB));
    }

    @Override
    public boolean getUseColorEmitter() {
        return false;
    }

    /**
     * Load images from an array.
     *
     * @param files array of images to load
     * @throws IOException thrown if the requested file is not available
     */
    public void loadImages(File[] files) {
        imageSource.loadImages(files);
    }

    /**
     * Returns number of frames in the "album" associated with this component.
     */
    public int getNumImages() {
        return imageSource.getNumFrames();
    }

    /**
     * Update the image source to the next image.
     */
    public void nextFrame() {
        imageSource.nextFrame();
    }

    /**
     * Update the image source to the previous image.
     */
    public void previousFrame() {
        imageSource.previousFrame();
    }

    @Override
    public ImageSourceAdapter getImageSource() {
        return imageSource;
    }

    @Override
    public void update() {
    }

    public Color getPenColor() {
        return penColor;
    }

    public void setPenColor(Color penColor) {
        this.penColor = penColor;
    }
}
