package org.simbrain.world.imageworld;

/**
 * The "Pixel display" compnoent which allows data to be received from Neural networks
 * and other Simbrain components via couplings and rendered in a Buffered "pixel" image.
 */
public class PixelDisplayWorld extends ImageWorld {

    /**
     * The BufferedImage that displays whatever pixel pattern is currently being
     * received from other Simbrain components via couplings.
     */
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

    public void setUseColorEmitter(boolean value) {
        imageSource.setUsingRGBColor(value);
    }

    public int getEmitterWidth() {
        return imageSource.getWidth();
    }

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
