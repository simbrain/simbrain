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
package org.simbrain.workspace.gui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A list of couplings viewed by a jlist.
 * @author jyoshimi
 *
 */
public class ModifiableListModel<E> extends GenericListModel<E> {

    private static final long serialVersionUID = 1L;

    /** List of couplings. */
    private final List<E> list;

    /**
     * Default constructor.
     */
    public ModifiableListModel() {
        this(new ArrayList<E>());
    }

    /**
     * Clear list.
     */
    public void clear() {
        this.list.clear();
    }

    /**
     * Constructs a list of couplings.
     * @param couplingList list of couplings
     */
    public ModifiableListModel(final List<E> list) {
        super(list);
        this.list = list;
    }

    /**
     * Adds a coupling to the list.
     * @param element to be added
     */
    public void addElement(final E element) {
    	//TODO: Make sure this coupling doesn't exist
        list.add(element);
        this.fireContentsChanged(this, 0, getSize());
    }

    /**
     * Remove a coupling.
     *
     * @param coupling the coupling to remove.
     */
    public void remove(final E coupling) {
        list.remove(coupling);
        this.fireContentsChanged(this, 0, getSize());
    }
    /**
     * Inserts a coupling at the specified location.
     * @param coupling to be inserted
     * @param i location of insertion
     */
    public void insertElementAt(final E coupling, final int i) {
        list.add(i, coupling);
        this.fireContentsChanged(this, 0, getSize());
    }
    
    public Iterator<E> iterator() {
       return list.iterator();
    }
}
