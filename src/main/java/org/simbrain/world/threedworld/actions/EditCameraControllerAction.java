/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2006 Jeff Yoshimi <www.jeffyoshimi.net>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.world.threedworld.actions;

import org.simbrain.util.ResourceManager;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.world.threedworld.ThreeDWorld;
import org.simbrain.world.threedworld.engine.ThreeDEngine;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Action for showing world preferences.
 */
public final class EditCameraControllerAction extends AbstractAction {
    private static final long serialVersionUID = 3353903249936368827L;

    private ThreeDWorld world;

    public EditCameraControllerAction(ThreeDWorld world) {
        super("Edit Camera Controller");
        this.world = world;
        putValue(SMALL_ICON, ResourceManager.getImageIcon("Prefs.png"));
        putValue(SHORT_DESCRIPTION, "Edit Camera Controller");
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(final ActionEvent event) {
        ThreeDEngine.State previousState = world.getEngine().getState();
        world.getEngine().queueState(ThreeDEngine.State.SystemPause, true);
        AnnotatedPropertyEditor editor = new AnnotatedPropertyEditor(world.getCameraController());
        JDialog dialog = editor.getDialog();
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                world.getEngine().queueState(previousState, false);
            }
        });
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
}
