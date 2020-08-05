package org.simbrain.world.imageworld.filters;

import org.simbrain.util.UserParameter;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;

public class OffsetOp extends ImageOperation<OffsetOp> implements BufferedImageOp {

    @UserParameter(
            label = "Offset X"
    )
    private int dx = 0;

    @UserParameter(
            label = "Offset Y"
    )
    private int dy = 0;

    public OffsetOp() {

    }

    public OffsetOp(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public OffsetOp getOp() {
        return this;
    }

    @Override
    public OffsetOp copy() {
        return new OffsetOp(dx, dy);
    }

    @Override
    public String getName() {
        return "Offset";
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
        AffineTransformOp op = new AffineTransformOp(
                new AffineTransform(1, 0, 0, 1, dx, dy),
                AffineTransformOp.TYPE_NEAREST_NEIGHBOR
        );
        dest = op.filter(src, null).getSubimage(0, 0, src.getWidth(), src.getHeight());
        return dest;
    }

    @Override
    public Rectangle2D getBounds2D(BufferedImage src) {
        return new Rectangle(0, 0, src.getWidth(), src.getHeight());
    }

    @Override
    public BufferedImage createCompatibleDestImage(BufferedImage source, ColorModel colorModel) {
        return new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
    }

    @Override
    public Point2D getPoint2D(Point2D sourcePoint, Point2D destinationPoint) {
        if (destinationPoint == null) {
            return sourcePoint;
        } else {
            destinationPoint.setLocation(sourcePoint);
            return destinationPoint;
        }
    }

    @Override
    public RenderingHints getRenderingHints() {
        return null;
    }
}
