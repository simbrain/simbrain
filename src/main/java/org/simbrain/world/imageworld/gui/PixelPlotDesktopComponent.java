package org.simbrain.world.imageworld.gui;

import org.simbrain.util.ResourceManager;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.workspace.gui.DesktopComponent;
import org.simbrain.world.imageworld.PixelPlotComponent;

import javax.swing.*;
import java.util.LinkedList;
import java.util.List;

public class PixelPlotDesktopComponent extends DesktopComponent<PixelPlotComponent> {

    /**
     * Construct a new PixelDisplayDesktopComponent GUI.
     *
     * @param frame     The frame in which to place GUI elements.
     * @param component The PixelDisplayComponent to interact with.
     */
    public PixelPlotDesktopComponent(GenericFrame frame, PixelPlotComponent component) {
        super(frame, component);
        //TODO
        // add(new ImageWorldDesktopPanel(this.getParentFrame(), this, getWorkspaceComponent().getWorld()));
    }


    /**
     * Toolbar buttons for pixel display world.
     *
     * @return the list of buttons
     */
    public java.util.List<JButton> getPixelDisplayToolbar() {
        List<JButton> returnList = new LinkedList<>();
        JButton editEmitterButton = new JButton();
        editEmitterButton.setIcon(ResourceManager.getSmallIcon("menu_icons/resize.png"));
        editEmitterButton.setToolTipText("Edit Emitter Matrix");
        //TODO
        // editEmitterButton.addActionListener(evt -> {
        //     ResizeEmitterMatrixDialog dialog = new ResizeEmitterMatrixDialog((PixelConsumer) world);
        //     dialog.setVisible(true);
        // });
        returnList.add(editEmitterButton);
        return returnList;
    }

    @Override
    protected void closing() {

    }


}
