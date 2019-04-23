package org.simbrain.world.imageworld.filters;

import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.world.imageworld.ImageSource;
import org.simbrain.world.imageworld.ImageSourceAdapter;
import org.simbrain.world.imageworld.ImageSourceListener;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;

/**
 * FilteredImageSource decorates an ImageSource with a color and size transform.
 * <p>
 * Based on a similar pattern in awt.
 *
 * @author Tim Shea
 * @author Jeff Yoshimi
 */
public class FilteredImageSource extends ImageSourceAdapter implements ImageSourceListener, EditableObject {

    private ImageSource wrappedSource;

    @UserParameter(
            label = "Name",
            order = 0
    )
    private String type;

    @UserParameter(
            label = "Width",
            order = 1
    )
    private int width = 10;

    @UserParameter(
            label = "Height",
            order = 2
    )
    private int height;

    @UserParameter(
            label = "Color Filter",
            order = 3,
            isObjectType = true
    )
    private ImageOperation colorOp;
    private transient BufferedImageOp scaleOp;

    /**
     * Construct a new FilteredImageSource.
     *
     * @param source  the ImageSource to be filtered
     * @param type    the type of this filter
     * @param colorOp the color filter to apply
     * @param width   the width of the output image
     * @param height  the height of the output image
     */
    public FilteredImageSource(ImageSource source, String type, ImageOperation colorOp, int width, int height) {
        wrappedSource = source;
        this.type = type;
        this.colorOp = colorOp;
        this.width = width;
        this.height = height;
        wrappedSource.addListener(this);
    }

    public Object readResolve() {
        super.readResolve();
        scaleToFit(wrappedSource);
        wrappedSource.addListener(this);
        return this;
    }

    public String getType() {
        return type;
    }

    public ImageOperation getColorOp() {
        return colorOp;
    }

    /**
     * @return the current unfiltered image
     */
    public BufferedImage getUnfilteredImage() {
        return wrappedSource.getCurrentImage();
    }

    /**
     * @param value The BufferedImageOp to assign to the color op.
     */
    protected void setColorOp(ImageOperation value) {
        colorOp = value;
    }

    /**
     * @param value the BufferedImageOp to assign to the scale op.
     */
    protected void setScaleOp(BufferedImageOp value) {
        scaleOp = value;
    }

    @Override
    public String getName() {
        return type;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void onImageUpdate(ImageSource source) {
        BufferedImage image = source.getCurrentImage();
        image = scaleOp.filter(image, null);
        image = colorOp.getOp().filter(image, null);
        setCurrentImage(image);
    }

    @Override
    public void onResize(ImageSource source) {
        scaleToFit(source);
        notifyResize();
    }

    protected void scaleToFit(ImageSource source) {
        // Subtract 0.1 from width and height to avoid exceeding the specified dimension due to floating point error.
        float scaleX = (width - 0.1f) / source.getWidth();
        float scaleY = (height - 0.1f) / source.getHeight();
        scaleOp = ImageFilterFactory.createScaleOp(scaleX, scaleY, true);
    }
}
