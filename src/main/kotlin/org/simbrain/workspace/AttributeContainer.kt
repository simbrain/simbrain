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
package org.simbrain.workspace

/**
 * Designates an object as one that contains [Consumable] or [Producible] annotations, that can be linked together in
 * [Coupling]s.
 */
interface AttributeContainer {
    /**
     * Returns an attribute id that can be used to identify this container. Used in persistence (see
     * [org.simbrain.workspace.serialization.ArchivedAttribute] and in displaying Producers and Consumers.
     */
    val id: String?

    val childrenContainers: List<AttributeContainer>?
        get() = null
}