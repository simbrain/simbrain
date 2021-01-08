package org.simbrain.world.imageworld.filters;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImageOp;

public class FilterUtils {

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
}
