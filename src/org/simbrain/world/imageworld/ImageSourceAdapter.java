package org.simbrain.world.imageworld;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
    
    private transient List<ImageSourceListener> listeners;

    /**
     * Construct a new ImageSourceAdapter and initialize the current image.
     */
    public ImageSourceAdapter() {
        currentImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        listeners = new CopyOnWriteArrayList<>();
    }

    /**
     * Construct a new ImageSourceAdapter with the specified currentImage.
     *
     * @param currentImage the image to provide from the source
     */
    public ImageSourceAdapter(BufferedImage currentImage) {
        this.currentImage = currentImage;
        listeners = new CopyOnWriteArrayList<>();
    }

    /**
     * Return a deserialized ImageSourceAdapter.
     */
    public Object readResolve() {
        listeners = new CopyOnWriteArrayList<>();
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
    protected void notifyImageUpdate() {
        if (isEnabled()) {
            for (ImageSourceListener listener : listeners) {
                listener.onImageUpdate(this);
            }
        }
    }

    @Override
    public BufferedImage getCurrentImage() {
        return currentImage;
    }

    /**
     * @param value The image to assign to the current image.
     */
    protected void setCurrentImage(BufferedImage value) {
        boolean resized = value.getWidth() != currentImage.getWidth() || value.getHeight() != currentImage.getHeight();
        currentImage = value;
        if (resized) {
            notifyResize();
        }
        notifyImageUpdate();
    }

    @Override
    public void addListener(ImageSourceListener listener) {
        listeners.add(listener);
        listener.onResize(this);
        listener.onImageUpdate(this);
    }

    @Override
    public void removeListener(ImageSourceListener listener) {
        listeners.remove(listener);
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
    protected void notifyResize() {
        if (isEnabled()) {
            for (ImageSourceListener listener : listeners) {
                listener.onResize(this);
            }
        }
    }
}
