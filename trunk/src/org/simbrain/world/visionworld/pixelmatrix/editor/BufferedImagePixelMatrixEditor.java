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

import java.awt.BorderLayout;
import java.awt.Component;

import java.awt.event.ActionEvent;

import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.util.LabelledItemPanel;

import org.simbrain.world.visionworld.PixelMatrix;

import org.simbrain.world.visionworld.pixelmatrix.BufferedImagePixelMatrix;

/**
 * Buffered image pixel matrix editor.
 */
public final class BufferedImagePixelMatrixEditor
    extends JPanel
    implements PixelMatrixEditor {

    /** Image file. */
    private File imageFile;

    /** Image file name. */
    private JTextField imageFileName;

    /** Height. */
    private JTextField height;

    /** Width. */
    private JTextField width;

    /** Open image file action. */
    private Action openImageFile;

    /** Default height. */
    private static final int DEFAULT_HEIGHT = 100;

    /** Default width. */
    private static final int DEFAULT_WIDTH = 100;


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
        imageFileName = new JTextField();
        imageFileName.setEnabled(false);
        height = new JTextField();
        width = new JTextField();

        openImageFile = new AbstractAction("...") {
                /** {@inheritDoc} */
                public void actionPerformed(final ActionEvent event) {
                    JFileChooser fileChooser = new JFileChooser();
                    // todo:  set file name filter
                    if (JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(null)) {
                        imageFile = fileChooser.getSelectedFile();
                        imageFileName.setText(imageFile.getName());
                        height.setText("(from image)");
                        height.setEnabled(false);
                        width.setText("(from image)");
                        width.setEnabled(false);
                    }
                }
            };
    }

    /**
     * Layout components.
     */
    private void layoutComponents() {
        setLayout(new BorderLayout());
        LabelledItemPanel filePanel = new LabelledItemPanel();
        filePanel.addItem("Image", createImageFilePanel());
        filePanel.addItem("Height", height);
        filePanel.addItem("Width", width);
        add("Center", filePanel);
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
        height.setText(String.valueOf(DEFAULT_HEIGHT));
        width.setText(String.valueOf(DEFAULT_WIDTH));
        height.setEnabled(true);
        width.setEnabled(true);
        return this;
    }

    /** {@inheritDoc} */
    public PixelMatrix createPixelMatrix() throws PixelMatrixEditorException {
        try {
            if (imageFile == null) {
                int h = Integer.valueOf(height.getText());
                int w = Integer.valueOf(width.getText());
                return new BufferedImagePixelMatrix(w, h);
            }
            else {
                BufferedImage image = ImageIO.read(imageFile);
                return new BufferedImagePixelMatrix(image);
            }
        }
        catch (IOException e) {
            throw new PixelMatrixEditorException(e);
        }
    }
}
