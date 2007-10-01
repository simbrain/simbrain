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
package org.simbrain.world.visionworld.dialog;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import javax.swing.border.EmptyBorder;

import org.simbrain.world.visionworld.Filter;
import org.simbrain.world.visionworld.SensorMatrix;
import org.simbrain.world.visionworld.VisionWorld;

import org.simbrain.world.visionworld.filter.editor.FilterEditor;
import org.simbrain.world.visionworld.filter.editor.FilterEditors;
import org.simbrain.world.visionworld.filter.editor.FilterEditorException;

import org.simbrain.world.visionworld.sensormatrix.editor.SensorMatrixEditor;
import org.simbrain.world.visionworld.sensormatrix.editor.SensorMatrixEditors;
import org.simbrain.world.visionworld.sensormatrix.editor.SensorMatrixEditorException;

/**
 * Create sensor matrix dialog.
 */
public final class CreateSensorMatrixDialog
    extends JDialog {

    /** Filters. */
    private JComboBox filters;

    /** Sensor matrices. */
    private JComboBox sensorMatrices;

    /** Filter editor. */
    private FilterEditor filterEditor;

    /** Sensor matrix editor. */
    private SensorMatrixEditor sensorMatrixEditor;

    /** Filter editor placeholder. */
    private Container filterEditorPlaceholder;

    /** Sensor matrix editor placeholder. */
    private Container sensorMatrixEditorPlaceholder;

    /** OK action. */
    private Action ok;

    /** Cancel action. */
    private Action cancel;

    /** Help action. */
    private Action help;

    /** Vision world. */
    private final VisionWorld visionWorld;

    /** Empty insets. */
    private static final Insets EMPTY_INSETS = new Insets(0, 0, 0, 0);

    /** Field insets. */
    private static final Insets FIELD_INSETS = new Insets(0, 0, 6, 0);

    /** Label insets. */
    private static final Insets LABEL_INSETS = new Insets(0, 0, 6, 0);


    /**
     * Create a new create sensor matrix dialog.
     *
     * @param visionWorld vision world, must not be null
     */
    public CreateSensorMatrixDialog(final VisionWorld visionWorld) {
        super();
        if (visionWorld == null) {
            throw new IllegalArgumentException("visionWorld must not be null");
        }
        this.visionWorld = visionWorld;

        setTitle("Create Sensor Matrix");
        initComponents();
        layoutComponents();
    }


    /**
     * Initialize components.
     */
    private void initComponents() {
        filters = new JComboBox(new FilterEditorsComboBoxModel());

        filters.addActionListener(new ActionListener() {
                /** {@inheritDoc} */
                public void actionPerformed(final ActionEvent event) {
                    filterEditorPlaceholder.remove(filterEditor.getEditorComponent());
                    filterEditor = (FilterEditor) filters.getModel().getSelectedItem();
                    filterEditorPlaceholder.add("Center", filterEditor.getEditorComponent());
                    filterEditorPlaceholder.invalidate();
                    getContentPane().validate();
                }
            });

        sensorMatrices = new JComboBox(new SensorMatrixEditorsComboBoxModel());

        sensorMatrices.addActionListener(new ActionListener() {
                /** {@inheritDoc} */
                public void actionPerformed(final ActionEvent event) {
                    sensorMatrixEditorPlaceholder.remove(sensorMatrixEditor.getEditorComponent());
                    sensorMatrixEditor = (SensorMatrixEditor) sensorMatrices.getModel().getSelectedItem();
                    sensorMatrixEditorPlaceholder.add("Center", sensorMatrixEditor.getEditorComponent());
                    sensorMatrixEditorPlaceholder.invalidate();
                    getContentPane().validate();
                }
            });

        filterEditor = FilterEditors.VALUES.get(0);
        sensorMatrixEditor = SensorMatrixEditors.VALUES.get(0);
        filterEditorPlaceholder = new JPanel();
        filterEditorPlaceholder.setLayout(new BorderLayout());
        sensorMatrixEditorPlaceholder = new JPanel();
        sensorMatrixEditorPlaceholder.setLayout(new BorderLayout());

        ok = new AbstractAction("OK") {
                /** {@inheritDoc} */
                public void actionPerformed(final ActionEvent event) {
                    ok();
                }
            };

        cancel = new AbstractAction("Cancel") {
                /** {@inheritDoc} */
                public void actionPerformed(final ActionEvent event) {
                    setVisible(false);
                }
            };

        help = new AbstractAction("Help") {
                /** {@inheritDoc} */
                public void actionPerformed(final ActionEvent event) {
                    // empty
                }
            };
        help.setEnabled(false);
    }

    /**
     * Layout components.
     */
    private void layoutComponents() {
        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        contentPane.add("Center", createMainPanel());
        contentPane.add("South", createButtonPanel());
    }

    /**
     * Create and return the main panel.
     *
     * @return the main panel
     */
    private JPanel createMainPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridheight = 1;
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.insets = LABEL_INSETS;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.33f;
        c.weighty = 0;
        panel.add(new JLabel("Default filter"), c);

        c.insets = FIELD_INSETS;
        c.gridx = 1;
        c.weightx = 0.66f;
        panel.add(filters, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = EMPTY_INSETS;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 1.0f;
        panel.add(Box.createVerticalStrut(6), c);

        c.insets = FIELD_INSETS;
        c.gridy++;
        filterEditorPlaceholder.add("Center", filterEditor.getEditorComponent());
        panel.add(filterEditorPlaceholder, c);

        c.insets = EMPTY_INSETS;
        c.gridy++;
        panel.add(Box.createVerticalStrut(6), c);

        c.gridwidth = GridBagConstraints.RELATIVE;
        c.insets = LABEL_INSETS;
        c.gridy++;
        c.weightx = 0.33f;
        panel.add(new JLabel("Sensor matrix"), c);

        c.insets = FIELD_INSETS;
        c.gridx = 1;
        c.weightx = 0.66f;
        panel.add(sensorMatrices, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = EMPTY_INSETS;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 1.0f;
        panel.add(Box.createVerticalStrut(6), c);

        c.insets = FIELD_INSETS;
        c.gridy++;
        sensorMatrixEditorPlaceholder.add("Center", sensorMatrixEditor.getEditorComponent());
        panel.add(sensorMatrixEditorPlaceholder, c);

        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.gridheight = 1;
        c.insets = EMPTY_INSETS;
        c.gridy++;
        c.weighty = 1.0f;
        c.weightx = 1.0f;
        panel.add(Box.createVerticalStrut(6), c);

        return panel;
    }

    /**
     * Create and return the button panel.
     *
     * @return the button panel
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(Box.createHorizontalGlue());
        panel.add(Box.createHorizontalGlue());

        JButton okButton = new JButton(ok);
        JButton cancelButton = new JButton(cancel);
        JButton helpButton = new JButton(help);
        Dimension d = new Dimension(Math.max(cancelButton.getPreferredSize().width, 70),
                                    cancelButton.getPreferredSize().height);
        okButton.setPreferredSize(d);
        cancelButton.setPreferredSize(d);
        helpButton.setPreferredSize(d);
        getRootPane().setDefaultButton(okButton);

        panel.add(okButton);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(cancelButton);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(helpButton);
        return panel;
    }

    /**
     * Rename me.
     */
    private void ok() {

        Filter filter = null;
        SensorMatrix sensorMatrix = null;

        try {
            filter = filterEditor.createFilter();
        }
        catch (FilterEditorException e) {
            JOptionPane.showInternalMessageDialog(this, "Cannot create filter", e.getMessage(), JOptionPane.ERROR_MESSAGE);
            filters.requestFocus();
        }

        try {
            sensorMatrix = sensorMatrixEditor.createSensorMatrix(filter);
        }
        catch (SensorMatrixEditorException e) {
            JOptionPane.showInternalMessageDialog(this, "Cannot create sensor matrix", e.getMessage(), JOptionPane.ERROR_MESSAGE);
            sensorMatrices.requestFocus();
        }

        visionWorld.getModel().setSensorMatrix(sensorMatrix);
        setVisible(false);
    }

    /**
     * Filter editors combo box model.
     */
    private class FilterEditorsComboBoxModel
        extends AbstractListModel
        implements ComboBoxModel {

        /** Selected filter editor. */
        private FilterEditor selection;


        /**
         * Create a new filter editors combo box model.
         */
        public FilterEditorsComboBoxModel() {
            super();
            selection = FilterEditors.VALUES.get(0);
        }


        /** {@inheritDoc} */
        public int getSize() {
            return FilterEditors.VALUES.size();
        }

        /** {@inheritDoc} */
        public Object getElementAt(final int index) {
            return FilterEditors.VALUES.get(index);
        }

        /** {@inheritDoc} */
        public Object getSelectedItem() {
            return selection;
        }

        /** {@inheritDoc} */
        public void setSelectedItem(final Object selection) {
            this.selection = (FilterEditor) selection;
        }
    }

    /**
     * Sensor matrix editors combo box model.
     */
    private class SensorMatrixEditorsComboBoxModel
        extends AbstractListModel
        implements ComboBoxModel {

        /** Selected sensor matrix editor. */
        private SensorMatrixEditor selection;


        /**
         * Create a new sensor matrix editors combo box model.
         */
        public SensorMatrixEditorsComboBoxModel() {
            super();
            selection = SensorMatrixEditors.VALUES.get(0);
        }


        /** {@inheritDoc} */
        public int getSize() {
            return SensorMatrixEditors.VALUES.size();
        }

        /** {@inheritDoc} */
        public Object getElementAt(final int index) {
            return SensorMatrixEditors.VALUES.get(index);
        }

        /** {@inheritDoc} */
        public Object getSelectedItem() {
            return selection;
        }

        /** {@inheritDoc} */
        public void setSelectedItem(final Object selection) {
            this.selection = (SensorMatrixEditor) selection;
        }
    }
}
