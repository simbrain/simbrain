package org.simbrain.world.imageworld.filters;

import org.simbrain.util.propertyeditor.EditableObject;

import java.awt.color.ColorSpace;
import java.awt.image.ColorConvertOp;

public class GrayOp extends ImageOperation<ColorConvertOp> {

    private ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);

    public ColorConvertOp getOp() {
        return op;
    }

    @Override
    public EditableObject copy() {
        return new GrayOp();
    }

    @Override
    public String getName() {
        return "Gray Scale";
    }
}
