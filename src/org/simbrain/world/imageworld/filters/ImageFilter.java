package org.simbrain.world.imageworld.filters;

import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorConvertOp;

import org.simbrain.world.imageworld.ImageSource;
import org.simbrain.world.imageworld.ImageSourceAdapter;
import org.simbrain.world.imageworld.ImageSourceListener;

/**
 * ImageFilter decorates an ImageSource with a color and size transform.
 *
 * The "model" that {@link ImageFilterFactory} provides a "view" on.
 *
 * Based on a similar pattern in awt.
 *
 * @author Tim Shea
 * @author Jeff Yoshimi
 */
public class ImageFilter extends ImageSourceAdapter implements ImageSourceListener {

    /** @return a BuffereImageOp which does not alter the input image */
    protected static BufferedImageOp getIdentityOp() {
        return new AffineTransformOp(new AffineTransform(),
                AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
    }

    /** @return a BufferedImageOp which converts the input image to a grayscale colorspace */
    protected static BufferedImageOp getGrayOp() {
        return new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
    }

    /**
     * @param x the horizontal scaling factor
     * @param y the vertical scaling factor
     * @param smooth whether the output image should receive bilinear smoothing
     * @return a BufferedImageOp which applies a scaling transform to input images
     */
    protected static BufferedImageOp getScaleOp(float x, float y, boolean smooth) {
        AffineTransform transform = AffineTransform.getScaleInstance(x, y);
        int interpolation = smooth ? AffineTransformOp.TYPE_BILINEAR
                : AffineTransformOp.TYPE_NEAREST_NEIGHBOR;
        return new AffineTransformOp(transform, interpolation);
    }

    private final ImageSource wrappedSource;
    private String type;
    private BufferedImageOp colorOp;
    private int width;
    private int height;
    private BufferedImageOp scaleOp;

    /**
     * Construct a new ImageFilter.
     * @param source the ImageSource to be filtered
     * @param type the type of this filter
     * @param colorOp the color filter to apply
     * @param width the width of the output image
     * @param height the height of the output image
     */
    public ImageFilter(ImageSource source, String type, BufferedImageOp colorOp, int width, int height) {
        wrappedSource = source;
        this.type = type;
        this.colorOp = colorOp;
        this.width = width;
        this.height = height;
        wrappedSource.addListener(this);
    }

    public String getType() {
        return type;
    }

    public BufferedImageOp getColorOp() {
        return colorOp;
    }

    /** @return the current unfiltered image */
    public BufferedImage getUnfilteredImage() {
        return wrappedSource.getCurrentImage();
    }

    /** @param value the BufferedImageOp to assign */
    protected void setScaleOp(BufferedImageOp value) {
        scaleOp = value;
    }

    @Override
    public void onImageUpdate(ImageSource source) {
        BufferedImage image = scaleOp.filter(source.getCurrentImage(), null);
        image = colorOp.filter(image, null);
        setCurrentImage(image);
    }

    @Override
    public void onResize(ImageSource source) {
        float scaleX = (float) width / source.getWidth();
        float scaleY = (float) height / source.getHeight();
        scaleOp = getScaleOp(scaleX, scaleY, true);
        notifyResize();
    }
}
