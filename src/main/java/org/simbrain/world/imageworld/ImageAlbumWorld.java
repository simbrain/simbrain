package org.simbrain.world.imageworld;

import org.simbrain.util.ResourceManager;
import org.simbrain.world.imageworld.gui.ImagePanel;

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

        // "Paint" pixel
        imagePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                toggle = toggle == 0xFFFFF ? 0: 0xFFFFF;
                imageSource.getCurrentImage().setRGB(7,7, toggle);
                imageSource.notifyImageUpdate();
            }
        });
    }
    // Temporary to study drawing to screen in image world
    int toggle = 0xFFFFF;


    @Override
    public void clearImage() {
        imageSource.setCurrentImage(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB));
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
