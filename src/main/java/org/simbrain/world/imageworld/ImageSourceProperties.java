package org.simbrain.world.imageworld;

import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.CopyableObject;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.world.imageworld.filters.IdentityOp;
import org.simbrain.world.imageworld.filters.ImageOperation;

/**
 * Properties used by a {@link org.simbrain.world.imageworld.filters.FilteredImageSource}
 * to facilitate use of {@link org.simbrain.util.propertyeditor.AnnotatedPropertyEditor}
 * to edit filter objects.
 */
public class ImageSourceProperties implements EditableObject, CopyableObject {

    @UserParameter(
            label ="Name"
    )
    private String name;

    @UserParameter(
            label = "Width",
            order = 1
    )
    private int width = 20;

    @UserParameter(
            label = "Height",
            order = 2
    )
    private int height = 20;

    @UserParameter(
            label = "Filter",
            isObjectType = true,
            order = 4
    )
    private transient ImageOperation colorOp = new IdentityOp();

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public ImageOperation getColorOp() {
        return colorOp;
    }

    public void setColorOp(ImageOperation colorOp) {
        this.colorOp = colorOp;
    }

    @Override
    public ImageSourceProperties copy() {
        ImageSourceProperties ret = new ImageSourceProperties();
        ret.colorOp = (ImageOperation) colorOp.copy();
        ret.height = height;
        ret.width = width;
        return ret;
    }
}
