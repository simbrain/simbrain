/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.world.odorworld;

import javax.swing.JTextField;

import org.simbrain.world.odorworld.entities.OdorWorldEntity;

/**
 * Panel to add a set of tile sensors to an entity.
 *
 * @author Lam Nguyen
 *
 */
public class TileSetPanel extends AbstractSensorPanel {

    /** Text field to edit x. */
    private JTextField x = new JTextField("" + 3);

    /** Text field to edit y. */
    private JTextField y = new JTextField("" + 3);

    /** Text field to edit offset. */
    private JTextField offset = new JTextField("" + 0);

    /** Entity to which a set of tile sensors is being added. */
    private OdorWorldEntity entity;

    /**
     * Default constructor.
     *
     * @param entity the entity to which a set of tile sensors is added.
     */
    public TileSetPanel(final OdorWorldEntity entity) {
        this.entity = entity;
        this.setName("Create grid of tile sensors");
        addItem("Rows", x);
        addItem("Columns", y);
        addItem("Offset", offset);
    }

    @Override
    public void commitChanges() {
        entity.addTileSensors(Integer.parseInt(x.getText()),
                Integer.parseInt(y.getText()),
                Integer.parseInt(offset.getText()));
    }
}