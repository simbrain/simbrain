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
package org.simbrain.world.visionworld;

import java.awt.BorderLayout;
import java.awt.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPanel;

import org.simbrain.world.visionworld.action.AddSensorMatrixAction;
import org.simbrain.world.visionworld.action.CreatePixelMatrixAction;
import org.simbrain.world.visionworld.action.NormalViewAction;
import org.simbrain.world.visionworld.action.StackedViewAction;

import org.simbrain.world.visionworld.dialog.AddSensorMatrixDialog;
import org.simbrain.world.visionworld.dialog.CreatePixelMatrixDialog;

import org.simbrain.world.visionworld.view.NormalView;
import org.simbrain.world.visionworld.view.StackedView;

/**
 * Vision world.
 */
public final class VisionWorld
    extends JPanel {

    /** Model for this vision world. */
    private final VisionWorldModel model;

    /** Normal view. */
    private final NormalView normalView;

    /** Stacked view. */
    private final StackedView stackedView;

    /** Model listener. */
    private final VisionWorldModelListener modelListener = new VisionWorldModelAdapter();


    /**
     * Create a new vision world with the specified model.
     *
     * @param model model for this vision world, must not be null
     */
    public VisionWorld(final VisionWorldModel model) {
        super();
        if (model == null) {
            throw new IllegalArgumentException("model must not be null");
        }
        this.model = model;
        this.model.addModelListener(modelListener);

        normalView = new NormalView(this);
        stackedView = new StackedView(this);

        setLayout(new BorderLayout());
        add("Center", normalView);
    }


    /**
     * Return the model for this vision world.
     * The model will not be null.
     *
     * @return the model for this vision world
     */
    public VisionWorldModel getModel() {
        return model;
    }

    /**
     * Switch to the normal view.
     */
    public void normalView() {
        if (getComponentCount() > 0) {
            Component child = getComponent(0);
            if (child != null) {
                if (child == stackedView) {
                    remove(stackedView);
                    add("Center", normalView);
                    invalidate();
                    validate();
                }
            }
        }
    }

    /**
     * Switch to the stacked view.
     */
    public void stackedView() {
        if (getComponentCount() > 0) {
            Component child = getComponent(0);
            if (child != null) {
                if (child == normalView) {
                    remove(normalView);
                    add("Center", stackedView);
                    invalidate();
                    validate();
                }
            }
        }
    }

    /**
     * Create pixel matrix.
     */
    public void createPixelMatrix() {
        CreatePixelMatrixDialog d = new CreatePixelMatrixDialog();
        d.setBounds(100, 100, 450, 500);
        d.setVisible(true);
    }

    /**
     * Add sensor matrix.
     */
    public void addSensorMatrix() {
        AddSensorMatrixDialog d = new AddSensorMatrixDialog();
        d.setBounds(100, 100, 450, 550);
        d.setVisible(true);
    }

    /**
     * Return a list of file menu actions for this vision world.
     *
     * @return a list of file menu actions for this vision world
     */
    public List<Action> getFileMenuActions() {
        return Arrays.asList(new Action[] { new AddSensorMatrixAction(this), new CreatePixelMatrixAction(this) });
    }

    /**
     * Return a list of edit menu actions for this vision world.
     *
     * @return a list of edit menu actions for this vision world
     */
    public List<Action> getEditMenuActions() {
        return Collections.<Action>emptyList();
    }

    /**
     * Return a list of view menu actions for this vision world.
     *
     * @return a list of view menu actions for this vision world
     */
    public List<Action> getViewMenuActions() {
        return Arrays.asList(new Action[] { new NormalViewAction(this), new StackedViewAction(this) });
    }
}
