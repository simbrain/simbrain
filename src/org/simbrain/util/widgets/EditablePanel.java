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
package org.simbrain.util.widgets;

import javax.swing.*;

/**
 * JPanels that edit the properties of an object can implement this interface to
 * achieve standard behaviors associated with editing properties. For example,
 * when committing changes made in such a panel, it useful to know whether the
 * commit succeeded (it can fail, for example if someone fills a numerical field
 * with text).
 *
 * @author ztosi
 */
public abstract class EditablePanel extends JPanel {

    /**
     * Initialize the panel with field values.
     */
    public abstract void fillFieldValues();

    /**
     * Uses the data in the fields of the panel which implements this to alter
     * the data of an appropriate model object accordingly.
     *
     * @return whether or not the commit was successful. Not always used.
     */
    public abstract boolean commitChanges();

}
