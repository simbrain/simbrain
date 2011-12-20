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
package org.simbrain.util;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Displays a simple JFrame, centers it, packs it, and makes it visible.
 *
 * @author jyoshimi
 */
public class SimpleFrame {

    /**
     * Show the provided panel in a jframe.
     *
     * @param component the component to show.
     */
    public static void displayPanel(JComponent component) {
        displayPanel(component, null);
    }

    /**
     * Show the panel in a jframe with the provided title.
     *
     * @param component the component to show
     * @param string the title
     */
    public static void displayPanel(JComponent component, String string) {
        JFrame frame = new JFrame();
        frame.getContentPane().add(component);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setTitle(string);
    }

}
