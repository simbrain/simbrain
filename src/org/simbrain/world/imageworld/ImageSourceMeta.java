package org.simbrain.world.imageworld;

import org.simbrain.util.UserParameter;
import org.simbrain.util.propertyeditor.CopyableObject;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.world.imageworld.filters.IdentityOp;
import org.simbrain.world.imageworld.filters.ImageOperation;

public class ImageSourceMeta implements EditableObject, CopyableObject {

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
            label = "Use Filter",
            order = 3

    )
    private boolean useFilter = true;


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

    public boolean isUseFilter() {
        return useFilter;
    }

    public void setUseFilter(boolean useFilter) {
        this.useFilter = useFilter;
    }

    public ImageOperation getColorOp() {
        return colorOp;
    }

    public void setColorOp(ImageOperation colorOp) {
        this.colorOp = colorOp;
    }

    @Override
    public EditableObject copy() {
        ImageSourceMeta ret = new ImageSourceMeta();
        ret.colorOp = (ImageOperation) colorOp.copy();
        ret.height = height;
        ret.width = width;
        ret.useFilter = useFilter;
        return ret;
    }
}
