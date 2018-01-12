package org.simbrain.world.imageworld;

import java.awt.*;
import java.awt.image.BufferedImage;

import org.simbrain.workspace.Consumable;
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
    private double[][] channels;

    private int[] colors;

    private boolean useColor = true;

    /**
     * Construct a sensor matrix without attaching it to a source.
     * @param name The name of the sensor matrix.
     */
    protected SensorMatrix(String name) {
        this.name = name;
    }

    /**
     * Construct a sensor matrix attached to an ImageSource.
     * @param name The name of the sensor matrix.
     * @param source The source to attach.
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

    protected void setSource(ImageSource source) {
        if (this.source == source) {
            return;
        }
        if (this.source != null) {
            this.source.removeListener(this);
        }
        this.source = source;
        this.source.addListener(this);
    }

    /** @return the width */
    public int getWidth() {
        return source.getWidth();
    }

    /** @return the height */
    public int getHeight() {
        return source.getHeight();
    }

    /** Returns an array of RGB colors encoded in integers. */
    @Producible(idMethod="getName", defaultVisibility=true)
    public int[] getColor() {
        return colors;
    }

    /** Returns an array of doubles which corresponds to the brightness of the pixels. */
    @Producible(idMethod="getName")
    public double[] getBrightness() {
        return channels[0];
    }

    /** @return Returns an array of doubles for the each pixel */
    @Producible(idMethod="getName", defaultVisibility=false)
    public double[] getRed() {
        return channels[1];
    }

    /** @return Returns an array of doubles for the each pixel */
    @Producible(idMethod="getName", defaultVisibility=false)
    public double[] getGreen() {
        return channels[2];
    }

    /** @return Returns an array of doubles for the each pixel */
    @Producible(idMethod="getName", defaultVisibility=false)
    public double[] getBlue() {
        return channels[3];
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
                if (useColor) {
                    colors[y * getWidth() + x] = color;
                } else {
                    double red = ((color >>> 16) & 0xFF) / 255.0;
                    double green = ((color >>> 8) & 0xFF) / 255.0;
                    double blue = (color & 0xFF) / 255.0;
                    channels[0][y * getWidth() + x] = (red * 0.2126 + green * 0.7152 + blue * 0.0722);
                    channels[1][y * getWidth() + x] = red;
                    channels[2][y * getWidth() + x] = green;
                    channels[3][y * getWidth() + x] = blue;
                }
            }
        }
    }

    @Override
    public void onResize(ImageSource source) {
        colors = new int[getWidth() * getHeight()];
        channels = new double[4][getWidth() * getHeight()];
    }
}
