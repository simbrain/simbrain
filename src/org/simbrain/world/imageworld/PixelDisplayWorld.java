package org.simbrain.world.imageworld;

public class PixelDisplayWorld extends ImageWorld {


    private EmitterMatrix imageSource;

    /**
     * Construct the image world.
     *
     */
    public PixelDisplayWorld() {
        super();

        imageSource = new  EmitterMatrix();

        initializeDefaultSensorMatrices();
    }

    @Override
    public void clearImage() {
        imageSource.clear();
        imageSource.emitImage();
    }

    @Override
    public boolean getUseColorEmitter() {
        return imageSource.isUsingRGBColor();
    }


    /**
     * Set the color mode of the emitter matrix.
     */
    public void setUseColorEmitter(boolean value) {
        imageSource.setUsingRGBColor(value);
    }

    /**
     * Get the width of the emitter matrix.
     */
    public int getEmitterWidth() {
        return imageSource.getWidth();
    }

    /**
     * Get the height of the emitter matrix.
     */
    public int getEmitterHeight() {
        return imageSource.getHeight();
    }

    /**
     * Set the size of the emitter matrix.
     */
    public void resizeEmitterMatrix(int width, int height) {
        imageSource.setSize(width, height);
    }

    /**
     * Update the emitter matrix image.
     */
    public void update() {
        imageSource.emitImage();
    }

    @Override
    public ImageSourceAdapter getImageSource() {
        return imageSource;
    }
}
