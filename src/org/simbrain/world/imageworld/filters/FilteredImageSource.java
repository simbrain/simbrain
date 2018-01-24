package org.simbrain.world.imageworld.filters;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;

import org.simbrain.world.imageworld.ImageSource;
import org.simbrain.world.imageworld.ImageSourceAdapter;
import org.simbrain.world.imageworld.ImageSourceListener;

/**
 * FilteredImageSource decorates an ImageSource with a color and size transform.
 *
 * Based on a similar pattern in awt.
 *
 * @author Tim Shea
 * @author Jeff Yoshimi
 */
public class FilteredImageSource extends ImageSourceAdapter implements ImageSourceListener {

    private ImageSource wrappedSource;
    private String type;
    private int width;
    private int height;
    private transient BufferedImageOp colorOp;
    private transient BufferedImageOp scaleOp;

    /**
     * Construct a new FilteredImageSource.
     * @param source the ImageSource to be filtered
     * @param type the type of this filter
     * @param colorOp the color filter to apply
     * @param width the width of the output image
     * @param height the height of the output image
     */
    public FilteredImageSource(ImageSource source, String type, BufferedImageOp colorOp, int width, int height) {
        wrappedSource = source;
        this.type = type;
        this.colorOp = colorOp;
        this.width = width;
        this.height = height;
        wrappedSource.addListener(this);
    }

    public Object readResolve() {
        super.readResolve();
        if (type.equals("Gray Filter")) {
            colorOp = ImageFilterFactory.createGrayOp();
        } else {
            // Default to color filter
            colorOp = ImageFilterFactory.createIdentityOp();
        }
        scaleToFit(wrappedSource);
        wrappedSource.addListener(this);
        return this;
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

    /** @param value The BufferedImageOp to assign to the color op. */
    protected void setColorOp(BufferedImageOp value) {
        colorOp = value;
    }

    /** @param value the BufferedImageOp to assign to the scale op. */
    protected void setScaleOp(BufferedImageOp value) {
        scaleOp = value;
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
        BufferedImage image = scaleOp.filter(source.getCurrentImage(), null);
        image = colorOp.filter(image, null);
        setCurrentImage(image);
    }

    @Override
    public void onResize(ImageSource source) {
        scaleToFit(source);
        notifyResize();
    }

    protected void scaleToFit(ImageSource source) {
        float scaleX = (float) width / source.getWidth();
        float scaleY = (float) height / source.getHeight();
        scaleOp = ImageFilterFactory.createScaleOp(scaleX, scaleY, true);
    }
}
