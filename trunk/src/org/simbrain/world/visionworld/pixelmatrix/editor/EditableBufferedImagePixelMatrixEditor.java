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

import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.util.LabelledItemPanel;

import org.simbrain.world.visionworld.PixelMatrix;

import org.simbrain.world.visionworld.pixelmatrix.EditableBufferedImagePixelMatrix;

/**
 * Editable buffered image pixel matrix editor.
 */
public final class EditableBufferedImagePixelMatrixEditor
    extends JPanel
    implements PixelMatrixEditor {

    /** Height. */
    private JTextField height;

    /** Width. */
    private JTextField width;

    /** Default height. */
    private static final int DEFAULT_HEIGHT = 100;

    /** Default width. */
    private static final int DEFAULT_WIDTH = 100;


    /**
     * Create a new editable buffered image pixel matrix editor.
     */
    public EditableBufferedImagePixelMatrixEditor() {
        super();

        initComponents();
        layoutComponents();
    }


    /**
     * Initialize components.
     */
    private void initComponents() {
        height = new JTextField();
        width = new JTextField();
    }

    /**
     * Layout components.
     */
    private void layoutComponents() {
        setLayout(new BorderLayout());
        LabelledItemPanel dimensionsPanel = new LabelledItemPanel();
        dimensionsPanel.addItem("Height", height);
        dimensionsPanel.addItem("Width", width);
        add("Center", dimensionsPanel);
    }

    /** {@inheritDoc} */
    public Component getEditorComponent() {
        height.setText(String.valueOf(DEFAULT_HEIGHT));
        width.setText(String.valueOf(DEFAULT_WIDTH));
        return this;
    }

    /** {@inheritDoc} */
    public PixelMatrix createPixelMatrix() throws PixelMatrixEditorException {
        try {
            int h = Integer.valueOf(height.getText());
            int w = Integer.valueOf(width.getText());
            return new EditableBufferedImagePixelMatrix(w, h);
        }
        catch (NumberFormatException e) {
            throw new PixelMatrixEditorException(e);
        }
        catch (IllegalArgumentException e) {
            throw new PixelMatrixEditorException(e);
        }
    }
}
