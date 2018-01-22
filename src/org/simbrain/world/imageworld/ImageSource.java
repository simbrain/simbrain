package org.simbrain.world.imageworld;

import java.awt.image.BufferedImage;

/**
 * ImageSource produces BufferedImages periodically and notifies listeners of new
 * images or changes to the image size.
 *
 * @author Tim Shea
 */
public interface ImageSource {
    /**
     * @return Returns whether the source will update the image when updateImage
     * is invoked.
     */
    boolean isEnabled();

    /**
     * @param value Assign whether the source should update the image.
     */
    void setEnabled(boolean value);

    /**
     * @return Returns the current image.
     */
    BufferedImage getCurrentImage();

    /**
     * Add a listener to be notified of new images and resizes.
     * @param listener The listener to add.
     */
    void addListener(ImageSourceListener listener);

    /**
     * Remove a listener to stop being notified.
     * @param listener The listener to remove.
     */
    void removeListener(ImageSourceListener listener);

    /**
     * @return Returns the width of the images produced by the source.
     */
    int getWidth();

    /**
     * @return Returns the height of the images produced by the source.
     */
    int getHeight();
}
