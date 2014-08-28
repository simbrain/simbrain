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
package org.simbrain.world.textworld;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.simbrain.world.textworld.TextWorld.TextItem;

/**
 * Display text data from another source.
 *
 * @author jyoshimi
 */
public class DisplayPanel extends JPanel {

    /** Underlying model text world. */
    private final DisplayWorld world;

    /** Text area for inputting text into networks. */
    private JTextArea textArea = new JTextArea();

    /**
     * Toolbar for opening and closing the world. Must be defined at component
     * level.
     */
    private JToolBar openCloseToolBar = null;

    /**
     * Construct a reader panel to represent data in a text world.
     *
     * @param theWorld the world
     * @param toolbar pass in open / close toolbar
     * @return the constructed panel
     */
    public DisplayPanel(DisplayWorld theWorld, JToolBar toolbar) {
        this.world = theWorld;
        this.openCloseToolBar = toolbar;

        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        
        // Set up toolbar
        JPanel topToolbarPanel = new JPanel();
        topToolbarPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        if (openCloseToolBar != null) {
            topToolbarPanel.add(openCloseToolBar);
        }
        JToolBar dictionaryToolBar = new JToolBar();
        dictionaryToolBar
                .add(TextWorldActions.showDictionaryEditor(world));
        topToolbarPanel.add(dictionaryToolBar);
        add(topToolbarPanel, BorderLayout.NORTH);

        // Force a bit of room at bottom
        add(new JLabel("  "), BorderLayout.SOUTH);

        // Set up main text area
        textArea.setLineWrap(true);
        textArea.setText(world.getText());
        textArea.addCaretListener(new CaretListener() {

            public void caretUpdate(CaretEvent arg0) {
                // Tricky here. Need to set the position without firing an event
                // (and then infinite loop),
                // but also need to reset the matcher in the underlying object.
                // I wish there were a cleaner way...
                world.setPosition(textArea.getCaretPosition(), false);
            }

        });
        textArea.getDocument().addDocumentListener(new DocumentListener() {

            public void changedUpdate(DocumentEvent arg0) {
                // TODO: Check if needed in all places, migrate to separate
                // method
                // Careful of infinite loops later if this fires events in world
                // System.out.println("changedUpdate");
                world.setText(textArea.getText(), false);
            }

            public void insertUpdate(DocumentEvent arg0) {
                // System.out.println("insertUpdate");
                world.setText(textArea.getText(), false);
            }

            public void removeUpdate(DocumentEvent arg0) {
                // System.out.println("removeUpdate");
                world.setText(textArea.getText(), false);
            }

        });

        final JScrollPane inputScrollPane = new JScrollPane(textArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(inputScrollPane, BorderLayout.CENTER);

        world.addListener(new TextListener() {

            public void textChanged() {
                textArea.setText(world.getText());
                if (world.getPosition() < textArea.getDocument().getLength()) {
                    textArea.setCaretPosition(world.getPosition());
                }
            }

            public void dictionaryChanged() {
            }

            public void positionChanged() {
                textArea.setCaretPosition(world.getPosition());
            }

            public void currentItemChanged(TextItem newItem) {
            }

            public void preferencesChanged() {
            }
        });

    }

    /**
     * @return the world
     */
    public TextWorld getWorld() {
        return world;
    }

}
