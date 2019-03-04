package org.simbrain.world.imageworld;

import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.world.imageworld.dialogs.ResizeEmitterMatrixDialog;

import javax.swing.*;
import java.util.LinkedList;
import java.util.List;

public class PixelDisplayDesktopComponent extends ImageDesktopComponent<PixelDisplayComponent> {

    /**
     * The image world component .
     */
    private PixelDisplayComponent component;

    /**
     * Construct a new ImageDesktopComponent GUI.
     *
     * @param frame     The frame in which to place GUI elements.
     * @param component The ImageWorldComponent to interact with.
     */
    public PixelDisplayDesktopComponent(GenericFrame frame, PixelDisplayComponent component) {
        super(frame, component);
    }

    @Override
    public List<JMenuItem> getAdditionalFileMenuItems() {
        return List.of();
    }

    @Override
    public List<JButton> getAdditionalSourceToolbarButtons() {
        List<JButton> returnList = new LinkedList<>();
        JButton editEmitterButton = new JButton();
        editEmitterButton.setIcon(org.simbrain.resource.ResourceManager.getSmallIcon("resize.png"));
        editEmitterButton.setToolTipText("Edit Emitter Matrix");
        editEmitterButton.addActionListener(evt -> {
            ResizeEmitterMatrixDialog dialog = new ResizeEmitterMatrixDialog(getComponent().getWorld());
            dialog.setVisible(true);
        });
        returnList.add(editEmitterButton);
        return returnList;
    }

    @Override
    public PixelDisplayComponent getComponent() {
        return component;
    }

    @Override
    public void setComponent(PixelDisplayComponent component) {
        this.component = component;
    }
}
