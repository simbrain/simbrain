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
package org.simbrain.network.gui;

import org.piccolo2d.PNode;
import org.simbrain.network.NetworkModel;
import org.simbrain.network.util.SimnetUtils;
import org.simbrain.util.math.SimbrainMath;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Buffer which holds network objects for cutting and pasting.
 */
public class Clipboard {

    // To add new copy-pastable items, must update:
    // 1) SimnetUtils.getCopy()
    // 2) Network.addObjects
    // 3) NetworkPanel.getSelectedModels()

    /**
     * Static list of cut or copied objects.
     */
    private static List<NetworkModel> copiedObjects = new ArrayList<>();

    /**
     * List of components which listen for changes to this clipboard.
     */
    private static HashSet listenerList = new HashSet();

    /**
     * Clear the clipboard.
     */
    public static void clear() {
        copiedObjects = new ArrayList<>();
        fireClipboardChanged();
    }

    /**
     * Add objects to the clipboard.  This happens with cut and copy.
     *
     * @param objects objects to add
     */
    public static void add(final List<NetworkModel> objects) {
        copiedObjects = objects;
        //System.out.println("add-->"+ Arrays.asList(objects));
        fireClipboardChanged();
    }

    /**
     * Paste objects into the netPanel.
     *
     * @param net the network to paste into
     */
    public static void paste(final NetworkPanel net) {
        if (isEmpty()) {
            return;
        }

        // Create a copy of the clipboard objects.
        List<NetworkModel> copy = SimnetUtils.getCopy(net.getNetwork(), copiedObjects);
        Point2D currentPosition = SimnetUtils.getUpperLeft(copy);
        Point2D targetLocation = net.getPlacementManager().getLocation();
        SimnetUtils.translate(copy, SimbrainMath.subtract(targetLocation, currentPosition));

        // Add the copied object
        net.getNetwork().addObjects(copy);

        // Select copied objects after pasting them
        List<PNode> toSelect = copy.stream()
                .map(net.getObjectNodeMap()::get)
                .collect(Collectors.toList());
        net.setSelection(toSelect);
        net.repaint();
    }

    /**
     * @return true if there's nothing in the clipboard, false otherwise
     */
    public static boolean isEmpty() {
        return copiedObjects.isEmpty();
    }

    /**
     * Add the specified clipboard listener.
     *
     * @param l listener to add
     */
    public static void addClipboardListener(final ClipboardListener l) {
        listenerList.add(l);
    }

    /**
     * Fire a clipboard changed event to all registered model listeners.
     */
    public static void fireClipboardChanged() {
        for (Iterator i = listenerList.iterator(); i.hasNext(); ) {
            ClipboardListener listener = (ClipboardListener) i.next();
            listener.clipboardChanged();
        }
    }

}
