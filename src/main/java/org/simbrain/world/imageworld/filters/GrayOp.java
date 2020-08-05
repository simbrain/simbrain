package org.simbrain.world.imageworld.filters;

import java.awt.color.ColorSpace;
import java.awt.image.ColorConvertOp;

public class GrayOp extends ImageOperation<ColorConvertOp> {

    private transient ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);

    public ColorConvertOp getOp() {
        return op;
    }

    @Override
    public GrayOp copy() {
        return new GrayOp();
    }

    @Override
    public String getName() {
        return "Gray Scale";
    }

    public Object readResolve() {
        op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        return this;
    }
}
