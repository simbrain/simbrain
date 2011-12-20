package org.simbrain.util;

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

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * A simple text editor component. Just a stub for now. Plan to incorporate
 * syntax highlighting, etc. later.
 */
public class EditorPanel extends JPanel {

    /** Default height. */
    private static final int DEFAULT_ROWS = 20;

    /** Default width. */
    private static final int DEFAULT_COLS = 40;

    /** Scroll pane. */
    private JScrollPane scrollPane;

    /** The text area. */
    private JTextArea textArea;

    /**
     * A panel for editing scripts.
     */
    public EditorPanel() {
        super();
        textArea = new JTextArea(DEFAULT_ROWS, DEFAULT_COLS);
        scrollPane = new JScrollPane(textArea);
        add(scrollPane);
    }

}
