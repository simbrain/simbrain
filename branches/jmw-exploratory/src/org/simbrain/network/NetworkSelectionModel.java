/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005-2006 Jeff Yoshimi <www.jeffyoshimi.net>
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.event.EventListenerList;

/**
 * Network selection model.
 */
final class NetworkSelectionModel {

    /** Listener list. */
    private final EventListenerList listenerList;

    /** Source of selection events. */
    private final NetworkPanel networkPanel;

    /** Set of selected elements. */
    private final Set selection;

    /** Adjusting. */
    private boolean adjusting;


    /**
     * Create a new network selection model for the specified
     * source of selection events.
     *
     * @param networkPanel source of selection events
     */
    public NetworkSelectionModel(final NetworkPanel networkPanel) {

        adjusting = false;
        selection = new HashSet();
        this.networkPanel = networkPanel;
        listenerList = new EventListenerList();
    }


    /**
     * Return the size of the selection.
     *
     * @return the size of the selection
     */
    public int size() {
        return selection.size();
    }

    /**
     * Clear the selection.
     */
    public void clear() {

        if (!isEmpty()) {
            Set oldSelection = new HashSet(selection);
            selection.clear();
            fireSelectionChanged(oldSelection, selection);
            oldSelection = null;
        }
    }

    /**
     * Return true if the selection is empty.
     *
     * @return true if the selection is empty
     */
    public boolean isEmpty() {
        return selection.isEmpty();
    }

    /**
     * Add the specified element to the selection.
     *
     * @param element element to add
     */
    public void add(final Object element) {

        Set oldSelection = new HashSet(selection);
        boolean rv = selection.add(element);
        if (rv) {
            fireSelectionChanged(oldSelection, selection);
        }
        oldSelection = null;
    }

    /**
     * Add all of the specified elements to the selection.
     *
     * @param elements elements to add
     */
    public void addAll(final Collection elements) {

        adjusting = true;
        Set oldSelection = new HashSet(selection);
        boolean rv = selection.addAll(elements);
        adjusting = false;

        if (rv) {
            fireSelectionChanged(oldSelection, selection);
        }
        oldSelection = null;
    }

    /**
     * Remove the specified element from the selection.
     *
     * @param element element to remove
     */
    public void remove(final Object element) {

        Set oldSelection = new HashSet(selection);
        boolean rv = selection.remove(element);
        if (rv) {
            fireSelectionChanged(oldSelection, selection);
        }
        oldSelection = null;
    }

    /**
     * Remove all of the specified elements from the selection.
     *
     * @param elements elements to remove
     */
    public void removeAll(final Collection elements) {

        adjusting = true;
        Set oldSelection = new HashSet(selection);
        boolean rv = selection.removeAll(elements);
        adjusting = false;

        if (rv) {
            fireSelectionChanged(oldSelection, selection);
        }
    }

    /**
     * Return true if the specified element is selected.
     *
     * @param element element
     * @return true if the specified element is selected
     */
    public boolean isSelected(final Object element) {
        return selection.contains(element);
    }

    /**
     * Return the selection as an unmodifiable collection of selected elements.
     *
     * @return the selection as an unmodifiable collection of selected elements
     */
    public Collection getSelection() {
        return Collections.unmodifiableSet(selection);
    }

    /**
     * Set the selection to the specified collection of elements.
     *
     * @param elements elements
     */
    public void setSelection(final Collection elements) {

        if (selection.isEmpty() && elements.isEmpty()) {
            return;
        }

        adjusting = true;
        Set oldSelection = new HashSet(selection);
        selection.clear();
        boolean rv = selection.addAll(elements);
        adjusting = false;

        if (rv || elements.isEmpty()) {
            fireSelectionChanged(oldSelection, selection);
        }

    }

    /**
     * Add the specified network selection listener.
     *
     * @param l network selection listener to add
     */
    public void addSelectionListener(final NetworkSelectionListener l) {
        listenerList.add(NetworkSelectionListener.class, l);
    }

    /**
     * Remove the specified network selection listener.
     *
     * @param l network selection listener to remove
     */
    public void removeSelectionListener(final NetworkSelectionListener l) {
        listenerList.remove(NetworkSelectionListener.class, l);
    }

    /**
     * Return true if this model will be adjusting over a series
     * of rapid changes.
     *
     * @return true if ths model will be adjusting over a series
     *    of rapid changes
     */
    public boolean isAdjusting() {
        return adjusting;
    }

    /**
     * Set to true if this model will be adjusting over a series
     * of rapid changes.
     *
     * @param adjusting true if this model will be adjusting over
     *    a series of rapid changes
     */
    public void setAdjusting(final boolean adjusting) {
        this.adjusting = adjusting;
    }

    /**
     * Fire a wholesale selection model changed event to all registered
     * selection listeners.
     *
     * @param oldSelection old selection
     * @param selection selection
     */
    public void fireSelectionChanged(final Set oldSelection, final Set selection) {
        if (isAdjusting()) {
            return;
        }

        Object[] listeners = listenerList.getListenerList();
        NetworkSelectionEvent e = null;

        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == NetworkSelectionListener.class) {
                if (e == null) {
                    e = new NetworkSelectionEvent(networkPanel, oldSelection, selection);
                }
                ((NetworkSelectionListener) listeners[i + 1]).selectionChanged(e);
            }
        }
    }
}