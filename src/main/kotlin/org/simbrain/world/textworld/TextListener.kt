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
package org.simbrain.world.textworld

import org.simbrain.world.textworld.TextWorld.TextItem

/**
 * Listen for changes in the text world.
 */
interface TextListener {
    /**
     * The text has changed.
     */
    fun textChanged()

    /**
     * The dictionary has changed.
     */
    fun dictionaryChanged()

    /**
     * The position of the caret has changed.
     */
    fun positionChanged()

    /**
     * The current item has changed.
     *
     * @param newItem the new current text item.
     */
    fun currentItemChanged(newItem: TextItem?)

    /**
     * The current preferences have changed.
     */
    fun preferencesChanged()

    /**
     * Adapter class so users of the interface don't have to implement every
     * method.
     */
    class TextAdapter : TextListener {
        override fun textChanged() {}
        override fun dictionaryChanged() {}
        override fun positionChanged() {}
        override fun currentItemChanged(newItem: TextItem?) {}
        override fun preferencesChanged() {}
    }
}