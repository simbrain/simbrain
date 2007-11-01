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
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponentListener;
import org.simbrain.workspace.gui.DesktopComponent;

/**
 * Vision world frame.
 */
public final class VisionWorldDesktopComponent extends DesktopComponent<VisionWorldComponent> {

    /** Vision world. */
    private final VisionWorld visionWorld;

    /** Listener for vision world component events. */
    private interface VisionWorldComponentListener extends WorkspaceComponentListener {
    };

    /** Listener for vision world component events. */
    private final VisionWorldComponentListener listener = new VisionWorldComponentListener() {
        public void componentUpdated() {
            updateComponent();
        }
    };

    /**
     * Create a new vision world frame with the specified workspace.
     *
     * @param component component, must not be null
     */
    public VisionWorldDesktopComponent(VisionWorldComponent component) {
        super(component);
        component.addListener(listener);
        this.setPreferredSize(new Dimension(400,400));

        VisionWorldModel visionWorldModel = new MutableVisionWorldModel();
        visionWorld = new VisionWorld(visionWorldModel);

        JMenuBar menuBar = new JMenuBar();
        JToolBar toolBar = new JToolBar();

        JMenu file = new JMenu("File");
        for (Action action : visionWorld.getFileMenuActions()) {
            file.add(action);
            toolBar.add(action);
        }

        toolBar.addSeparator();

        JMenu edit = new JMenu("Edit");
        for (Action action : visionWorld.getEditMenuActions()) {
            edit.add(action);
            toolBar.add(action);
        }

        toolBar.addSeparator();

        JMenu view = new JMenu("View");
        for (Action action : visionWorld.getViewMenuActions()) {
            view.add(action);
            toolBar.add(action);
        }

        menuBar.add(file);
        menuBar.add(edit);
        menuBar.add(view);
        setJMenuBar(menuBar);

        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add("North", toolBar);
        contentPane.add("Center", visionWorld);
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
    }

    @Override
    public String getFileExtension() {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public void save(File saveFile) {
        // TODO Auto-generated method stub
    }


    /** {@inheritDoc} */
    public List<Consumer> getConsumers() {
        return Collections.<Consumer>emptyList();
    }


    /** {@inheritDoc} */
    public List<Coupling> getCouplings() {
        return Collections.<Coupling>emptyList();
    }


    /** {@inheritDoc} */
    public List<Producer> getProducers() {
        List<Producer> producers = new ArrayList<Producer>();
        VisionWorldModel model = visionWorld.getModel();
        SensorMatrix sensorMatrix = model.getSensorMatrix();
        for (int column = 0, columns = sensorMatrix.columns(); column < columns; column++) {
            for (int row = 0, rows = sensorMatrix.rows(); row < rows; row++) {
                Sensor sensor = sensorMatrix.getSensor(row, column);
                producers.add(sensor);
            }
        }
        return Collections.unmodifiableList(producers);
    }

    /** {@inheritDoc} */
    public void updateComponent() {
        super.updateComponent();
        // Possibly change this later so only sensors with couplings are updated.
        VisionWorldModel model = visionWorld.getModel();
        PixelMatrix pixelMatrix = model.getPixelMatrix();
        SensorMatrix sensorMatrix = model.getSensorMatrix();
        for (int column = 0, columns = sensorMatrix.columns(); column < columns; column++) {
            for (int row = 0, rows = sensorMatrix.rows(); row < rows; row++) {
                Sensor sensor = sensorMatrix.getSensor(row, column);
                sensor.sample(pixelMatrix);
            }
        }
    }

    @Override
    public void open(File openFile) {
        // TODO Auto-generated method stub
    }

}
