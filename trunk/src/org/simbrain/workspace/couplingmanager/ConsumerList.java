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

import org.simbrain.workspace.Consumer;

/**
 * A list of couplings viewed by a jlist.
 */
public class ConsumerList extends AbstractListModel implements ListModel {

    /** List of consumers. */
    private ArrayList<Consumer> consumerList = new ArrayList<Consumer>();

    /**
     * Constructs a list of consumers.
     * @param consumerList List of consumers
     */
    public ConsumerList(final ArrayList<Consumer>  consumerList) {
        this.consumerList = consumerList;
    }

    /**
     * Returns a specific consumer.
     * @param index of consumer.
     * @return consumer at specific location.
     */
    public Object getElementAt(final int index) {
       return consumerList.get(index);
    }

    /**
     * Returns the number of consumers in the list.
     * @return Number of consumers.
     */
    public int getSize() {
        return consumerList.size();
    }

    /**
     * Adds a new element to the consumer list.
     * @param element Consumer to be added
     */
    public void addElement(final Consumer element) {
        consumerList.add(element);
    }

    /**
     * @return the consumerList
     */
    public ArrayList<Consumer> asArrayList() {
        return consumerList;
    }
}
