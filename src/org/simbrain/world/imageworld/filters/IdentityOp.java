package org.simbrain.world.imageworld.filters;

import org.simbrain.util.propertyeditor.EditableObject;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;

public class IdentityOp extends ImageOperation<AffineTransformOp> {

    private transient AffineTransformOp op = new AffineTransformOp(new AffineTransform(), AffineTransformOp.TYPE_NEAREST_NEIGHBOR);

    public AffineTransformOp getOp() {
        return op;
    }

    @Override
    public EditableObject copy() {
        return new IdentityOp();
    }

    @Override
    public String getName() {
        return "Color";
    }

    public Object readResolve() {
        op = new AffineTransformOp(new AffineTransform(), AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        return this;
    }
}