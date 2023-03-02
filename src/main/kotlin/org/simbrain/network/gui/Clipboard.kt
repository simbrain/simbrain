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
package org.simbrain.network.gui

import kotlinx.coroutines.launch
import org.simbrain.network.LocatableModel
import org.simbrain.network.NetworkModel
import org.simbrain.network.util.SimnetUtils

/**
 * Buffer which holds network objects for cutting and pasting.
 */
object Clipboard {
    // To add new copy-pastable items, must update:
    // 1) SimnetUtils.getCopy()
    // 2) Network.addObjects
    // 3) NetworkPanel.getSelectedModels()
    /**
     * Static list of cut or copied objects.
     */
    private var copiedObjects: List<NetworkModel> = ArrayList()

    /**
     * List of components which listen for changes to this clipboard.
     */
    private val listenerList = HashSet<ClipboardListener>()

    /**
     * Clear the clipboard.
     */
    fun clear() {
        copiedObjects = ArrayList()
        fireClipboardChanged()
    }

    /**
     * Add objects to the clipboard.  This happens with cut and copy.
     *
     * @param objects objects to add
     */
    fun add(objects: List<NetworkModel>) {
        copiedObjects = objects
        // System.out.println("add-->"+ Arrays.asList(objects));
        fireClipboardChanged()
    }

    /**
     * Paste objects into the netPanel.
     *
     * @param net the network to paste into
     */
    fun paste(net: NetworkPanel) {
        if (isEmpty) {
            return
        }

        // Create a copy of the clipboard objects.
        val copy = SimnetUtils.getCopy(net.network, copiedObjects)
        copy.filterIsInstance<LocatableModel>()
            .forEach { it.shouldBePlaced = false }

        net.launch {
            // Add the copied object
            net.network.addNetworkModels(copy).join()

            // Unselect "old" copied objects
            net.selectionManager.clear()

            // Paste objects intelligently using placement manager
            net.network.placementManager.placeObjects(
                copy.filterIsInstance<LocatableModel>()
                    .onEach { it.shouldBePlaced = true }
            )

            // Select copied objects after pasting them
            copy.forEach { it.select() }
        }
    }

    @JvmStatic
    val isEmpty: Boolean
        /**
         * @return true if there's nothing in the clipboard, false otherwise
         */
        get() = copiedObjects.isEmpty()

    /**
     * Add the specified clipboard listener.
     *
     * @param l listener to add
     */
    @JvmStatic
    fun addClipboardListener(l: ClipboardListener) {
        listenerList.add(l)
    }

    /**
     * Fire a clipboard changed event to all registered model listeners.
     */
    fun fireClipboardChanged() {
        for (listener in listenerList) {
            listener.clipboardChanged()
        }
    }
}