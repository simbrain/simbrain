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
public class ImageAlbumWorld extends ImageWorld {

    /**
     * The object which produces the actual images processed by the "album".
     */
    private ImageAlbumSource imageSource;

    /**
     * Construct the image world.
     */
    public ImageAlbumWorld() {
        super();
        showGridLines = false;
        imagePanel = new ImagePanel(showGridLines);
        imageSource = new ImageAlbumSource();
        imageSource.loadImage(ResourceManager.getImageIcon("imageworld/bobcat.jpg"));
        initializeDefaultSensorMatrices();

        // TODO: Add a button for this and ability to choose size of a blank canvas
        //  That is, instead of loading an image the user can also just create a drawing canvas of a certain size
        clearImage();

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

    private void drawPixel(MouseEvent evt) {
        var ratioX = 1.0 * imagePanel.getWidth() / imageSource.getWidth();
        var ratioY = 1.0 * imagePanel.getHeight() / imageSource.getHeight();
        var x = (int) (evt.getX() / ratioX);
        var y = (int) (evt.getY() / ratioY);

        int currentColor =  imageSource.getCurrentImage().getRGB(x, y);
        int drawColor = -1; // White
        if (currentColor == -1) {
            drawColor = 0; // If white, toggle to black
        }
        imageSource.getCurrentImage().setRGB(x, y, drawColor);
        imageSource.notifyImageUpdate();
    }


    @Override
    public void clearImage() {
        imageSource.setCurrentImage(new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB));
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
     * Returns number of frames in album
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

}
