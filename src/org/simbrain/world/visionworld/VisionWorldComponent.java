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
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * Vision world frame.
 */
public final class VisionWorldComponent extends WorkspaceComponent {

    /** Vision world. */
    private final VisionWorld visionWorld;


    /**
     * Create a new vision world frame with the specified workspace.
     *
     * @param workspace workspace, must not be null
     */
    public VisionWorldComponent() {
        super();

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
    public int getDefaultHeight() {
        // TODO Auto-generated method stub
        return 450;
    }

    @Override
    public int getDefaultWidth() {
        return 450;
    }

    @Override
    public int getDefaultLocationX() {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public int getDefaultLocationY() {
        // TODO Auto-generated method stub
        return 0;
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
        for (SensorMatrix sensorMatrix : model.getSensorMatrices()) {
            for (int column = 0, columns = sensorMatrix.columns(); column < columns; column++) {
                for (int row = 0, rows = sensorMatrix.rows(); row < rows; row++) {
                    Sensor sensor = sensorMatrix.getSensor(row, column);
                    producers.add(sensor);
                }
            }
        }
        return Collections.unmodifiableList(producers);
    }

    /** {@inheritDoc} */
    public void updateComponent() {
        super.updateComponent();
        VisionWorldModel model = visionWorld.getModel();
        PixelMatrix pixelMatrix = model.getPixelMatrix();
        for (SensorMatrix sensorMatrix : model.getSensorMatrices()) {
            for (int column = 0, columns = sensorMatrix.columns(); column < columns; column++) {
                for (int row = 0, rows = sensorMatrix.rows(); row < rows; row++) {
                    Sensor sensor = sensorMatrix.getSensor(row, column);
                    sensor.sample(pixelMatrix);
                }
            }
        }
    }

    @Override
    public void open(File openFile) {
        // TODO Auto-generated method stub
        
    }


    @Override
    public int getWindowIndex() {
        // TODO Auto-generated method stub
        return 0;
    }
}
