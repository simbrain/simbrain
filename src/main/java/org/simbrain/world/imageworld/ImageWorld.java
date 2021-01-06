package org.simbrain.world.imageworld;

import org.simbrain.util.ResourceManager;
import org.simbrain.world.imageworld.filters.FilterSelector;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * The default "Image World" which allows images to be filtered by sensor
 * matrices and the resulting vectors to be sent to Neural networks and other
 * Simbrain components via couplings.
 */
public class ImageWorld {

    /**
     * Contains the current image rendered here.
     */
    private ImageAlbum imageAlbum;

    /**
     * List of filters.
     */
    private FilterSelector filterSelector;

    /**
     * Construct the image world.
     */
    public ImageWorld() {
        super();

        // Image Album
        imageAlbum = new ImageAlbum();
        imageAlbum.loadImage(ResourceManager.getImageIcon("imageworld/bobcat.jpg"));

        // Filter Selector
        filterSelector = new FilterSelector(imageAlbum);

    }

    /**
     * Replace the current image with a blank canvas of the indicated size.
     */
    public void createBlankCanvas(int width, int height) {
        imageAlbum.setCurrentImage(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB));
    }

    /**
     * Load images from an array.
     *
     * @param files array of images to load
     * @throws IOException thrown if the requested file is not available
     */
    public void loadImages(File[] files) {
        imageAlbum.loadImages(files);
    }

    /**
     * Returns number of frames in the "album" associated with this component.
     */
    public int getNumImages() {
        return imageAlbum.getNumFrames();
    }

    /**
     * Update the image source to the next image.
     */
    public void nextFrame() {
        imageAlbum.nextFrame();
    }

    /**
     * Update the image source to the previous image.
     */
    public void previousFrame() {
        imageAlbum.previousFrame();
    }

    public ImageAlbum getImageAlbum() {
        return imageAlbum;
    }

    public FilterSelector getFilterSelector() {
        return filterSelector;
    }
}
