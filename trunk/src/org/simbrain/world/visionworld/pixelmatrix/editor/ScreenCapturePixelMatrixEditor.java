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

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.simbrain.world.visionworld.PixelMatrix;
import org.simbrain.world.visionworld.pixelmatrix.ScreenCapturePixelMatrix;

/**
 * Screen capture pixel matrix editor.
 */
public final class ScreenCapturePixelMatrixEditor
    extends JPanel
    implements PixelMatrixEditor {

    /** Screen origin x. */
    private JTextField originX;

    /** Screen origin y. */
    private JTextField originY;

    /** Height. */
    private JTextField height;

    /** Width. */
    private JTextField width;

    /** Display name. */
    private static final String DISPLAY_NAME = "Screen capture pixel matrix";

    /** Description. */
    private static final String DESCRIPTION = null;

    /** Empty insets. */
    private static final Insets EMPTY_INSETS = new Insets(0, 0, 0, 0);

    /** Field insets. */
    private static final Insets FIELD_INSETS = new Insets(0, 0, 6, 0);

    /** Label insets. */
    private static final Insets LABEL_INSETS = new Insets(0, 0, 6, 0);


    /**
     * Create a new screen capture pixel matrix editor.
     */
    public ScreenCapturePixelMatrixEditor() {
        super();

        initComponents();
        layoutComponents();
    }


    /**
     * Initialize components.
     */
    private void initComponents() {
        originX = new JTextField();
        originY = new JTextField();
        height = new JTextField();
        width = new JTextField();
    }

    /**
     * Layout components.
     */
    private void layoutComponents() {
        setLayout(new GridBagLayout());
        setBorder(new CompoundBorder(new TitledBorder(DISPLAY_NAME), new EmptyBorder(6, 6, 6, 6)));
        setToolTipText(DESCRIPTION);
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

        c.gridwidth = GridBagConstraints.RELATIVE;
        c.insets = LABEL_INSETS;
        c.gridy++;
        c.weightx = 0.33f;
        add(new JLabel("Screen origin x"), c);

        c.insets = FIELD_INSETS;
        c.gridx = 1;
        c.weightx = 0.66f;
        add(originX, c);

        c.gridwidth = GridBagConstraints.RELATIVE;
        c.insets = LABEL_INSETS;
        c.gridx = 0;
        c.gridy++;
        c.weightx = 0.33f;
        add(new JLabel("Screen origin y"), c);

        c.insets = FIELD_INSETS;
        c.gridx = 1;
        c.weightx = 0.66f;
        add(originY, c);

        c.gridwidth = GridBagConstraints.RELATIVE;
        c.insets = LABEL_INSETS;
        c.gridx = 0;
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

        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = EMPTY_INSETS;
        c.gridx = 0;
        c.gridy++;
        c.weighty = 1.0f;
        add(Box.createGlue(), c);
    }

    /** {@inheritDoc} */
    public Component getEditorComponent() {
        originX.setText(String.valueOf(ScreenCapturePixelMatrix.DEFAULT_ORIGIN_X));
        originY.setText(String.valueOf(ScreenCapturePixelMatrix.DEFAULT_ORIGIN_Y));
        height.setText(String.valueOf(ScreenCapturePixelMatrix.DEFAULT_HEIGHT));
        width.setText(String.valueOf(ScreenCapturePixelMatrix.DEFAULT_WIDTH));
        return this;
    }

    /** {@inheritDoc} */
    public PixelMatrix createPixelMatrix() throws PixelMatrixEditorException {
        try {
            int x = Integer.valueOf(originX.getText());
            int y = Integer.valueOf(originY.getText());
            int h = Integer.valueOf(height.getText());
            int w = Integer.valueOf(width.getText());
            ScreenCapturePixelMatrix pixelMatrix = new ScreenCapturePixelMatrix(x, y, w, h);
            pixelMatrix.capture();
            return pixelMatrix;
        }
        catch (NumberFormatException e) {
            throw new PixelMatrixEditorException(e);
        }
        catch (IllegalArgumentException e) {
            throw new PixelMatrixEditorException(e);
        }
    }

    /** {@inheritDoc} */
    public String toString() {
        return DISPLAY_NAME;
    }
}
