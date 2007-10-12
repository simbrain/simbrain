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
package org.simbrain.workspace.couplingmanager;

import java.util.ArrayList;

import javax.swing.AbstractListModel;
import javax.swing.ListModel;

import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.ProducingAttribute;

/**
 * A list of couplings viewed by a jlist.
 * @author jyoshimi
 *
 */
public class CouplingList extends AbstractListModel implements ListModel {

    private static final long serialVersionUID = 1L;
    
    /** List of couplings. */
    private ArrayList<Coupling> couplingList = new ArrayList<Coupling>();

    /**
     * Default constructor.
     */
    public CouplingList() {
        super();
    }

    /**
     * Constructs a list of couplings.
     * @param couplingList list of couplings
     */
    public CouplingList(final ArrayList<Coupling> couplingList) {
        super();
        this.couplingList = couplingList;
    }

    /**
     * Returns the object at the specified location.
     * @param index of item
     * @return object at given index
     */
    public Object getElementAt(final int index) {
       return couplingList.get(index);
    }

    /**
     * Returns the size of the list.
     * @return size of list
     */
    public int getSize() {
        return couplingList.size();
    }

    /**
     * Adds a coupling to the list.
     * @param element to be added
     */
    public void addElement(final Coupling element) {
    	//TODO: Make sure this coupling doesn't exist
        couplingList.add(element);
        this.fireContentsChanged(this, 0, getSize());
    }

    /**
     * Position to bind a producer.
     * @param producer to be bound
     * @param index of location to bind
     */
    public void bindElementAt(final ProducingAttribute producer, final int index) {
        couplingList.get(index).setProducingAttribute(producer);
        this.fireContentsChanged(this, 0, getSize());
    }

    /**
     * Remove a coupling.
     *
     * @param coupling the coupling to remove.
     */
    public void removeCoupling(final Coupling coupling) {
        couplingList.remove(coupling);
        this.fireContentsChanged(this, 0, getSize());
    }
    /**
     * Inserts a coupling at the specified location.
     * @param coupling to be inserted
     * @param i location of insertion
     */
    public void insertElementAt(final Coupling coupling, final int i) {
        couplingList.add(i, coupling);
        this.fireContentsChanged(this, 0, getSize());
    }

    /**
     * Returns the coupling list.
     * @return the couplingList
     */
    public ArrayList<Coupling> asArrayList() {
        return couplingList;
    }
}
