/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
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
package org.simbrain.util.widgets;

import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Simple utility for making rows with a label and a component.
 */
public class LabelledItem extends JPanel {

    /**
     * Construct the labelled item.
     *
     * @param labelText
     *            the text for the label
     * @param component
     *            the labelled component
     */
    public LabelledItem(String labelText, JComponent component) {
        Box itemBox = Box.createHorizontalBox();
        itemBox.setAlignmentX(Box.LEFT_ALIGNMENT);
        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(100, 10));
        itemBox.add(label);
        itemBox.add(Box.createHorizontalStrut(10));
        itemBox.add(Box.createHorizontalGlue());
        itemBox.add(component);
        add(itemBox);
    }

}
