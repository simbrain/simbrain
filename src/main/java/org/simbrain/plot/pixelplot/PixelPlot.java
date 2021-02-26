package org.simbrain.plot.pixelplot;

/**
 * The "Pixel display" component which allows data to be received from Neural networks
 * and other Simbrain components via couplings and rendered in a Buffered "pixel" image.
 */
public class PixelPlot {

    /**
     * The BufferedImage that displays whatever pixel pattern is currently being
     * received from other Simbrain components via couplings.
     */
    public EmitterMatrix emitterMatrix; // TODO: Public

    /**
     * Construct the image world.
     *
     */
    public PixelPlot() {
        super();
        // TODO
        // getImagePanel().setShowGridLines(true);
        emitterMatrix = new EmitterMatrix();
    }

    // public void clearImage() {
    //     emitterMatrix.clear();
    //     emitterMatrix.emitImage();
    // }

    public void setUseColorEmitter(boolean value) {
        emitterMatrix.setUsingRGBColor(value);
    }

    public int getEmitterWidth() {
        return emitterMatrix.getWidth();
    }

    public int getEmitterHeight() {
        return emitterMatrix.getHeight();
    }

    /**
     * Set the size of the emitter matrix.
     */
    public void resizeEmitterMatrix(int width, int height) {
        emitterMatrix.setSize(width, height);
    }

    /**
     * Update the emitter matrix image.
     */
    public void update() {
        emitterMatrix.emitImage();
    }

}
