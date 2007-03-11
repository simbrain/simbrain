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
package org.simbrain.world.visionworld.pixelmatrix.editor;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.simbrain.world.visionworld.PixelMatrix;

import org.simbrain.world.visionworld.pixelmatrix.BufferedImagePixelMatrix;
import org.simbrain.world.visionworld.sensormatrix.editor.*;

/**
 * Buffered image pixel matrix editor.
 */
public final class BufferedImagePixelMatrixEditor
    extends JPanel
    implements PixelMatrixEditor {

    /** Empty image checkbox. */
    private JCheckBox emptyImage;

    /** Existing image checkbox. */
    private JCheckBox existingImage;

    /** Height. */
    private JTextField height;

    /** Width. */
    private JTextField width;

    /** Image file. */
    private File imageFile;

    /** Image file name. */
    private JTextField imageFileName;

    /** Open image file action. */
    private Action openImageFile;

    /** Empty insets. */
    private static final Insets EMPTY_INSETS = new Insets(0, 0, 0, 0);

    /** Field insets. */
    private static final Insets FIELD_INSETS = new Insets(0, 0, 6, 0);

    /** Label insets. */
    private static final Insets LABEL_INSETS = new Insets(0, 0, 6, 0);


    /**
     * Create a new buffered image pixel matrix editor.
     */
    public BufferedImagePixelMatrixEditor() {
        super();

        initComponents();
        layoutComponents();
    }

    /**
     * Initialize components.
     */
    private void initComponents() {
        emptyImage = new JCheckBox("Empty image", true);
        emptyImage.addActionListener(new ActionListener() {
                /** {@inheritDoc} */
                public void actionPerformed(final ActionEvent event) {
                    if (emptyImage.isSelected()) {
                        height.setEnabled(true);
                        width.setEnabled(true);
                        openImageFile.setEnabled(false);
                    }
                    else {
                        height.setEnabled(false);
                        width.setEnabled(false);
                        openImageFile.setEnabled(true);
                    }
                }
            });

        existingImage = new JCheckBox("Existing image", false);
        existingImage.addActionListener(new ActionListener() {
                /** {@inheritDoc} */
                public void actionPerformed(final ActionEvent event) {
                    if (existingImage.isSelected()) {
                        height.setEnabled(false);
                        width.setEnabled(false);
                        openImageFile.setEnabled(true);
                    }
                    else {
                        height.setEnabled(true);
                        width.setEnabled(true);
                        openImageFile.setEnabled(false);
                    }
                }
            });

        ButtonGroup group = new ButtonGroup();
        group.add(emptyImage);
        group.add(existingImage);

        height = new JTextField();
        width = new JTextField();
        imageFileName = new JTextField();
        imageFileName.setEnabled(false);

        openImageFile = new AbstractAction("...") {
                /** {@inheritDoc} */
                public void actionPerformed(final ActionEvent event) {
                    JFileChooser fileChooser = new JFileChooser();
                    // todo:  set file name filter
                    if (JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(null)) {
                        imageFile = fileChooser.getSelectedFile();
                        imageFileName.setText(imageFile.getName());
                    }
                }
            };

        openImageFile.setEnabled(false);
    }

    /**
     * Layout components.
     */
    private void layoutComponents() {
        setLayout(new GridBagLayout());
        setBorder(new CompoundBorder(new TitledBorder("Buffered image"), new EmptyBorder(6, 6, 6, 6)));
        GridBagConstraints c = new GridBagConstraints();

        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridheight = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = FIELD_INSETS;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0f;
        c.weighty = 0;
        add(emptyImage, c);

        c.gridwidth = GridBagConstraints.RELATIVE;
        c.insets = LABEL_INSETS;
        c.gridy++;
        c.weightx = 0.33f;
        add(new JLabel("Height"), c);

        c.insets = FIELD_INSETS;
        c.gridx = 1;
        c.weightx = 0.66f;
        add(height, c);

        c.gridwidth = GridBagConstraints.RELATIVE;
        c.insets = LABEL_INSETS;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0.33f;
        add(new JLabel("Width"), c);

        c.insets = FIELD_INSETS;
        c.gridx = 1;
        c.weightx = 0.66f;
        add(width, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = EMPTY_INSETS;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 1.0f;
        add(Box.createVerticalStrut(12), c);

        c.gridy++;
        c.insets = FIELD_INSETS;
        add(existingImage, c);

        c.gridwidth = GridBagConstraints.RELATIVE;
        c.insets = LABEL_INSETS;
        c.gridy++;
        c.weightx = 0.33f;
        add(new JLabel("Image"), c);

        c.insets = FIELD_INSETS;
        c.gridx = 1;
        c.weightx = 0.66f;
        add(createImageFilePanel(), c);

        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = EMPTY_INSETS;
        c.gridx = 0;
        c.gridy++;
        c.weighty = 1.0f;
        add(Box.createGlue(), c);
    }

    /**
     * Create and return the image file panel.
     *
     * @return the image file panel
     */
    private JPanel createImageFilePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(imageFileName);
        panel.add(Box.createHorizontalStrut(6));
        panel.add(new JButton(openImageFile));
        return panel;
    }

    /** {@inheritDoc} */
    public Component getEditorComponent() {
        imageFile = null;
        imageFileName.setText("");
        height.setText(String.valueOf(BufferedImagePixelMatrix.DEFAULT_HEIGHT));
        width.setText(String.valueOf(BufferedImagePixelMatrix.DEFAULT_WIDTH));
        emptyImage.setEnabled(true);
        return this;
    }

    /** {@inheritDoc} */
    public PixelMatrix createPixelMatrix() throws PixelMatrixEditorException {
        try {
            if (emptyImage.isSelected()) {
                int h = Integer.valueOf(height.getText());
                int w = Integer.valueOf(width.getText());
                return new BufferedImagePixelMatrix(w, h);
            }
            BufferedImage image = ImageIO.read(imageFile);
            return new BufferedImagePixelMatrix(image);
        }
        catch (IOException e) {
            throw new PixelMatrixEditorException(e);
        }
        catch (IllegalArgumentException e) {
            throw new PixelMatrixEditorException(e);
        }
    }
}
