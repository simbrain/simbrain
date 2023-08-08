package org.simbrain.plot.pixelplot;

import org.simbrain.util.UserParameter;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;
import org.simbrain.world.imageworld.events.ImageEvents2;
import org.simbrain.world.imageworld.filters.Filter;

import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * Contains several arrays which can consume
 * array values and then "emit" them as a kind of pixel display.  The consumed
 * values are stored in two arrays, one for rgb colors and
 * another with separate channels for brightness, red, green, and blue.
 * <br>
 * An emitter matrix is a kind of countepart to a {@link Filter}. Rather
 * than sensing visual locations emitters are like pixels that an "organism" can emit
 * cf. a squid's iridiphores.
 *
 * @author Tim Shea
 * @author Jeff Yoshimi
 */
public class EmitterMatrix implements AttributeContainer {

    /**
     * Image rendered from provided values;
     */
    private BufferedImage image;

    @UserParameter(label = "Use RGB Colors", description = "Sets whether to couple integer array of RGB colors or" + "separate red, green, and blue channels.")
    private boolean usingRGBColor = false;

    /**
     * The values this matrix produces for floating point channel couplings.
     * Four copies of the (flattened) matrix are stored for brightness (0),
     * red (1), green (2), and blue (3).
     * See {@link #emitImage()}.
     */
    private double[][] channels;

    /**
     * Array of ints representing rgb colors. See
     * {@link BufferedImage#getRGB(int, int)}
     */
    private int[] rgbColors;

    /**
     * Handle Image source Events.
     */
    private transient ImageEvents2 events = new ImageEvents2();

    /**
     * Construct an empty emitter matrix.
     */
    public EmitterMatrix() {
        image =  new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        channels = new double[3][image.getWidth() * image.getHeight()];
        rgbColors = new int[image.getWidth() * image.getHeight()];
    }

    /**
     * Construct an emitter matrix from an image.
     *
     * @param currentImage
     */
    public EmitterMatrix(BufferedImage currentImage) {
        channels = new double[3][image.getWidth() * image.getHeight()];
        rgbColors = new int[image.getWidth() * image.getHeight()];
    }

    /**
     * Returns whether the emitter matrix should use int RGB colors or double channels.
     */
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
        int length = (int) Math.ceil(Math.sqrt(values.length));
        if (image.getWidth() != length || image.getHeight() != length) {
            setSize(length, length);
        }
        System.arraycopy(values, 0, channels[0], 0, values.length);
        System.arraycopy(values, 0, channels[1], 0, values.length);
        System.arraycopy(values, 0, channels[2], 0, values.length);
        emitImage();
    }

    @Consumable
    public void setRGBColor(int[] values) {
        int length = Math.min(values.length, image.getWidth() * image.getHeight());
        System.arraycopy(values, 0, rgbColors, 0, length);
        emitImage();
    }

    @Consumable(defaultVisibility = false)
    public void setRed(double[] values) {
        int length = Math.min(values.length, image.getWidth() * image.getHeight());
        System.arraycopy(values, 0, channels[0], 0, length);
        emitImage();
    }

    @Consumable(defaultVisibility = false)
    public void setGreen(double[] values) {
        int length = Math.min(values.length, image.getWidth() * image.getHeight());
        System.arraycopy(values, 0, channels[1], 0, length);
        emitImage();
    }

    @Consumable(defaultVisibility = false)
    public void setBlue(double[] values) {
        int length = Math.min(values.length, image.getWidth() * image.getHeight());
        System.arraycopy(values, 0, channels[2], 0, length);
        emitImage();
    }

    public void setSize(int width, int height) {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        channels = new double[3][image.getWidth() * image.getHeight()];
        rgbColors = new int[image.getWidth() * image.getHeight()];
        emitImage();
    }

    /**
     * Clears the image and sets it to black.
     */
    public void clear() {
        Arrays.fill(channels[0], 0.0);
        Arrays.fill(channels[1], 0.0);
        Arrays.fill(channels[2], 0.0);
        Arrays.fill(rgbColors, 0);
        emitImage();
    }

    /**
     * Update the emitter matrix image from the couplable arrays.
     * <p>
     * If the emitter matrix is using color ints, then the new image will simply copy the coupled integer array
     * to the current image.
     * <p>
     * If the emitter matrix is using double channels, then each channel of values (0.0 to 1.0) will be translated
     * to integers (0 to 255) and assigned to the corresponding pixels.
     */
    public void emitImage() {
        if (usingRGBColor) {
            for (int y = 0; y < image.getHeight(); ++y) {
                for (int x = 0; x < image.getWidth(); ++x) {
                    int rgb = rgbColors[y * image.getWidth() + x];
                    image.setRGB(x, y, rgb);
                }
            }
        } else {
            for (int y = 0; y < image.getHeight(); ++y) {
                for (int x = 0; x < image.getWidth(); ++x) {
                    int red = (int) (channels[0][y * image.getWidth() + x] * 255.0);
                    red = Math.max(Math.min(red, 255), 0) << 16;
                    int blue = (int) (channels[1][y * image.getWidth() + x] * 255.0);
                    blue = Math.max(Math.min(blue, 255), 0) << 8;
                    int green = (int) (channels[2][y * image.getWidth() + x] * 255.0);
                    green = Math.max(Math.min(green, 255), 0);
                    int color = red + blue + green;
                    image.setRGB(x, y, color);
                }
            }
        }
        events.getImageUpdate().fireAndForget();
    }

    public BufferedImage getImage() {
        return image;
    }

    @Override
    public String toString() {
        return "EmitterMatrix";
    }

    @Override
    public String getId() {
        return "Emitter Matrix";
    }

    public ImageEvents2 getEvents() {
        return events;
    }

    /**
     * See {@link org.simbrain.workspace.serialization.WorkspaceComponentDeserializer}
     */
    public Object readResolve() {
        events = new ImageEvents2();
        return this;
    }
}
