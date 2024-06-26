package org.simbrain.plot.pixelplot;

import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;
import org.simbrain.world.imageworld.events.ImageEvents;
import org.simbrain.world.imageworld.filters.Filter;

import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * Contains several arrays which can consume
 * array values and then "emit" them as a kind of pixel display.  The consumed
 * values are stored in two arrays, one for rgb colors and
 * another with separate channels for brightness, red, green, and blue.
 * <br>
 * An pixel plot is a kind of countepart to a {@link Filter}. Rather
 * than sensing visual locations emitters are like pixels that an "organism" can emit
 * cf. a squid's iridiphores.
 *
 * @author Tim Shea
 * @author Jeff Yoshimi
 */
public class PixelPlot implements AttributeContainer, EditableObject {

    /**
     * Image rendered from provided values;
     */
    private BufferedImage image;

    @UserParameter(label = "Invert brightness", description = "If true, 0 is mapped to white and 1 to white.", useLegacySetter = true)
    private boolean invertBrightness = true;

    /**
     * The values this matrix produces for floating point channel couplings.
     * Four copies of the (flattened) matrix are stored for brightness (0),
     * red (1), green (2), and blue (3).
     * See {@link #emitImage()}.
     */
    private double[][] channels;

    /**
     * Handle Image source Events.
     */
    private transient ImageEvents events = new ImageEvents();

    /**
     * Construct an empty pixel plot.
     */
    public PixelPlot() {
        image =  new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        channels = new double[3][image.getWidth() * image.getHeight()];
        clear();
    }

    private int resizeToFit(double[] values) {
        int length = (int) Math.ceil(Math.sqrt(values.length));
        if (image.getWidth() != length || image.getHeight() != length) {
            setSize(length, length);
        }
        return length;
    }

    @Consumable
    public void setBrightness(double[] values) {
        resizeToFit(values);
        if (invertBrightness) {
            for (int i = 0; i < values.length; i++) {
                values[i] = 1 - values[i];
            }
        }
        System.arraycopy(values, 0, channels[0], 0, values.length);
        System.arraycopy(values, 0, channels[1], 0, values.length);
        System.arraycopy(values, 0, channels[2], 0, values.length);
        emitImage();
    }

    @Consumable()
    public void setRed(double[] values) {
        resizeToFit(values);
        System.arraycopy(values, 0, channels[0], 0, values.length);
        emitImage();
    }

    @Consumable()
    public void setGreen(double[] values) {
        resizeToFit(values);
        System.arraycopy(values, 0, channels[1], 0, values.length);
        emitImage();
    }

    @Consumable()
    public void setBlue(double[] values) {
        resizeToFit(values);
        System.arraycopy(values, 0, channels[2], 0, values.length);
        emitImage();
    }

    public void setSize(int width, int height) {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        channels = new double[3][image.getWidth() * image.getHeight()];
        emitImage();
    }

    /**
     * Clears the image and sets it to black.
     */
    public void clear() {
        clearData();
        emitImage();
    }

    public void clearData() {
        Arrays.fill(channels[0], 0.0);
        Arrays.fill(channels[1], 0.0);
        Arrays.fill(channels[2], 0.0);
    }

    /**
     * Update the pixel plot image from the couplable arrays.
     * <p>
     * If the pixel plot is using color ints, then the new image will simply copy the coupled integer array
     * to the current image.
     * <p>
     * If the pixel plot is using double channels, then each channel of values (0.0 to 1.0) will be translated
     * to integers (0 to 255) and assigned to the corresponding pixels.
     */
    public void emitImage() {
        for (int y = 0; y < image.getHeight(); ++y) {
            for (int x = 0; x < image.getWidth(); ++x) {
                int red = getChannelValue(0, x, y);
                red = Math.max(Math.min(red, 255), 0) << 16;
                int blue = getChannelValue(1, x, y);
                blue = Math.max(Math.min(blue, 255), 0) << 8;
                int green = getChannelValue(2, x, y);
                green = Math.max(Math.min(green, 255), 0);
                int color = red + blue + green;
                image.setRGB(x, y, color);
            }
        }
        events.getImageUpdate().fire();
    }

    private int getChannelValue(int channelIndex, int x, int y) {
        return (int) (channels[channelIndex][x + y * image.getWidth()] * 255.0);
    }


    public BufferedImage getImage() {
        return image;
    }

    @Override
    public String toString() {
        return "PixelPlot";
    }

    @Override
    public String getId() {
        return "Pixel Plot";
    }

    public ImageEvents getEvents() {
        return events;
    }

    /**
     * See {@link org.simbrain.workspace.serialization.WorkspaceComponentDeserializer}
     */
    public Object readResolve() {
        events = new ImageEvents();
        return this;
    }

    /**
     * Property editor calls this by reflection to re-render the plot when inversion is changed
     */
    public void setInvertBrightness(boolean invertBrightness) {
        this.invertBrightness = invertBrightness;
        emitImage();
    }
}
