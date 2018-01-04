package org.simbrain.world.imageworld;

import java.awt.image.BufferedImage;

import org.simbrain.workspace.Consumible;

public class EmitterMatrix extends ImageSourceAdapter {
    private double[][] emitterValues;

    public EmitterMatrix() {
        super();
        emitterValues = new double[3][getWidth() * getHeight()];
    }

    public EmitterMatrix(BufferedImage currentImage) {
        super(currentImage);
        emitterValues = new double[3][getWidth() * getHeight()];
    }

    @Consumible
    public void setChannel1(double[] values) {
        System.arraycopy(values, 0, emitterValues[0], 0, values.length);
        emitImage();
    }

    @Consumible
    public void setChannel2(double[] values) {
        System.arraycopy(values, 0, emitterValues[1], 0, values.length);
        emitImage();
    }

    @Consumible
    public void setChannel3(double[] values) {
        System.arraycopy(values, 0, emitterValues[2], 0, values.length);
        emitImage();
    }

    public void setSize(int width, int height) {
        setCurrentImage(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB));
        emitterValues = new double[3][getWidth() * getHeight()];
    }

    private void emitImage() {
        BufferedImage image = getCurrentImage();
        for (int y = 0; y < image.getHeight(); ++y) {
            for (int x = 0; x < image.getWidth(); ++x) {
                int red = (int) (emitterValues[0][y * getWidth() + x] * 255.0);
                red = Math.max(Math.min(red, 255), 0) << 16;
                int blue = (int) (emitterValues[1][y * getWidth() + x] * 255.0);
                blue = Math.max(Math.min(blue, 255), 0) << 8;
                int green = (int) (emitterValues[2][y * getWidth() + x] * 255.0);
                green = Math.max(Math.min(green, 255), 0);
                int color = red + blue + green;
                image.setRGB(x, y, color);
            }
        }
        notifyImageUpdate();
    }
}
