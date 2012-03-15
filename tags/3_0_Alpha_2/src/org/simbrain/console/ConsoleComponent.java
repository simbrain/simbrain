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
package org.simbrain.console;

import java.io.InputStream;
import java.io.OutputStream;

import org.simbrain.workspace.WorkspaceComponent;

/**
 * Component corresponding to a beanshell window.
 */
public class ConsoleComponent extends WorkspaceComponent {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     */
    public ConsoleComponent(String name) {
        super(name);
    }

    @Override
    public void closing() {
        // TODO Auto-generated method stub

    }

    /**
     * Opens a saved component. There isn't much to do here since currently
     * there is nothing to persist with a console. This just ensures that a
     * component is created and (in the gui) presented.
     *
     * @param input stream
     * @param name name of file
     * @param format format
     * @return component to be opened
     */
    public static ConsoleComponent open(InputStream input, final String name,
            final String format) {
        return new ConsoleComponent(name);
    }

    @Override
    public void save(OutputStream output, final String format) {
        // TODO implement
    }

    @Override
    public void update() {
        // TODO Auto-generated method stub

    }
}
