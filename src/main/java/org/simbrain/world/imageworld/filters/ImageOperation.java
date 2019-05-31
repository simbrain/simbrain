package org.simbrain.world.imageworld.filters;

import org.simbrain.util.propertyeditor.CopyableObject;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImageOp;
import java.util.List;

/**
 * An abstract image operation.
 *
 * @param <O> The type of the image operation
 */
public abstract class ImageOperation<O extends BufferedImageOp> implements CopyableObject {

    /**
     * List of classes for filter menu in property editor
     */
    private static List<Class<? extends ImageOperation>> OP_LIST = List.of(
            IdentityOp.class,
            GrayOp.class,
            ThresholdOp.class
    );
    // TODO: Can later add OffsetOp when use cases are worked out

    public static List<Class<? extends ImageOperation>> getTypes() {
        return OP_LIST;
    }

    abstract O getOp();
}
