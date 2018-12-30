package org.simbrain.world.imageworld;

import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Producible;

import java.awt.image.BufferedImage;

/**
 * A rectangular matrix of filtered sensors on an {@link ImageSource} which
 * can be coupled to.  Arrays tracking int rgb colors and doubles for
 * brightness, red, green, and blue separately are maintained and can serve
 * as producers for couplings.
 * <br>
 * The actual filtering happens in the {@link org.simbrain.world.imageworld.filters}
 * package. Sensor matrices do the work of allowing the filtered images to
 * couple to something else.  This makes sense biologically: retinal patterns
 * are what neurons "sense".
 *
 * @author Tim Shea
 * @author Jeff Yoshimi
 */
public class SensorMatrix implements ImageSourceListener, AttributeContainer {

    /**
     * Name of this matrix.
     */
    private String name;

    /**
     * An ImageSource from which to extract sensor values.
     */
    private ImageSource source;

    /**
     * The values this matrix produces for floating point channel couplings.
     * Four copies of the (flattened) matrix are stored for brightness (0),
     * red (1), green (2), and blue (3).
     * See {@link #updateSensorValues(BufferedImage)}.
     */
    private transient double[][] channels;

    /**
     * Array of ints representing rgb colors. See
     * {@link BufferedImage#getRGB(int, int)}
     */
    private transient int[] rgbColors;

    /**
     * Construct a sensor matrix without attaching it to a source.
     * Currently used by 3d component.
     *
     * @param name The name of the sensor matrix.
     */
    protected SensorMatrix(String name) {
        this.name = name;
    }

    /**
     * Construct a sensor matrix attached to an ImageSource.
     *
     * @param name   The name of the sensor matrix.
     * @param source The source to attach.
     */
    public SensorMatrix(String name, ImageSource source) {
        this.name = name;
        this.source = source;
        source.addListener(this);
    }

    public Object readResolve() {
        source.addListener(this);
        onResize(source);
        return this;
    }

    public String getName() {
        return name;
    }

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

    public int getWidth() {
        return source.getWidth();
    }

    public int getHeight() {
        return source.getHeight();
    }

    @Producible(idMethod = "getName")
    public double[] getBrightness() {
        return channels[0];
    }

    @Producible(idMethod = "getName", defaultVisibility = false)
    public double[] getRed() {
        return channels[1];
    }

    @Producible(idMethod = "getName", defaultVisibility = false)
    public double[] getGreen() {
        return channels[2];
    }

    @Producible(idMethod = "getName", defaultVisibility = false)
    public double[] getBlue() {
        return channels[3];
    }

    @Producible(idMethod = "getName")
    public int[] getRGBColor() {
        return rgbColors;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public void onImageUpdate(ImageSource source) {
        updateSensorValues(source.getCurrentImage());
    }

    @Override
    public void onResize(ImageSource source) {
        channels = new double[4][getWidth() * getHeight()];
        rgbColors = new int[getWidth() * getHeight()];
    }

    /**
     * Update the sensor matrix values.
     *
     * @param image the image to copy to the sensor values
     */
    private void updateSensorValues(BufferedImage image) {
        if (image.getHeight() != getHeight() || image.getWidth() != getWidth()) {
            throw new AssertionError();
        }

        for (int y = 0; y < image.getHeight(); ++y) {
            for (int x = 0; x < image.getWidth(); ++x) {
                int color = image.getRGB(x, y);

                // Update rgb colors
                rgbColors[y * getWidth() + x] = color;

                // Update other color channels
                // Cf https://stackoverflow.com/questions/2534116/how-to-convert-get-rgbx-y-integer-pixel-to-colorr-g-b-a-in-java
                double red = ((color >>> 16) & 0xFF) / 255.0;
                double green = ((color >>> 8) & 0xFF) / 255.0;
                double blue = (color & 0xFF) / 255.0;
                // Cf. https://en.wikipedia.org/wiki/Luma_(video)
                channels[0][y * getWidth() + x] = (red * 0.2126 + green * 0.7152 + blue * 0.0722);
                channels[1][y * getWidth() + x] = red;
                channels[2][y * getWidth() + x] = green;
                channels[3][y * getWidth() + x] = blue;
            }
        }
    }

}
