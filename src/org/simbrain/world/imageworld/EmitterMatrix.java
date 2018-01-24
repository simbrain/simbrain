package org.simbrain.world.imageworld;

import java.awt.image.BufferedImage;
import java.util.Arrays;

import org.simbrain.util.UserParameter;
import org.simbrain.workspace.Consumable;

public class EmitterMatrix extends ImageSourceAdapter {
    @UserParameter(label="Use RGB Colors", description="Sets whether to couple integer array of RGB colors or" +
            "separate red, green, and blue channels.")
    private boolean usingRGBColor = false;
    private double[][] channels;
    private int[] colors;

    public EmitterMatrix() {
        super();
        channels = new double[3][getWidth() * getHeight()];
        colors = new int[getWidth() * getHeight()];
    }

    public EmitterMatrix(BufferedImage currentImage) {
        super(currentImage);
        channels = new double[3][getWidth() * getHeight()];
        colors = new int[getWidth() * getHeight()];
    }

    /** Returns whether the emitter matrix should use int RGB colors or double channels. */
    public boolean isUsingRGBColor() {
        return usingRGBColor;
    }

    /**
     * Set whether the emitter matrix should use int RGB color values (true) or double channel values (false).
     * Note that the set brightness coupling requires useColor = false.
     */
    public void setUsingRGBColor(boolean value) {
        usingRGBColor = value;
    }

    @Consumable
    public void setBrightness(double[] values) {
        int length = Math.min(values.length, getWidth() * getHeight());
        System.arraycopy(values, 0, channels[0], 0, length);
        System.arraycopy(values, 0, channels[1], 0, length);
        System.arraycopy(values, 0, channels[2], 0, length);
    }

    @Consumable
    public void setRGBColor(int[] values) {
        int length = Math.min(values.length, getWidth() * getHeight());
        System.arraycopy(values, 0, colors, 0, length);
    }

    @Consumable(defaultVisibility=false)
    public void setRed(double[] values) {
        int length = Math.min(values.length, getWidth() * getHeight());
        System.arraycopy(values, 0, channels[0], 0, length);
    }

    @Consumable(defaultVisibility=false)
    public void setGreen(double[] values) {
        int length = Math.min(values.length, getWidth() * getHeight());
        System.arraycopy(values, 0, channels[1], 0, length);
    }

    @Consumable(defaultVisibility=false)
    public void setBlue(double[] values) {
        int length = Math.min(values.length, getWidth() * getHeight());
        System.arraycopy(values, 0, channels[2], 0, length);
    }

    public void setSize(int width, int height) {
        setCurrentImage(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB));
        channels = new double[3][getWidth() * getHeight()];
        colors = new int[getWidth() * getHeight()];
    }

    /**
     * Update the emitter matrix image from the couplable arrays.
     *
     * If the emitter matrix is using color ints, then the new image will simply copy the coupled integer array
     * to the current image.
     *
     * If the emitter matrix is using double channels, then each channel of values (0.0 to 1.0) will be translated
     * to integers (0 to 255) and assigned to the corresponding pixels.
     */
    public void emitImage() {
        if (usingRGBColor) {
            BufferedImage image = getCurrentImage();
            for (int y = 0; y < image.getHeight(); ++y) {
                for (int x = 0; x < image.getWidth(); ++x) {
                    int rgb = colors[y * getWidth() + x];
                    image.setRGB(x, y, rgb);
                }
            }
        } else {
            BufferedImage image = getCurrentImage();
            for (int y = 0; y < image.getHeight(); ++y) {
                for (int x = 0; x < image.getWidth(); ++x) {
                    int red = (int) (channels[0][y * getWidth() + x] * 255.0);
                    red = Math.max(Math.min(red, 255), 0) << 16;
                    int blue = (int) (channels[1][y * getWidth() + x] * 255.0);
                    blue = Math.max(Math.min(blue, 255), 0) << 8;
                    int green = (int) (channels[2][y * getWidth() + x] * 255.0);
                    green = Math.max(Math.min(green, 255), 0);
                    int color = red + blue + green;
                    image.setRGB(x, y, color);
                }
            }
        }
        notifyImageUpdate();
    }

    public void clear() {
        Arrays.fill(channels[0], 0.0);
        Arrays.fill(channels[1], 0.0);
        Arrays.fill(channels[2], 0.0);
        Arrays.fill(colors, 0);
    }

    @Override
    public String toString() {
        return "EmitterMatrix";
    }
}
