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
package org.simbrain.world.visionworld.filter.editor;

import java.awt.Component;

import org.simbrain.world.visionworld.Filter;

/**
 * Filter editor.
 */
public interface FilterEditor {

    /**
     * Return the editor component for this filter editor.
     * The editor component will not be null.
     *
     * @return the editor component for this filter editor
     */
    Component getEditorComponent();

    /**
     * Create a new instance of Filter from the properties of this
     * filter editor.  The filter will not be null.
     *
     * @return a new instance of Filter created from the properties
     *    of this filter editor
     * @throws FilterEditorException if a Filter cannot properly be
     *    created from the properties of this filter editor
     */
    Filter createFilter() throws FilterEditorException;
}