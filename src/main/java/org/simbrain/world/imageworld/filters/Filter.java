package org.simbrain.world.imageworld.filters;

import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Producible;
import org.simbrain.world.imageworld.ImageSource;

import java.awt.image.BufferedImage;

/**
 * Wraps an {@link ImageOperation} in a structure that allows for coupling, event handling etc.
 *
 * Arrays tracking int rgb colors and doubles for
 * brightness, red, green, and blue separately are maintained and can serve
 * as producers for couplings.
 * <br>
 * The actual filtering happens in the {@link org.simbrain.world.imageworld.filters}
 * package. Filters do the work of allowing the filtered images to
 * couple to something else. This makes sense biologically: retinal patterns
 * are what neurons "sense".
 *
 * @author Tim Shea
 * @author Jeff Yoshimi
 */
public class Filter implements AttributeContainer, EditableObject {

    /**
     * Name of the filter.
     */
    @UserParameter(
            label = "Name"
    )
    private String name;

    /**
     * An ImageSource from which to extract filter values.  For "image world" this will be a
     * {@link java.awt.image.FilteredImageSource}, which applies the relevant downscaling, thresholding,
     * and other operations.
     */
    private ImageSource source;

    /**
     * The values this matrix produces for floating point channel couplings.
     * Four copies of the (flattened) matrix are stored for brightness (0),
     * red (1), green (2), and blue (3).
     */
    private transient double[][] channels;

    /**
     * Array of ints representing rgb colors. See
     * {@link BufferedImage#getRGB(int, int)}
     */
    private transient int[] rgbColors;

    /**
     * Construct a filter without attaching it to a source.
     * Currently used by 3d component.
     *
     * @param name The name of the filter
     */
    protected Filter(String name) {
        this.name = name;
    }

    /**
     * Construct a filter attached to an ImageSource.
     *
     * @param name   The name of the filter
     * @param source The source to attach.
     */
    public Filter(String name, ImageSource source) {
        this.name = name;
        this.source = source;
        initChannels();
        source.getEvents().onResize(this::initChannels);
        source.getEvents().onImageUpdate(this::updateFilter);
    }

    public Object readResolve() {
        source.getEvents().onResize(this::initChannels);
        source.getEvents().onImageUpdate(this::updateFilter);
        initChannels();
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ImageSource getSource() {
        return source;
    }

    protected void setSource(ImageSource source) {
        if (this.source == source) {
            return;
        }
        this.source = source;
    }

    public int getWidth() {
        return source.getWidth();
    }

    public int getHeight() {
        return source.getHeight();
    }

    @Producible()
    public double[] getBrightness() {
        return channels[0];
    }

    @Producible( defaultVisibility = false)
    public double[] getRed() {
        return channels[1];
    }

    @Producible( defaultVisibility = false)
    public double[] getGreen() {
        return channels[2];
    }

    @Producible( defaultVisibility = false)
    public double[] getBlue() {
        return channels[3];
    }

    @Producible()
    public int[] getRGBColor() {
        return rgbColors;
    }

    @Override
    public String toString() {
        return this.name;
    }


    private void initChannels() {
        channels = new double[4][getWidth() * getHeight()];
        rgbColors = new int[getWidth() * getHeight()];
    }

    /**
     * Update the filter.
     */
    private void updateFilter() {

        if (source.getHeight() != getHeight() || source.getWidth() != getWidth()) {
            throw new AssertionError();
        }

        for (int y = 0; y < source.getHeight(); ++y) {
            for (int x = 0; x < source.getWidth(); ++x) {
                int color = source.getCurrentImage().getRGB(x, y);

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

    @Override
    public String getId() {
        return getName();
    }
}
