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

import org.simbrain.util.StandardDialog;
import org.simbrain.util.widgets.EditablePanel;
import org.simbrain.util.widgets.ShowHelpAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Top level dialog for displaying dictionary editors.
 *
 * @author Jeff Yoshimi
 */
public class DictionarySelector extends StandardDialog {

    /**
     * The main card panel that changes depending on the combo box
     */
    private final JPanel cardPanel = new JPanel(new CardLayout());

    /**
     * Combo box for selecting ditionary type.
     */
    private final JComboBox dictionaryType = new JComboBox(new String[]{"Scalar", "Vector"});

    /**
     * The panel for the scalar dictionary editor.
     */
    private final JPanel scalarPanel;

    /**
     * The panel for the vector dictionary editor.
     */
    private final JPanel vectorPanel;

    /**
     * Factory method to create the editor.
     *
     * @param scalarPanel the panel for editing the scalar dict.
     * @param vectorPanel the panel for editing the vector dict.
     * @return the constructed editor
     */
    public static DictionarySelector createVectorDictionaryEditor(JPanel scalarPanel, JPanel vectorPanel) {
        DictionarySelector editor = new DictionarySelector(scalarPanel, vectorPanel);
        editor.addButton(new JButton(new ShowHelpAction("Pages/Worlds/TextWorld/TextWorld.html")));
        editor.setMinimumSize(new Dimension(300, 400));
        return editor;
    }

    /**
     * Construct the editor.
     *
     * @param scalarPanel the panel for editing the scalar dict.
     * @param vectorPanel the panel for editing the vector dict.
     */
    private DictionarySelector(final JPanel scalarPanel, final JPanel vectorPanel) {
        this.scalarPanel = scalarPanel;
        this.vectorPanel = vectorPanel;
        setTitle("Dictionary Editor");

        // Main panel
        dictionaryType.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent arg0) {
                initCardPanel();
            }
        });
        dictionaryType.setSelectedIndex(1);
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Select Dictionary Type:"));
        topPanel.add(dictionaryType);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(BorderLayout.NORTH, topPanel);
        mainPanel.add(BorderLayout.CENTER, cardPanel);
        setContentPane(mainPanel);

        // Init dialog
        initCardPanel();
    }

    /**
     * Set the current "card".
     */
    private void initCardPanel() {
        if (dictionaryType.getSelectedItem().equals("Vector")) {
            cardPanel.removeAll();
            cardPanel.add(vectorPanel);
        } else {
            cardPanel.removeAll();
            cardPanel.add(scalarPanel);
        }
        cardPanel.revalidate();
        cardPanel.repaint();
        pack();
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        if (dictionaryType.getSelectedItem().equals("Vector")) {
            ((EditablePanel) vectorPanel).commitChanges();
        } else {
            ((EditablePanel) scalarPanel).commitChanges();
        }
    }

}
