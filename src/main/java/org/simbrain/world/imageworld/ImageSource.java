package org.simbrain.world.imageworld;

import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.world.imageworld.events.ImageSourceEvents;

import java.awt.image.BufferedImage;

/**
 * ImageSource is basically any visible image in an ImageWorld.
 * produces BufferedImages periodically and notifies listeners of new
 * images or changes to the image size.
 * <br>
 * Image sources can be enabled or disabled. E.g. if a webcam is available it can
 * enable its image source, and then when it is turned off the image source can be
 * disabled (however this is not currently used and has not been tested).
 *
 * @author Tim Shea
 */
public interface ImageSource extends AttributeContainer, EditableObject {

    /**
     * @return Returns whether the source will update the image when updateImage
     * is invoked.
     */
    boolean isEnabled();

    /**
     * @param value whether the source should update the image.
     */
    void setEnabled(boolean value);

    /**
     * @return Returns the current image.
     */
    BufferedImage getCurrentImage();

    /**
     * @return Returns the width of the images produced by the source.
     */
    int getWidth();

    /**
     * @return Returns the height of the images produced by the source.
     */
    int getHeight();

    /**
     * @return Returns event handler
     */
    ImageSourceEvents getEvents();
}
