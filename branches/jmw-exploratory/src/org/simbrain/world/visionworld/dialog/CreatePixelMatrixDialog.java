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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.awt.event.ActionEvent;

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

import org.simbrain.world.visionworld.PixelMatrix;
import org.simbrain.world.visionworld.VisionWorld;

import org.simbrain.world.visionworld.pixelmatrix.editor.PixelMatrixEditor;
import org.simbrain.world.visionworld.pixelmatrix.editor.PixelMatrixEditors;
import org.simbrain.world.visionworld.pixelmatrix.editor.PixelMatrixEditorException;

/**
 * Create pixel matrix dialog.
 */
public final class CreatePixelMatrixDialog
    extends JDialog {

    /** Pixel matrices. */
    private JComboBox pixelMatrices;

    /** Pixel matrix editor. */
    private PixelMatrixEditor pixelMatrixEditor;

    /** Pixel matrix editor placeholder. */
    private Container pixelMatrixEditorPlaceholder;

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
     * Create a new pixel matrix dialog.
     *
     * @param visionWorld vision world, must not be null
     */
    public CreatePixelMatrixDialog(final VisionWorld visionWorld) {
        super();
        if (visionWorld == null) {
            throw new IllegalArgumentException("visionWorld must not be null");
        }
        this.visionWorld = visionWorld;

        setTitle("Create Pixel Matrix");
        initComponents();
        layoutComponents();
    }


    /**
     * Initialize components.
     */
    private void initComponents() {
        pixelMatrices = new JComboBox(new PixelMatrixEditorsComboBoxModel());
        pixelMatrixEditor = PixelMatrixEditors.VALUES.get(0);
        pixelMatrixEditorPlaceholder = new JPanel();
        pixelMatrixEditorPlaceholder.setLayout(new BorderLayout());

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
        panel.add(new JLabel("Pixel matrix"), c);

        c.insets = FIELD_INSETS;
        c.gridx = 1;
        c.weightx = 0.66f;
        panel.add(pixelMatrices, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 1.0f;
        pixelMatrixEditorPlaceholder.add("Center", pixelMatrixEditor.getEditorComponent());
        panel.add(pixelMatrixEditorPlaceholder, c);

        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = EMPTY_INSETS;
        c.gridy++;
        c.weighty = 1.0f;
        panel.add(Box.createGlue(), c);
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
        PixelMatrix pixelMatrix = null;
        try {
            pixelMatrix = pixelMatrixEditor.createPixelMatrix();
        }
        catch (PixelMatrixEditorException e) {
            JOptionPane.showInternalMessageDialog(this, "Cannot create pixel matrix", e.getMessage(), JOptionPane.ERROR_MESSAGE);
            pixelMatrices.requestFocus();
        }
        visionWorld.getModel().setPixelMatrix(pixelMatrix);
        setVisible(false);
    }

    /**
     * Pixel matrix editors combo box model.
     */
    private class PixelMatrixEditorsComboBoxModel
        extends AbstractListModel
        implements ComboBoxModel {

        /** Selected pixel matrix editor. */
        private PixelMatrixEditor selection;


        /**
         * Create a new pixel matrix editors combo box model.
         */
        public PixelMatrixEditorsComboBoxModel() {
            super();
            selection = PixelMatrixEditors.VALUES.get(0);
        }


        /** {@inheritDoc} */
        public int getSize() {
            return PixelMatrixEditors.VALUES.size();
        }

        /** {@inheritDoc} */
        public Object getElementAt(final int index) {
            return PixelMatrixEditors.VALUES.get(index);
        }

        /** {@inheritDoc} */
        public Object getSelectedItem() {
            return selection;
        }

        /** {@inheritDoc} */
        public void setSelectedItem(final Object selection) {
            this.selection = (PixelMatrixEditor) selection;
        }
    }
}
