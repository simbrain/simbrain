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
package org.simbrain.world.textworld.dictionary;

import org.simbrain.util.Utils;
import org.simbrain.util.projection.DataPoint;
import org.simbrain.util.projection.NTree;
import org.simbrain.util.table.SimbrainJTable;
import org.simbrain.util.table.TextTable;
import org.simbrain.util.widgets.EditablePanel;
import org.simbrain.world.textworld.DisplayWorld;
import org.simbrain.world.textworld.DisplayWorld.StringDataPoint;

import javax.swing.*;

/**
 * Dialog for editing vector to token dictionary (which associates vectors with
 * matching tokens). Used in display world.
 *
 * @author Jeff Yoshimi
 */
public class VectorToTokenPanel extends VectorDictionaryPanel implements EditablePanel {

    /**
     * The world to edit.
     */
    private final DisplayWorld world;

    /**
     * The table representing the dictionary data.
     */
    private SimbrainJTable table;

    /**
     * Construct the panel.
     *
     * @param world the world whose dictionary is being edited.
     */
    public VectorToTokenPanel(final DisplayWorld world) {
        super("<html><p>These entries consume vector inputs. " + "When a vector is received (e.g. from a neuron group) " + "the text associated with the closest vector on the left is found and displayed " + "in the display world.");
        this.world = world;
        table = SimbrainJTable.createTable(new VectorDictionaryTable(world.getVectorToTokenDict()));
        initPanel(table);
    }

    /**
     * Mutable text table customized for display of maps from double-arrays to
     * Strings.
     */
    private final class VectorDictionaryTable extends TextTable {

        /**
         * Construct table that associates tokens with vector strings.
         */
        private VectorDictionaryTable(final NTree tree) {
            super();
            init(tree.size(), 2);
            int i = 0;
            for (DataPoint point : tree.asArrayList()) {
                String token = ((StringDataPoint) point).getString();
                double[] vals = ((StringDataPoint) point).getVector();
                setLogicalValue(i, 0, token, false);
                setLogicalValue(i, 1, Utils.doubleArrayToString(vals), false);
                i++;
            }
            fireTableDataChanged();
        }
    }

    @Override
    public void fillFieldValues() {
    }

    @Override
    public boolean commitChanges() {
        world.loadVectorToTokenDict(table.getData().asStringArray());
        return true;
    }

    @Override
    public JPanel getPanel() {
        return this;
    }
}
