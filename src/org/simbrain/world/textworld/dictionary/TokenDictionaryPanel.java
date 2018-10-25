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

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.projection.DataPoint;
import org.simbrain.util.table.SimbrainJTable;
import org.simbrain.util.table.SimbrainJTableScrollPanel;
import org.simbrain.util.table.TextTable;
import org.simbrain.util.widgets.EditablePanel;
import org.simbrain.world.textworld.DisplayWorld;
import org.simbrain.world.textworld.DisplayWorld.StringDataPoint;
import org.simbrain.world.textworld.ReaderWorld;
import org.simbrain.world.textworld.TextListener.TextAdapter;
import org.simbrain.world.textworld.TextWorld;
import org.simbrain.world.textworld.TextWorldActions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

/**
 * Panel for editing dictionaries which only consist of a list of string tokens,
 * used in scalar couplings.
 *
 * @author Jeff Yoshimi
 */
public class TokenDictionaryPanel extends EditablePanel {

    /**
     * The display or reader world.
     */
    private final TextWorld world;

    /**
     * The jtable.
     */
    private SimbrainJTable table;

    /**
     * The scroll panel.
     */
    private final SimbrainJTableScrollPanel scroller;

    /**
     * The text field showing the display threshold.
     */
    private final JTextField thresholdField;

    // todo; factory method since it has listeners?

    /**
     * Construct the editor.
     *
     * @param world the world whose dictionary is being edited.
     */
    public TokenDictionaryPanel(final TextWorld world) {
        super();
        this.world = world;

        table = SimbrainJTable.createTable(new DictionaryTable(((TextWorld) world).getTokenDictionary()));
        scroller = new SimbrainJTableScrollPanel(table);
        initTable();

        JPanel toolbar = new JPanel();

        toolbar.setLayout(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(table.getToolbarCSV(true, false));
        toolbar.add(table.getToolbarEditRows());
        JToolBar importToolBar = new JToolBar();
        importToolBar.add(TextWorldActions.getExtractDictionaryAction(world));
        importToolBar.add(getExtractTokensFromVectorDict(world));
        toolbar.add(importToolBar);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1.0; // Give scroller more horizontal weight
        gbc.weighty = 1.0; // Make sure all vertical space is filled
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(scroller, gbc);
        JLabel textLabel = new JLabel();
        String text;
        thresholdField = new JTextField();
        if (world instanceof DisplayWorld) {
            JToolBar thresholdToolbar = new JToolBar();
            thresholdToolbar.add(new JLabel("Threshold:"));
            thresholdToolbar.add(thresholdField);
            toolbar.add(thresholdToolbar);
            text = "<html><p>" + "These entries consume scalar inputs.  When a coupling receives an input (e.g. from a neuron) " + "above the threshold value, the corresponding token " + "is produced in the display world.</p>";
        } else {
            text = "<html><p>" + "These entries produce scalar outputs. " + "When a dictionary entry is 'activated', a value of 1 " + "is sent to the coupling's consumer (e.g. a neuron).</p>";
        }
        textLabel.setText(text);

        textLabel.setPreferredSize(new Dimension(150, 200));
        textLabel.setMinimumSize(new Dimension(150, 200));
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0;
        gbc.insets = new Insets(5, 10, 5, 5);
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(textLabel, gbc);

        this.setLayout(new BorderLayout());
        add(toolbar, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);

        fillFieldValues();

        world.addListener(new TextAdapter() {

            @Override
            public void dictionaryChanged() {
                table = SimbrainJTable.createTable(new DictionaryTable(((TextWorld) world).getTokenDictionary()));
                initTable();
            }
        });

    }

    /**
     * Initialize the table.
     */
    private void initTable() {
        table.setShowCSVInPopupMenu(true);
        table.setShowDeleteColumnPopupMenu(false);
        table.setShowInsertColumnPopupMenu(false);
        table.setShowEditInPopupMenu(false);
        table.setDisplayColumnHeadings(true);
        table.setColumnHeadings(Arrays.asList("Entry"));
        scroller.setTable(table);
        scroller.revalidate();

    }

    /**
     * Mutable text table customized for display of lists of strings.
     */
    private class DictionaryTable extends TextTable {

        /**
         * Construct the table.
         */
        private DictionaryTable(final Set<String> set) {
            super();
            init(set.size(), 1);
            int i = 0;
            for (Iterator<String> iterator = set.iterator(); iterator.hasNext(); ) {
                String token = (String) iterator.next();
                setLogicalValue(i, 0, token, false);
                i++;
            }
            fireTableDataChanged();
        }
    }

    public void fillFieldValues() {
        if (world instanceof DisplayWorld) {
            thresholdField.setText("" + ((DisplayWorld) world).getDisplayThreshold());
        }
    }

    @Override
    public boolean commitChanges() {
        if (world instanceof DisplayWorld) {
            ((DisplayWorld) world).setDisplayThreshold(Double.parseDouble(thresholdField.getText()));
        }
        world.loadTokenDictionary(table.getData().asStringArray());
        return true;
    }

    /**
     * An action for extracting tokens from the vector dictionary associated
     * with the world object.
     *
     * @param theWorld the text world or display world to extract items from
     * @return the action
     */
    public static Action getExtractTokensFromVectorDict(final TextWorld theWorld) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Import.png"));
                putValue(NAME, "Extract vector tokens");
                putValue(SHORT_DESCRIPTION, "Extract tokens from vector dictionary.");
            }

            public void actionPerformed(ActionEvent arg0) {
                if (theWorld instanceof DisplayWorld) {
                    for (DataPoint point : ((DisplayWorld) theWorld).getVectorToTokenDict().asArrayList()) {
                        theWorld.addWordToTokenDictionary(((StringDataPoint) point).getString());
                    }
                    theWorld.fireDictionaryChangedEvent();
                } else {
                    for (String word : ((ReaderWorld) theWorld).getTokenToVectorDict().keySet()) {
                        theWorld.addWordToTokenDictionary(word);
                    }
                    theWorld.fireDictionaryChangedEvent();
                }

            }
        };
    }

}
