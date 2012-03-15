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
package org.simbrain.network.listeners;

import org.simbrain.network.interfaces.NetworkTextObject;

/**
 * Listener interface for receiving network events relating to network text
 * objects. Classes interested in responding to text related events are
 * registered with a RootNetwork, which broadcasts text relevant events to
 * registered observer classes.
 */
public interface TextListener {

    /**
     * Notify this listener of a text removed event.
     *
     * @param removedText removed text
     */
    void textRemoved(NetworkTextObject removedText);

    /**
     * Notify this listener of a text added event.
     *
     * @param newText added text
     */
    void textAdded(NetworkTextObject newText);

    /**
     * Notify this listener that text's state changed.
     *
     * @param newText changed text
     */
    void textChanged(NetworkTextObject newText);

}