package org.simbrain.world.imageworld;

import java.awt.image.BufferedImage;

import org.simbrain.workspace.Producible;

/**
 * A rectangular matrix of filtered sensors on an ImageSource which
 * can be coupled to.
 * @author Jeff Yoshimi, Tim Shea
 */
public class SensorMatrix implements ImageSourceListener {
    /** Name of this matrix. */
    private String name;

    /** An ImageSource from which to extract sensor values. */
    private ImageSource source;

    /** The vales this matrix produces for couplings. */
    private double[][] sensorValues;

    /**
     * Construct the sensor matrix with a specified name.
     * @param name the name of this sensor matrix
     * @param source the source to couple
     */
    public SensorMatrix(String name, ImageSource source) {
        this.name = name;
        this.source = source;
        source.addListener(this);
    }

    /** @return the name */
    public String getName() {
        return name;
    }

    /** @return the image source this sensor matrix reads */
    public ImageSource getSource() {
        return source;
    }

    /** @return the width */
    public int getWidth() {
        return source.getWidth();
    }

    /** @return the height */
    public int getHeight() {
        return source.getHeight();
    }

    // TODO: The following three methods should be collapsed to a single indexed producer
    /** @return Returns an array of doubles for the each pixel */
    @Producible(customDescriptionMethod = "getName")
    public double[] getChannel1() {
        return sensorValues[0];
    }

    /** @return Returns an array of doubles for the each pixel */
    @Producible(customDescriptionMethod = "getName", visible = false)
    public double[] getChannel2() {
        return sensorValues[1];
    }

    /** @return Returns an array of doubles for the each pixel */
    @Producible(customDescriptionMethod = "getName", visible = false)
    public double[] getChannel3() {
        return sensorValues[2];
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public void onImageUpdate(ImageSource source) {
        updateSensorValues(source.getCurrentImage());
    }

    /**
     * Update the sensor matrix values.
     * @param image the image to copy to the sensor values
     */
    private void updateSensorValues(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); ++y) {
            for (int x = 0; x < image.getWidth(); ++x) {
                int color = image.getRGB(x, y);
                int red = (color >>> 16) & 0xFF;
                int green = (color >>> 8) & 0xFF;
                int blue = color & 0xFF;
                sensorValues[0][y * getWidth() + x] = red / 255.0;
                sensorValues[1][y * getWidth() + x] = green / 255.0;
                sensorValues[2][y * getWidth() + x] = blue / 255.0;
            }
        }
    }

    @Override
    public void onResize(ImageSource source) {
        sensorValues = new double[3][getWidth() * getHeight()];
    }
}
