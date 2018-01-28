package org.simbrain.world.imageworld;

public class CompositeImageSource extends ImageSourceAdapter implements ImageSourceListener {
    private ImageSource selectedSource;

    public CompositeImageSource(ImageSource selectedSource) {
        super();
        selectedSource.addListener(this);
        this.selectedSource = selectedSource;
    }

    public Object readResolve() {
        super.readResolve();
        selectedSource.addListener(this);
        return this;
    }

    /**
     * Get the current ImageSource used by the composite.
     */
    public ImageSource getImageSource() {
        return selectedSource;
    }

    /**
     * Set the ImageSource to pass through the composite.
     */
    public void setImageSource(ImageSource source) {
        if (selectedSource != source) {
            selectedSource.removeListener(this);
            source.addListener(this);
            selectedSource = source;
        }
    }

    @Override
    public void onImageUpdate(ImageSource source) {
        setCurrentImage(source.getCurrentImage());
    }

    @Override
    public void onResize(ImageSource source) {
        setCurrentImage(source.getCurrentImage());
    }
}
