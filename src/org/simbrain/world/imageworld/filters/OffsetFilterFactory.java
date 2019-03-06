package org.simbrain.world.imageworld.filters;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.world.imageworld.ImageSource;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;

/**
 * OffsetFilterFactory constructs offset filters for use in ImageWorld. An
 * offset filter applies a translation along the x and y axes of the image after
 * scaling is applied. The translation is specified in pixels.
 */
public class OffsetFilterFactory extends ImageFilterFactory {

    private int xOffset;
    private int yOffset;

    static {
        ImageFilterFactory.putFactory("Offset Filter", new OffsetFilterFactory());
    }

    public static FilteredImageSource createOffsetFilter(ImageSource source, int xOffset, int yOffset, int width, int height) {
        return new OffsetFilterSource(source, xOffset, yOffset, width, height);
    }

    @Override
    public void setDefaultValues() {
        super.setDefaultValues();
        xOffset = 0;
        yOffset = 0;
    }

    @Override
    public FilteredImageSource create(ImageSource source) {
        return createOffsetFilter(source, xOffset, yOffset, getWidth(), getHeight());
    }

    private static class OffsetFilterSource extends FilteredImageSource {
        private int xOffset;
        private int yOffset;

        OffsetFilterSource(ImageSource source, int xOffset, int yOffset, int width, int height) {
            super(source, "Offset Filter", ImageFilterFactory.createIdentityOp(), width, height);
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            onImageUpdate(source);
        }

        @Override
        protected void setCurrentImage(BufferedImage image) {
            BufferedImage offsetImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics graphics = offsetImage.getGraphics();
            graphics.drawImage(image, xOffset, yOffset, null);
            super.setCurrentImage(offsetImage);
        }
    }
}