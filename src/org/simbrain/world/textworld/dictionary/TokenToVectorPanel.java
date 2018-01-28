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
import org.simbrain.util.table.SimbrainJTable;
import org.simbrain.util.table.TextTable;
import org.simbrain.util.widgets.EditablePanel;
import org.simbrain.world.textworld.ReaderWorld;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Dialog for editing token to vector dictionary (which associates string tokens
 * with numerical vectors.). Used in reader world.
 *
 * @author Jeff Yoshimi
 */
public class TokenToVectorPanel extends VectorDictionaryPanel implements EditablePanel {

    /**
     * The world to edit.
     */
    private final ReaderWorld world;

    /**
     * The table representing the dictionary data.
     */
    private SimbrainJTable table;

    /**
     * Construct the panel.
     *
     * @param world the world whose dictionary is being edited.
     */
    public TokenToVectorPanel(final ReaderWorld world) {
        super("<html><p>These entries produce vector outputs. " + "When they are 'activated', the " + "associated vector is sent to any coupled consumers " + "(e.g. a neuron group).");
        this.world = world;
        table = SimbrainJTable.createTable(new VectorDictionaryTable(world.getTokenToVectorDict()));
        initPanel(table);
    }

    /**
     * Mutable text table customized for display of maps from strings to
     * double-arrays.
     */
    private final class VectorDictionaryTable extends TextTable {

        /**
         * Construct table that associates tokens with vector strings.
         */
        private VectorDictionaryTable(final HashMap<String, double[]> dictionary) {
            super();
            init(dictionary.size(), 2);
            int i = 0;
            for (Map.Entry<String, double[]> entry : dictionary.entrySet()) {
                String token = entry.getKey();
                double[] vals = entry.getValue();
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
        world.loadTokenToVectorDict(table.getData().asStringArray());
        return true;
    }

    @Override
    public JPanel getPanel() {
        return this;
    }
}
