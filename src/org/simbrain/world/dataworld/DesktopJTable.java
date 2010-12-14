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
package org.simbrain.world.dataworld;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import org.simbrain.util.table.SimbrainDataTable;
import org.simbrain.util.table.SimbrainJTable;
import org.simbrain.workspace.PotentialConsumer;
import org.simbrain.workspace.PotentialProducer;
import org.simbrain.workspace.gui.CouplingMenuConsumer;
import org.simbrain.workspace.gui.CouplingMenuProducer;

/**
 * Extends SimbrainJTable context menu with attribute menus.
 *
 * @author jyoshimi
 */
public class DesktopJTable extends SimbrainJTable {

    /** Parent component. */
    DataWorldComponent component;

    /**
     * Construct the table.
     *
     * @param dataModel base table
     * @param component parent component
     */
    public DesktopJTable(SimbrainDataTable dataModel, DataWorldComponent component) {
        super(dataModel);
        this.component = component;
    }


    /**
     * Build the context menu for the table.
     *
     * @return The context menu.
     */
    protected JPopupMenu buildPopupMenu() {

        JPopupMenu ret = super.buildPopupMenu();
        ret.addSeparator();
        String producerDescription = component.getProducingColumnType().getDescription("Column " +  getSelectedColumn());
        PotentialProducer producer = component.getAttributeManager()
                .createPotentialProducer(
                        component.getObjectFromKey("" + getSelectedColumn()),
                        component.getProducingColumnType(),
                        producerDescription);
        JMenu producerMenu = new CouplingMenuProducer("Send coupling to", component.getWorkspace(), producer); 
        ret.add(producerMenu);
        String consumerDescription = component.getConsumingColumnType().getDescription("Column " +  getSelectedColumn());
        PotentialConsumer consumer= component.getAttributeManager()
        .createPotentialConsumer(
                component.getObjectFromKey("" + getSelectedColumn()),
                component.getConsumingColumnType(),
                consumerDescription);
        JMenu consumerMenu = new CouplingMenuConsumer("Receive coupling from", component.getWorkspace(), consumer);
        ret.add(consumerMenu);
        return ret;
    }
}
