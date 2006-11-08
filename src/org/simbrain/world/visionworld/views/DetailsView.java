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
package org.simbrain.world.visionworld.views;

import java.awt.Insets;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import javax.swing.border.EmptyBorder;

import javax.swing.table.AbstractTableModel;

import org.simbrain.world.visionworld.PixelMatrix;
import org.simbrain.world.visionworld.SensorMatrix;
import org.simbrain.world.visionworld.VisionWorld;

/**
 * Details view.
 */
public final class DetailsView
    extends JPanel {

    /** Vision world. */
    private final VisionWorld visionWorld;

    /** Empty insets. */
    private static final Insets EMPTY_INSETS = new Insets(0, 0, 0, 0);

    /** Field insets. */
    private static final Insets FIELD_INSETS = new Insets(0, 0, 6, 0);


    /**
     * Create a new details view for the specified vision world.
     *
     * @param visionWorld vision world for this details view, must not be null
     */
    public DetailsView(final VisionWorld visionWorld) {
        super();
        if (visionWorld == null) {
            throw new IllegalArgumentException("visionWorld must not be null");
        }
        this.visionWorld = visionWorld;

        setBorder(new EmptyBorder(11, 11, 11, 11));

        GridBagLayout l = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(l);

        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridheight = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = EMPTY_INSETS;
        c.gridx = 0;
        c.weighty = 0;
        c.weightx = 1.0f;
        add(new JLabel("Sensors:"), c);

        c.insets = FIELD_INSETS;
        c.gridy++;
        add(new JScrollPane(new SensorMatrixTableEditor()), c);

        c.insets = EMPTY_INSETS;
        c.gridy++;
        add(new JLabel("Pixels:"), c);

        c.insets = FIELD_INSETS;
        c.gridy++;
        add(new JScrollPane(new PixelMatrixTableEditor()), c);

        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = EMPTY_INSETS;
        c.weighty = 1.0f;
        c.gridy++;
        add(Box.createGlue(), c);
    }

    /**
     * Sensor matrix table editor.
     */
    private class SensorMatrixTableEditor
        extends JTable {

        /**
         * Create a new sensor matrix table editor.
         */
        SensorMatrixTableEditor() {
            super();
            setModel(new SensorMatrixTableEditorModel());
            setPreferredScrollableViewportSize(new Dimension(0, 12 * (getRowHeight() + (2 * getRowMargin()))));
        }
    }

    /**
     * Pixel matrix table editor.
     */
    private class PixelMatrixTableEditor
        extends JTable {

        /**
         * Create a new pixel matrix table editor.
         */
        PixelMatrixTableEditor() {
            super();
            setModel(new PixelMatrixTableEditorModel());
            setPreferredScrollableViewportSize(new Dimension(0, 2 * (getRowHeight() + (2 * getRowMargin()))));
        }
    }

    /**
     * Sensor matrix table editor model.
     */
    private class SensorMatrixTableEditorModel
        extends AbstractTableModel {

        /** {@inheritDoc} */
        public int getRowCount() {
            return visionWorld.getModel().getSensorMatrixCount();
        }

        /** {@inheritDoc} */
        public int getColumnCount() {
            return 6;
        }

        /** {@inheritDoc} */
        public String getColumnName(final int column) {
            switch (column) {
            case 0:
                return "Sensor matrix";
            case 1:
                return "Receptive fields";
            case 2:
                return "Receptive field dimensions";
            case 3:
                return "Visible";
            case 4:
                return "Transparency";
            case 5:
                return "Properties editor";
            default:
                return null;
            }
        }

        /** {@inheritDoc} */
        public Object getValueAt(final int row, final int column) {
            SensorMatrix sensorMatrix = visionWorld.getModel().getSensorMatrices().get(row);
            if (sensorMatrix != null) {
                switch (column) {
                case 0:
                    return "Sparse sensor matrix";
                case 1:
                    return sensorMatrix.columns() + " x " + sensorMatrix.rows();
                case 2:
                    return sensorMatrix.getReceptiveFieldWidth() + " x " + sensorMatrix.getReceptiveFieldHeight();
                case 3:
                    return " <o> ";
                case 4:
                    return " -----|-- ";
                case 5:
                    return " [ ... ] ";
                default:
                    return null;
                }
            }
            return null;
        }
    }

    /**
     * Pixel matrix table editor model.
     */
    private class PixelMatrixTableEditorModel
        extends AbstractTableModel {

        /** {@inheritDoc} */
        public int getRowCount() {
            return 1;
        }

        /** {@inheritDoc} */
        public int getColumnCount() {
            return 5;
        }

        /** {@inheritDoc} */
        public String getColumnName(final int column) {
            switch (column) {
            case 0:
                return "Pixel matrix";
            case 1:
                return "Dimensions";
            case 2:
                return "Visible";
            case 3:
                return "Transparency";
            case 4:
                return "Properties editor";
            default:
                return null;
            }
        }

        /** {@inheritDoc} */
        public Object getValueAt(final int row, final int column) {
            PixelMatrix pixelMatrix = visionWorld.getModel().getPixelMatrix();
            switch (column) {
            case 0:
                return "Buffered image pixel matrix";
            case 1:
                return pixelMatrix.getWidth() + " x " + pixelMatrix.getHeight();
            case 2:
                return " <o> ";
            case 3:
                return " -----|-- ";
            case 4:
                return " [ ... ] ";
            default:
                return null;
            }
        }
    }
}
