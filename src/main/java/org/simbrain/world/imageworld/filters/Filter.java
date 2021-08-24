package org.simbrain.world.imageworld.filters;

import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Producible;
import org.simbrain.world.imageworld.ImageSource;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;

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
 * @author Yulin Li
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

    @UserParameter(
            label = "Width",
            order = 1
    )
    private int width;

    @UserParameter(
            label = "Height",
            order = 2
    )
    private int height;

    @UserParameter(
            label = "Filter",
            order = 3,
            isObjectType = true
    )
    private ImageOperation imageOp;

    /**
     * Use for rescaling.
     */
    private transient BufferedImageOp scaleOp;

    /**
     * An ImageSource from which to extract filter values.  For "image world" this will be a
     * {@link java.awt.image.FilteredImageSource}, which applies the relevant downscaling, thresholding,
     * and other operations.
     */
    private ImageSource source;

    /**
     * The filtered image that can be displayed in the desktop.
     */
    private BufferedImage filteredImage;

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
     * Construct a filter attached to an ImageSource.
     *
     * @param name   The name of the filter
     * @param source The source to attach.
     */
    public Filter(String name, ImageSource source, ImageOperation imageOp, int width, int height) {
        this.name = name;
        this.source = source;
        this.imageOp = imageOp;
        this.width = width;
        this.height = height;
        refreshFilter();
    }

    /**
     * See {@link org.simbrain.workspace.serialization.WorkspaceComponentDeserializer}
     */
    public Object readResolve() {
        refreshFilter();
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

    void initChannels() {
        channels = new double[4][width * height];
        rgbColors = new int[width * height];
    }

    BufferedImage applyFilter() {
        BufferedImage image = source.getCurrentImage();
        image = scaleOp.filter(image, null);
        image = imageOp.getOp().filter(image, null);
        return image;
    }

    void initScaleOp() {
        // Subtract 0.1 from width and height to avoid exceeding the specified dimension due to floating point error.
        float scaleX = (width - 0.1f) / source.getWidth();
        float scaleY = (height - 0.1f) / source.getHeight();
        scaleOp = FilterUtils.createScaleOp(scaleX, scaleY, true);
    }

    /**
     * Update the filter.
     */
    public void updateFilter() {

        filteredImage = applyFilter();

        if (filteredImage.getHeight() != height || filteredImage.getWidth() != width) {
            throw new AssertionError("Filtered image size not equal to filter size");
        }

        // Set values of channels
        for (int y = 0; y < filteredImage.getHeight(); ++y) {
            for (int x = 0; x < filteredImage.getWidth(); ++x) {

                // Update rgb colors
                int color = filteredImage.getRGB(x, y);
                rgbColors[y * width + x] = color;

                // Update other color channels
                // Cf https://stackoverflow.com/questions/2534116/how-to-convert-get-rgbx-y-integer-pixel-to-colorr-g-b-a-in-java
                double red = ((color >>> 16) & 0xFF) / 255.0;
                double green = ((color >>> 8) & 0xFF) / 255.0;
                double blue = (color & 0xFF) / 255.0;
                // Cf. https://en.wikipedia.org/wiki/Luma_(video)
                channels[0][y * width + x] = (red * 0.2126 + green * 0.7152 + blue * 0.0722);
                channels[1][y * width + x] = red;
                channels[2][y * width + x] = green;
                channels[3][y * width + x] = blue;
            }
        }
    }

    @Override
    public String getId() {
        return getName();
    }

    public Image getFilteredImage() {
        return filteredImage;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void refreshFilter() {
        initChannels();
        initScaleOp();
        updateFilter();
    }
}
