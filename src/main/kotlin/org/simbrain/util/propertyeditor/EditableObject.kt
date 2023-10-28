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
package org.simbrain.util.propertyeditor

/**
 * Indicates that objects that can be edited in an [AnnotatedPropertyEditor]. Returns a name that is used in the
 * GUI and has a commit method that can be overridden if special actions are needed when committing.
 *
 * @author Jeff Yoshimi
 */
interface EditableObject {
    /**
     * Returns the name of this object.
     */
    val name: String
        get() = "No-name (be sure getName() is overridden)"
    // See ParameterWidget#getTypeMap
    /**
     * A method to be invoked at the end of [AnnotatedPropertyEditor.commitChanges]
     */
    fun onCommit() {}


    fun typeMapProvider(): List<Class<out EditableObject>>? = null
}