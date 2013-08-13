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
package org.simbrain.workspace.gui.couplingmanager;

import java.awt.Color;

/**
 * Some static utilities for the coupling manager GUI.
 *
 * @author Jeff Yoshimi
 */
public class CouplingManagerUtils {

    /**
     * Associates attribute and coupling data types (Classes) with colors used
     * in displaying attributes and couplings.
     *
     * @param dataType the data type to associate with a color
     * @return the color associated with a data type
     */
    public static Color getColor(Class<?> dataType) {
        if (dataType == double.class) {
            return Color.black;
        } else if (dataType == double[].class) {
            return Color.green.darker().darker();
        } else if (dataType == String.class) {
            return Color.blue.brighter();
        }
        return Color.black;
    }
}
