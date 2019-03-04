package org.simbrain.world.imageworld;

import org.simbrain.util.genericframe.GenericFrame;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class ImageAlbumDesktopComponent extends ImageDesktopComponent<ImageAlbumComponent> {

    /**
     * The image world component .
     */
    private ImageAlbumComponent component;

    /**
     * Construct a new ImageDesktopComponent GUI.
     *
     * @param frame     The frame in which to place GUI elements.
     * @param component The ImageWorldComponent to interact with.
     */
    public ImageAlbumDesktopComponent(GenericFrame frame, ImageAlbumComponent component) {
        super(frame, component);
    }

    public void initializeSensorMatrices() {

    }

    @Override
    public List<JMenuItem> getAdditionalFileMenuItems() {
        List<JMenuItem> returnList = new LinkedList<>();
        JMenuItem loadImages = new JMenuItem("Load Images...");
        loadImages.addActionListener(this::loadImages);
        returnList.add(loadImages);
        return returnList;
    }

    @Override
    public List<JButton> getAdditionalSourceToolbarButtons() {
        List<JButton> returnList = new LinkedList<>();
        JButton loadImagesButton = new JButton();
        loadImagesButton.setIcon(org.simbrain.resource.ResourceManager.getSmallIcon("photo.png"));
        loadImagesButton.setToolTipText("Load Images");
        loadImagesButton.addActionListener(this::loadImages);
        returnList.add(loadImagesButton);
        JButton previousImagesButton = new JButton();
        previousImagesButton.setIcon(org.simbrain.resource.ResourceManager.getSmallIcon("TangoIcons-GoPrevious.png"));
        previousImagesButton.setToolTipText("Previous Image");
        previousImagesButton.addActionListener(this::previousImage);
        returnList.add(previousImagesButton);
        JButton nextImagesButton = new JButton();
        nextImagesButton.setIcon(org.simbrain.resource.ResourceManager.getSmallIcon("TangoIcons-GoNext.png"));
        nextImagesButton.setToolTipText("Next Image");
        nextImagesButton.addActionListener(this::nextImage);
        returnList.add(nextImagesButton);
        return returnList;
    }

    @Override
    public ImageAlbumComponent getComponent() {
        return component;
    }

    @Override
    public void setComponent(ImageAlbumComponent component) {
        this.component = component;
    }

    private void loadImages(ActionEvent evt) {
        fileChooser.setDescription("Select an image to load");
        File[] files = fileChooser.showMultiOpenDialogNative();
        if (files != null) {
            this.getComponent().getWorld().loadImages(files);
        }
    }


    private void previousImage(ActionEvent evt) {
        component.getWorld().previousFrame();
    }

    private void nextImage(ActionEvent evt) {
        component.getWorld().nextFrame();
    }

}
