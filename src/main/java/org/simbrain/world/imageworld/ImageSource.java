package org.simbrain.world.imageworld;

import org.simbrain.util.ImageUtilsKt;
import org.simbrain.world.imageworld.events.ImageEvents;

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
    private transient ImageEvents events = new ImageEvents();

    /**
     * Construct a new ImageSourceAdapter and initialize the current image.
     */
    public ImageSource() {
        currentImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        getEvents().getImageUpdate().fireAndForget();
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
     * See {@link org.simbrain.workspace.serialization.WorkspaceComponentDeserializer}
     */
    public Object readResolve() {
        events = new ImageEvents();
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
            events.getImageUpdate().fireAndForget();
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
                events.getResize().fireAndForget();
            }
            fireImageUpdate();
        }
    }

    /**
     * @param image The image to assign to the current image.
     */
    protected void setCurrentImage(BufferedImage image) {
        setCurrentImage(ImageUtilsKt.copy(image), true);
    }

    public int getWidth() {
        return currentImage.getWidth();
    }

    public int getHeight() {
        return currentImage.getHeight();
    }

    public ImageEvents getEvents() {
        return events;
    }
}
