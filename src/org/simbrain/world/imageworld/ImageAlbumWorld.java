package org.simbrain.world.imageworld;

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
    private ImageAlbum imageSource;

    /**
     * Construct the image world.
     */
    public ImageAlbumWorld() {
        super();
        imagePanel = new ImagePanel(false);
        imageSource = new ImageAlbum();
        imageSource.loadImage(org.simbrain.resource.ResourceManager.getImageIcon("bobcat.jpg"));

        initializeDefaultSensorMatrices();
    }

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
