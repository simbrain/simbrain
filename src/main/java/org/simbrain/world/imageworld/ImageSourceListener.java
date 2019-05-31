package org.simbrain.world.imageworld;

/**
 * ImageSourceListener allows an object to be notified when a source
 * of images produces a new image or is resized.
 *
 * @author Tim Shea
 */
public interface ImageSourceListener {

    /**
     * Called by an ImageSource when a new image is produced or the
     * image is updated.
     *
     * @param source The image source that produced a new image.
     */
    void onImageUpdate(ImageSource source);

    /**
     * Called by the ImageSource when the source is resized.
     *
     * @param source the resized source.
     */
    void onResize(ImageSource source);
}
