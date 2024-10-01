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
package org.simbrain.network.core

import org.piccolo2d.event.PInputEvent
import org.simbrain.network.events.TextObjectEvents
import java.awt.geom.Point2D

/**
 * **NetworkTextObject** is a string of text in a neural network, typically
 * used to label elements of a neural network simulation. Contains basic text
 * properties as well. Multiple lines of text can be displayed.
 */
open class NetworkTextObject : LocatableModel {
    /**
     * x-coordinate of this object in 2-space.
     */
    private var x = 0.0

    /**
     * y-coordinate of this object in 2-space.
     */
    private var y = 0.0

    /**
     * The main text data.
     */
    var text = ""
        set(value) {
            field = value
            events.textUpdated.fire()
        }

    /**
     * Name of Font for this text.
     */
    var fontName: String = "Helvetica"

    /**
     * Font size.
     */
    var fontSize: Int = 12

    /**
     * Is this text italic or not.
     */
    var isItalic: Boolean = false

    /**
     * Is this text bold or not.
     */
    var isBold: Boolean = false

    /**
     * Support for property change events.
     */
    @Transient
    override val events: TextObjectEvents = TextObjectEvents()

    // TODO: Temporary so that when added to networkpanel the event is availalble
    @Transient
    var inputEvent: PInputEvent? = null

    @XStreamConstructor
    constructor(): super() {

    }

    /**
     * Construct the text object with initial text.
     *
     * @param initialText text for the text object
     */
    constructor(initialText: String): super() {
        text = initialText
    }

    /**
     * Copy constructor.
     *
     * @param text text object to copy
     */
    constructor(text: NetworkTextObject): super() {
        this.text = text.text
        this.x = text.x
        this.y = text.y
        this.fontSize = text.fontSize
        this.fontName = text.fontName
        this.isBold = text.isBold
        this.isItalic = text.isItalic
    }

    override fun toString(): String {
        return "(${Math.round(x)},${Math.round(y)})"
    }

    override suspend fun delete() {
        events.deleted.fire(this).await()
    }

    override var location: Point2D
        get() = Point2D.Double(x, y)
        set(location) {
            x = location.x
            y = location.y
            events.locationChanged.fire()
        }


}
