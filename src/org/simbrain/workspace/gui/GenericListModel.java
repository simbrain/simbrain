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

import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

/**
 * A list of couplings viewed by a jlist.
 */
public class GenericListModel<E> extends AbstractListModel implements ComboBoxModel, Iterable<E> {

    private static final long serialVersionUID = 1L;
    
    /** List of consumers. */
    private final List<? extends E> list;
    
    private E selected;

    /**
     * Constructs a list of consumers.
     * @param consumerList List of consumers
     */
    public GenericListModel(final List<? extends E> consumerList) {
        this.list = consumerList;
    }

    /**
     * Returns a specific consumer.
     * @param index of consumer.
     * @return consumer at specific location.
     */
    public E getElementAt(final int index) {
       return list.get(index);
    }

    /**
     * Returns the number of consumers in the list.
     * @return Number of consumers.
     */
    public int getSize() {
        return list.size();
    }

    /**
     * Returns the selected item.
     * @return selected item.
     */
    public E getSelectedItem() {
        return selected;
    }

    /**
     * Sets the selected item(s).
     * @param arg0 items to be set as selected.
     * //TODO: Check this stuff...
     */
    public void setSelectedItem(final Object arg0) {
        for (E component : list) {
            if (component == arg0) {
                selected = component;
            }
        }
    }

    public Iterator<E> iterator() {
        return new Iterator<E>() {
            Iterator<? extends E> internal = list.iterator();
            
            public boolean hasNext() {
                return internal.hasNext();
            }

            public E next() {
                return internal.next();
            }

            public void remove() {
                throw new UnsupportedOperationException("cannot remove elements with this iterator");
            }
        };
    }
}
