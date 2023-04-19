package org.simbrain.world.imageworld;

import org.simbrain.util.ResourceManager;
import org.simbrain.world.imageworld.filters.Filter;
import org.simbrain.world.imageworld.filters.FilterCollection;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 *
 * At each update, apply all the filters in a {@link FilterCollection} to the current image in an {@link ImageAlbum}
 *
 * Display the result of the current filter applied to the current image  to the screen.
 */
public class ImageWorld {

    /**
     * Contains the current image rendered here.
     */
    private ImageAlbum imageAlbum;

    /**
     * List of filters.
     */
    private FilterCollection filterCollection;

    /**
     * Construct the image world.
     */
    public ImageWorld() {
        super();

        // Image Album
        imageAlbum = new ImageAlbum();
        imageAlbum.addImage(ResourceManager.getBufferedImage("imageworld/bobcat.jpg"));

        // Filter Selector
        filterCollection = new FilterCollection(imageAlbum);

    }

    /**
     * Clear the image album and set the current image with a blank canvas of the indicated size.
     */
    public void resetImageAlbum(int width, int height) {
        imageAlbum.reset(width, height);
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

    public FilterCollection getFilterCollection() {
        return filterCollection;
    }

    /**
     * Convenience method to get current filter.
     */
    public Filter getCurrentFilter() {
        return filterCollection.getCurrentFilter();
    }

    /**
     * Convenience method to set current filter on collection.
     */
    public void setCurrentFilter(String name) {
        filterCollection.getFilters().stream()
                .filter(f -> f.getName().equals(name)).findAny()
                .ifPresent(f -> filterCollection.setCurrentFilter(f));
    }

    /**
     * Convenience method to get current image.
     */
    public BufferedImage getCurrentImage() {
        return imageAlbum.getCurrentImage();
    }

}
