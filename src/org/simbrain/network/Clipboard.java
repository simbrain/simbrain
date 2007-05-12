/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.network;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.simnet.interfaces.Network;
import org.simnet.interfaces.Neuron;
import org.simnet.util.CopyFactory;
import org.simnet.util.SimnetUtils;


/**
 * Buffer which holds network objects for cutting and pasting.
 */
public class Clipboard {

    /** Static list of cut or copied objects. */
    private static ArrayList clipboard = new ArrayList();

    /** List of components which listen for changes to this clipboard. */
    private static HashSet listenerList = new HashSet();

    /** Distance between pasted elemeents. */
    private static final double PASTE_INCREMENT = 15;

    /**
     * Clear the clipboard.
     */
    public static void clear() {
        clipboard = new ArrayList();
        fireClipboardChanged();
    }

    /**
     * Add objects to the clipboard.
     *
     * @param objects objects to add
     */
    public static void add(final ArrayList objects) {
        clipboard = objects;
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
        ArrayList copy = CopyFactory.getCopy(clipboard);

        // Gather data for translating the object then add the objects to the network.
        Point2D upperLeft = SimnetUtils.getUpperLeft(clipboard);
        SimnetUtils.translate(copy, getPasteOffset(net, upperLeft,  "X"), getPasteOffset(net, upperLeft, "Y"));
        net.getRootNetwork().addObjects(copy);

        // Select pasted items
        net.setSelection(getPostPasteSelectionObjects(net, copy));
        net.repaint();
    }

    /**
     * Returns those objects that should be selected after a paste.
     *
     * @param net reference to network panel.
     * @param list list of objects.
     * @return list of objects to be selected after pasting.
     */
    private static ArrayList getPostPasteSelectionObjects(final NetworkPanel net, final ArrayList list) {
        ArrayList<Object> ret = new ArrayList<Object>();
        for (Object object : list) {
            if (object instanceof Neuron) {
                ret.add(net.findNeuronNode((Neuron) object));
            } else if (object instanceof Network) {
                ret.add(net.findSubnetworkNode((Network) object));
            }
        }
        return ret;
    }

    /**
     * @return true if there's nothing in the clipboard, false otherwise
     */
    public static boolean isEmpty() {
        return clipboard.isEmpty();
    }

    /**
     * Returns the paste offset.  Handles complexities of paste-trails.
     *
     * Begin where the last object was pasted (default to a standard position if none has).
     * Add the size of the object.
     * Add the number of recent clicks times the paste increment. 
     *
     * @param net reference to network panel.
     * @param upperLeft the upper left of the group of objects to be pasted
     * @param xOrY whether to return x or y offset.
     * @return the offset for the pasted items.
     */
    private static double getPasteOffset(final NetworkPanel net, final Point2D upperLeft, final String xOrY) {

        if (xOrY.equals("X")) {
            return (net.getBeginPosition().getX() - upperLeft.getX()
                    - ((net.getNumberOfPastes() + 1) * getPasteIncrement(net, "X")));
        } else {
            return (net.getBeginPosition().getY() - upperLeft.getY()
                    - ((net.getNumberOfPastes() + 1) * getPasteIncrement(net, "Y")));
        }
    }

    /**
     * Private method for handling complexities of paste-trails.
     *
     * When first pasting, paste at the default location relative to the original paste.
     * Otherwise use the paste increment computed by the network.
     *
     * @param net Reference to network
     * @param xOrY Whether to look at x or y values
     * @return the proper paste increment
     */
    private static double getPasteIncrement(final NetworkPanel net, final String xOrY) {

        if (xOrY.equals("X")) {
            if (net.getPasteX() != 0) {
                return net.getPasteX();
            } else {
                return -PASTE_INCREMENT;
            }
        }

        if (xOrY.equals("Y")) {
            if (net.getPasteY() != 0) {
                return net.getPasteY();
            } else {
                return -PASTE_INCREMENT;
            }
        }
        return 0;
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
