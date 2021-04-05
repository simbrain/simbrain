package org.simbrain.world.imageworld;

import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.world.imageworld.events.ImageSourceEvents;

import java.awt.image.BufferedImage;

/**
 * Produces BufferedImages periodically and notifies listeners when the image changes or is resized.
 * <br>
 * Image sources can be enabled or disabled. E.g. if a webcam is available it can enable its image source, and then
 * when it is turned off the image source can be disabled (however this is not currently used and has not been tested).
 *
 * Whenever the current image is updated, the adapter compares it to previous image and decides which events need to
 * be called.
 *
 * @author Tim Shea
 */
public abstract class ImageSource  {

    /**
     * Whether the source will update the image when updateImage
     * is invoked.
     */
    private boolean enabled = true;

    /**
     * Image backing the source.
     */
    private BufferedImage currentImage;

    /**
     * Handle Image source Events.
     */
    private transient ImageSourceEvents events = new ImageSourceEvents(this);

    /**
     * Construct a new ImageSourceAdapter and initialize the current image.
     */
    public ImageSource() {
        currentImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        getEvents().fireImageUpdate();
    }

    /**
     * Construct a new ImageSourceAdapter with the specified currentImage.
     *
     * @param currentImage the image to provide from the source
     */
    public ImageSource(BufferedImage currentImage) {
        this.currentImage = currentImage;
    }

    /**
     * Return a deserialized ImageSourceAdapter.
     */
    public Object readResolve() {
        events = new ImageSourceEvents(this);
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
    }

    /**
     * Notify ImageSourceListeners that a new image is available.
     */
    public void fireImageUpdate() {
        if (isEnabled()) {
            events.fireImageUpdate();
        }
    }

    public BufferedImage getCurrentImage() {
        return currentImage;
    }

    /**
     * Set the current image on the source, and optionally fire an updaet event.
     */
    protected void setCurrentImage(BufferedImage image, boolean fireEvents) {
        boolean resized = image.getWidth() != currentImage.getWidth() || image.getHeight() != currentImage.getHeight();
        currentImage = image;
        if (fireEvents) {
            if (resized && isEnabled()) {
                events.fireResize();
            }
            fireImageUpdate();
        }
    }

    /**
     * @param image The image to assign to the current image.
     */
    protected void setCurrentImage(BufferedImage image) {
        setCurrentImage(image, true);
    }

    public int getWidth() {
        return currentImage.getWidth();
    }

    public int getHeight() {
        return currentImage.getHeight();
    }

    public ImageSourceEvents getEvents() {
        return events;
    }
}
