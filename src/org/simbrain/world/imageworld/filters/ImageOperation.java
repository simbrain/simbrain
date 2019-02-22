package org.simbrain.world.imageworld.filters;

import org.simbrain.util.propertyeditor.CopyableObject;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImageOp;
import java.util.List;

public abstract class ImageOperation<O extends BufferedImageOp> implements CopyableObject {

    private static List<Class<? extends ImageOperation>> OP_LIST = List.of(
            IdentityOp.class,
            OffsetOp.class,
            GrayOp.class,
            ThresholdOp.class
    );

    public static List<Class<? extends ImageOperation>> getTypes() {
        return OP_LIST;
    }

    abstract O getOp();
}
