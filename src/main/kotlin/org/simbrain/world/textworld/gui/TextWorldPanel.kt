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

import org.simbrain.world.textworld.ReaderWorld.ParseStyle;
import org.simbrain.world.textworld.TextWorld.TextItem;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;

/**
 * Display panel for reading data from user and showing text world's state.
 *
 * @author jyoshimi
 */
public class ReaderPanel extends JPanel {

    /**
     * Underlying model text world.
     */
    private final ReaderWorld world;

    /**
     * Text area for inputting text into networks.
     */
    private JTextArea textArea = new JTextArea();

    /**
     * The main scroll panel.
     */
    final JScrollPane inputScrollPane;

    /**
     * Displays the current parse style and allows it to be set.
     */
    private final ButtonGroup parseStyle = new ButtonGroup();

    /**
     * Parse style is word-based.
     */
    private final JRadioButton wordButton = new JRadioButton("Word");

    /**
     * Parse style is character based.
     */
    private final JRadioButton charButton = new JRadioButton("Character");

    /**
     * Toolbar for opening and closing the world. Must be defined at component
     * level.
     */
    private JToolBar openCloseToolBar = null;

    /**
     * Factory method for panel (so that listeners are not created in
     * constructor).
     *
     * @param theWorld the world
     * @param toolbar  pass in open / close toolbar
     * @return the constructed panel
     */
    public static ReaderPanel createReaderPanel(ReaderWorld theWorld, JToolBar toolbar) {
        ReaderPanel panel = new ReaderPanel(theWorld, toolbar);
        panel.initListeners();
        return panel;
    }

    /**
     * Factory method for panel (so that listeners are not created in
     * constructor).
     *
     * @param theWorld the world
     * @return the constructed panel
     */
    public static ReaderPanel createReaderPanel(ReaderWorld theWorld) {
        ReaderPanel panel = new ReaderPanel(theWorld, null);
        panel.initListeners();
        return panel;
    }

    /**
     * Initialize the panel with an open / close toolbar.
     *
     * @param theWorld the reader world to display
     * @param toolbar  the openClose toolbar.
     */
    private ReaderPanel(ReaderWorld theWorld, JToolBar toolbar) {
        this.world = theWorld;
        openCloseToolBar = toolbar;
        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        textArea.setLineWrap(true);
        textArea.setText(world.getText());
        inputScrollPane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(inputScrollPane);

        // Add toolbars
        JPanel topToolbarPanel = new JPanel();
        topToolbarPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        // if (openCloseToolBar != null) {
        //     topToolbarPanel.add(openCloseToolBar);
        // }
        JToolBar dictionaryToolBar = new JToolBar();
        dictionaryToolBar.add(TextWorldActions.showDictionaryEditor(world));
        dictionaryToolBar.add(TextWorldActions.getExtractDictionaryAction(world));
        topToolbarPanel.add(dictionaryToolBar);

        add(topToolbarPanel, BorderLayout.NORTH);
        JPanel bottomToolbarPanel = new JPanel();
        bottomToolbarPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        bottomToolbarPanel.add(getToolbarModeSelect());
        add(bottomToolbarPanel, BorderLayout.SOUTH);

        syncParseStyleButtons();
    }

    /**
     * Init the listeners, called by factor method, outside the constructor.
     */
    private void initListeners() {

        // Reset text position when user clicks in text area
        textArea.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                world.setPosition(textArea.getCaretPosition(), false);
                world.updateMatcher();
            }

        });

        // Listener for changes in the textarea (i.e. adding or removing text
        // directly in the area).
        textArea.getDocument().addDocumentListener(new DocumentListener() {

            public void changedUpdate(DocumentEvent arg0) {
                //System.out.println("readerworld: changedUpdate");
                world.setText(textArea.getText(), false);
            }

            public void insertUpdate(DocumentEvent arg0) {
                //System.out.println("readerworld: insertUpdate");
                world.setText(textArea.getText(), false);
            }

            public void removeUpdate(DocumentEvent arg0) {
                //System.out.println("readerworld: removeUpdate");
                world.setText(textArea.getText(), false);
            }

        });

        // Force component to fill up parent panel
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // textArea.setPreferredSize(ReaderPanel.this.getPreferredSize());
                inputScrollPane.setPreferredSize(new Dimension(ReaderPanel.this.getPreferredSize().width - 25, ReaderPanel.this.getPreferredSize().height - 25));
                // inputScrollPane.revalidate();
            }
        });

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
                if (world.getCurrentItem().getText().equalsIgnoreCase("")) {
                    removeHighlights(textArea);
                } else {
                    highlight(world.getCurrentItem().getBeginPosition(), world.getCurrentItem().getEndPosition());
                }
            }

            public void preferencesChanged() {
                if (world.getTheParseStyle() == ParseStyle.CHARACTER) {
                    charButton.setSelected(true);
                } else if (world.getTheParseStyle() == ParseStyle.WORD) {
                    wordButton.setSelected(true);
                }
            }

        });

    }

    /**
     * Syncs the parse style buttons to the underlying model state.
     */
    public void syncParseStyleButtons() {
        if (world.getTheParseStyle() == ParseStyle.CHARACTER) {
            charButton.setSelected(true);
        } else if (world.getTheParseStyle() == ParseStyle.WORD) {
            wordButton.setSelected(true);
        }
    }

    /**
     * Highlight word beginning at <code>begin</code> nd ending at
     * <code>end</code>.
     *
     * @param begin offset of beginning of highlight
     * @param end   offset of end of highlight
     */
    public void highlight(final int begin, final int end) {
        // An instance of the private subclass of the default highlight painter
        Highlighter.HighlightPainter myHighlightPainter = new MyHighlightPainter(world.getHighlightColor());
        removeHighlights(textArea);
        try {
            Highlighter hilite = textArea.getHighlighter();
            hilite.addHighlight(begin, end, myHighlightPainter);
        } catch (BadLocationException e) {
            System.err.checkError();
        }
    }

    /**
     * Removes highlights from specified component.
     *
     * @param textComp text component to remove highlights from.
     */
    public void removeHighlights(final JTextComponent textComp) {
        Highlighter hilite = textComp.getHighlighter();
        Highlighter.Highlight[] hilites = hilite.getHighlights();
        for (int i = 0; i < hilites.length; i++) {
            if (hilites[i].getPainter() instanceof MyHighlightPainter) {
                hilite.removeHighlight(hilites[i]);
            }
        }
    }

    /**
     * A private subclass of the default highlight painter.
     */
    class MyHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {

        /**
         * Sets the color of highlighter.
         *
         * @param color Color of highlight
         */
        public MyHighlightPainter(final Color color) {
            super(color);
        }
    }

    /**
     * @return the world
     */
    public ReaderWorld getWorld() {
        return world;
    }

    /**
     * Return a toolbar with buttons for switching between word and character
     * mode.
     *
     * @return the mode selection toolbar
     */
    public JToolBar getToolbarModeSelect() {
        JToolBar toolbar = new JToolBar();
        parseStyle.add(wordButton);
        parseStyle.add(charButton);
        wordButton.setSelected(true);
        toolbar.add(wordButton);
        toolbar.add(charButton);

        wordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                world.setParseStyle(ParseStyle.WORD);
            }

        });
        charButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                world.setParseStyle(ParseStyle.CHARACTER);
            }
        });

        // add action listener for switching between char and word buttons:
        // wordButton.addActionListener(a);
        return toolbar;
    }

}
