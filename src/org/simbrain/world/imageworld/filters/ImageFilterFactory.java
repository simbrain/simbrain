package org.simbrain.world.imageworld.filters;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.world.imageworld.ImageSource;

import javax.swing.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorConvertOp;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Maintains gui information (name, editor panel) relating to filters. Add
 * custom filter types by extending this factory class, see e.g
 * {@link ThresholdFilterFactory}
 */
public abstract class ImageFilterFactory {
    
    private static Map<String, ImageFilterFactory> factories = new HashMap<String, ImageFilterFactory>();

    /**
     * The default filters.
     */
    static {
        putFactory("Color Filter", new ImageFilterFactory() {
            @Override
            public FilteredImageSource create(ImageSource source) {
                return createColorFilter(source, getWidth(), getHeight());
            }
        });
        putFactory("Gray Filter", new ImageFilterFactory() {
            @Override
            public FilteredImageSource create(ImageSource source) {
                return createGrayFilter(source, getWidth(), getHeight());
            }
        });
    }

    public static FilteredImageSource createColorFilter(ImageSource source, int width, int height) {
        return new FilteredImageSource(source, "Color Filter", createIdentityOp(), width, height);
    }

    public static FilteredImageSource createGrayFilter(ImageSource source, int width, int height) {
        return new FilteredImageSource(source, "Gray Filter", createGrayOp(), width, height);
    }

    /**
     * Create an identity image op (no transform). Useful for null object pattern.
     */
    protected static ImageOperation<AffineTransformOp> createIdentityOp() {
        return new IdentityOp();
    }

    /**
     * Create a BufferedImageOp which converts the input image to a grayscale colorspace
     */
    protected static ImageOperation<ColorConvertOp> createGrayOp() {
        return new GrayOp();
    }

    /**
     * Create a scaling image op.
     *
     * @param sX     the horizontal scaling factor
     * @param sY     the vertical scaling factor
     * @param smooth whether the output image should receive bilinear smoothing
     * @return a BufferedImageOp which applies a scaling transform to input images
     */
    protected static BufferedImageOp createScaleOp(float sX, float sY, boolean smooth) {
        AffineTransform transform = AffineTransform.getScaleInstance(sX, sY);
        int interpolation = smooth ? AffineTransformOp.TYPE_BILINEAR : AffineTransformOp.TYPE_NEAREST_NEIGHBOR;
        return new AffineTransformOp(transform, interpolation);
    }

    /**
     * Create a translation and scaling transform op.
     *
     * @param tX     The amount to translate in the x direction.
     * @param tY     The amount to translate in the y direction.
     * @param sX     The amount to scale in the x direction.
     * @param sY     The amount to scale in the y direction.
     * @param smooth Whether to use bilinear smoothing (slower) or nearest neighbor.
     * @return a BufferedImageOp which applies a scaling and translation to input images.
     */
    protected static BufferedImageOp createTranslateAndScaleOp(float tX, float tY, float sX, float sY, boolean smooth) {
        AffineTransform transform = AffineTransform.getTranslateInstance(tX, tY);
        transform.scale(sX, sY);
        int interpolation = smooth ? AffineTransformOp.TYPE_BILINEAR : AffineTransformOp.TYPE_NEAREST_NEIGHBOR;
        return new AffineTransformOp(transform, interpolation);
    }

    public static void putFactory(String type, ImageFilterFactory factory) {
        factories.put(type, factory);
    }

    public static ImageFilterFactory getFactory(String type) {
        return factories.get(type);
    }

    public static Set<String> getTypes() {
        return factories.keySet();
    }

    private int width;
    private int height;

    public void setDefaultValues() {
        width = 10;
        height = 10;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int value) {
        width = value;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int value) {
        height = value;
    }

    public LabelledItemPanel getEditorPanel() {
        LabelledItemPanel panel = new LabelledItemPanel();
        JFormattedTextField widthField = new JFormattedTextField(NumberFormat.getIntegerInstance());
        widthField.setValue(width);
        widthField.addPropertyChangeListener("value", (evt) -> {
            width = ((Number) widthField.getValue()).intValue();
            width = Math.max(Math.min(width, 10000), 1);
            widthField.setValue(width);
        });
        panel.addItem("Width", widthField);
        JFormattedTextField heightField = new JFormattedTextField(NumberFormat.getIntegerInstance());
        heightField.setValue(height);
        heightField.addPropertyChangeListener("value", (evt) -> {
            height = ((Number) heightField.getValue()).intValue();
            height = Math.max(Math.min(height, 10000), 1);
            heightField.setValue(height);
        });
        panel.addItem("Height", heightField);
        return panel;
    }

    public abstract FilteredImageSource create(ImageSource source);
}
