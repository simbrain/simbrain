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

import org.simbrain.util.getSimbrainXStream
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.WorkspaceComponent
import java.io.InputStream
import java.io.OutputStream
import java.util.*

/**
 * ReaderComponent is the container for the readerworld, which adds
 * producers.
 */
class TextWorldComponent : WorkspaceComponent {

    var world: TextWorld
        private set

    /**
     * Creates a new frame of type TextWorld.
     *
     * @param name name of this component
     */
    constructor(name: String?) : super(name) {
        world = TextWorld()
        init()
    }

    /**
     * Construct a component from an existing world; used in deserializing.
     *
     * @param name     name of component
     * @param newWorld provided world
     */
    constructor(name: String?, newWorld: TextWorld) : super(name) {
        world = newWorld
        init()
    }

    /**
     * Initialize attribute types.
     */
    private fun init() {
        world = world
    }

    override fun save(output: OutputStream, format: String) {
        getSimbrainXStream().toXML(world, output)
    }

    override fun update() {
        world.update()
    }

    override fun getAttributeContainers(): List<AttributeContainer> {
        return Arrays.asList<AttributeContainer>(world)
    }

    companion object {
        fun open(input: InputStream?, name: String?, format: String?): TextWorldComponent {
            val newWorld = getSimbrainXStream().fromXML(input) as TextWorld
            return TextWorldComponent(name, newWorld)
        }
    }
}