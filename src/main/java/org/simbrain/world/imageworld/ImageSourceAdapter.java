package org.simbrain.world.imageworld;

import org.simbrain.world.imageworld.events.ImageSourceEvents;

import java.awt.image.BufferedImage;

/**
 * Abstract helper class which provides basic listener and image management for
 * an {@link ImageSource}
 * <p>
 * Whenever the private current image is updated, the adapter compares it to previous
 * image and decides which events need to be called.
 *
 * @author Tim Shea
 */
public abstract class ImageSourceAdapter implements ImageSource {

    /**
     * See {@link ImageSource}.
     */
    private boolean enabled = true;

    /**
     * Default buffered image to work with.
     */
    private BufferedImage currentImage;

    /**
     * Handle Image source Events.
     */
    private transient ImageSourceEvents events = new ImageSourceEvents(this);

    /**
     * Construct a new ImageSourceAdapter and initialize the current image.
     */
    public ImageSourceAdapter() {
        currentImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        getEvents().fireImageUpdate(this);
    }

    /**
     * Construct a new ImageSourceAdapter with the specified currentImage.
     *
     * @param currentImage the image to provide from the source
     */
    public ImageSourceAdapter(BufferedImage currentImage) {
        this.currentImage = currentImage;
    }

    /**
     * Return a deserialized ImageSourceAdapter.
     */
    public Object readResolve() {
        events = new ImageSourceEvents(this);
        return this;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean value) {
        enabled = value;
    }

    /**
     * Notify ImageSourceListeners that a new image is available.
     */
    public void notifyImageUpdate() {
        if (isEnabled()) {
            events.fireImageUpdate(this);
        }
    }

    @Override
    public BufferedImage getCurrentImage() {
        return currentImage;
    }

    /**
     * @param image The image to assign to the current image.
     */
    protected void setCurrentImage(BufferedImage image) {
        boolean resized = image.getWidth() != currentImage.getWidth() || image.getHeight() != currentImage.getHeight();
        currentImage = image;
        if (resized) {
            notifyResize();
        }
        notifyImageUpdate();
    }

    @Override
    public int getWidth() {
        return currentImage.getWidth();
    }

    @Override
    public int getHeight() {
        return currentImage.getHeight();
    }

    /**
     * Notify ImageSourceListeners that a resize has occurred.
     */
    public void notifyResize() {
        if (isEnabled()) {
            events.fireImageResize(this);
        }
    }

    public ImageSourceEvents getEvents() {
        return events;
    }
}
